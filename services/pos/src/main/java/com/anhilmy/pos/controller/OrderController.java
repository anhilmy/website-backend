package com.anhilmy.pos.controller;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.anhilmy.pos.dto.CompleteOrderRequest;
import com.anhilmy.pos.dto.OrderItemRequest;
import com.anhilmy.pos.dto.OrderRequest;
import com.anhilmy.pos.dto.PageResponse;
import com.anhilmy.pos.dto.RefundRequest;
import com.anhilmy.pos.dto.VoidItemRequest;
import com.anhilmy.pos.exception.AuthorizationException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.logging.LoggingManager;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.service.TransactionService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import spark.Request;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

/**
 * Order lifecycle endpoints: create, modify, complete, cancel and list.
 *
 * <p>Cashiers may only touch their own orders; managers may act on any order.
 */
public class OrderController {

    private static final TransactionService service = new TransactionService();
    private static final com.anhilmy.pos.service.DiningTableService tableService =
            new com.anhilmy.pos.service.DiningTableService();
    private static final com.anhilmy.pos.repository.DiningTableRepository tableRepository =
            new com.anhilmy.pos.repository.DiningTableRepository();

    public static void registerRoutes() {
        // Create a draft order for the authenticated cashier.
        post("/orders", Routes.wrap((req, res) -> {
            OrderRequest body = parseOptional(req, OrderRequest.class);
            Long cashierId = AuthenticationFilter.userId(req);
            return Responder.created(res,
                    service.createDraft(cashierId, body == null ? null : body.customerInfo));
        }));

        get("/orders", Routes.wrap((req, res) -> {
            Transaction.Status status = parseStatus(req.queryParams("status"));
            LocalDateTime from = parseDate(req.queryParams("from"));
            LocalDateTime to = parseDate(req.queryParams("to"));
            List<Transaction> all = service.list(status, from, to);

            if (!Roles.MANAGER.equals(AuthenticationFilter.role(req))) {
                Long uid = AuthenticationFilter.userId(req);
                all = all.stream()
                        .filter(tx -> tx.getCashier() != null && tx.getCashier().getId().equals(uid))
                        .collect(Collectors.toList());
            }

            int page = intOr(req.queryParams("page"), 1);
            int size = intOr(req.queryParams("size"), 20);
            return Responder.ok(res, paginate(all, page, size));
        }));

        get("/orders/:id", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            Transaction tx = service.getById(id);
            requireAccessible(req, tx);
            return Responder.ok(res, tx);
        }));

        post("/orders/:id/items", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            requireAccessible(req, service.getById(id));
            OrderItemRequest body = Responder.parse(req, OrderItemRequest.class);
            return Responder.ok(res, service.addItem(id, body.menuItemId, body.quantity,
                    body.modifierOptionIds, body.note));
        }));

        put("/orders/:id/items/:itemId", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            Long itemId = InputValidator.parseLongId(req.params(":itemId"), "itemId");
            requireAccessible(req, service.getById(id));
            OrderItemRequest body = Responder.parse(req, OrderItemRequest.class);
            return Responder.ok(res, service.updateItem(id, itemId, body.quantity,
                    body.modifierOptionIds, body.note));
        }));

        // Kept for older clients; drafts now void the line instead of deleting it.
        delete("/orders/:id/items/:itemId", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            Long itemId = InputValidator.parseLongId(req.params(":itemId"), "itemId");
            requireAccessible(req, service.getById(id));
            Transaction voided = service.voidItem(id, itemId, "Removed at till",
                    AuthenticationFilter.username(req));
            auditDetail(req, "order.item_voided", id,
                    Map.<String, Object>of("itemId", itemId,
                            "reason", "Removed at till"));
            return Responder.ok(res, voided);
        }));

        post("/orders/:id/items/:itemId/void", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            Long itemId = InputValidator.parseLongId(req.params(":itemId"), "itemId");
            requireAccessible(req, service.getById(id));
            VoidItemRequest body = Responder.parse(req, VoidItemRequest.class);
            Transaction voided = service.voidItem(id, itemId, body.reason,
                    AuthenticationFilter.username(req));
            auditDetail(req, "order.item_voided", id,
                    Map.<String, Object>of("itemId", itemId,
                            "reason", String.valueOf(body.reason)));
            return Responder.ok(res, voided);
        }));

        post("/orders/:id/refunds", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            requireAccessible(req, service.getById(id));
            RefundRequest body = Responder.parse(req, RefundRequest.class);
            Transaction refunded = service.refundItem(id, body.transactionItemId,
                    body.quantity, body.reason, body.paymentMethod,
                    AuthenticationFilter.username(req));
            if (!refunded.getRefunds().isEmpty()) {
                var latest = refunded.getRefunds().get(refunded.getRefunds().size() - 1);
                auditDetail(req, "order.refunded", id, Map.<String, Object>of(
                        "refundId", latest.getId(),
                        "itemId", latest.getTransactionItemId(),
                        "quantity", latest.getQuantity(),
                        "amount", latest.getAmount(),
                        "method", String.valueOf(latest.getPaymentMethod()),
                        "refundedTotal", refunded.getRefundedAmount()));
            } else {
                audit(req, "order.refunded", id, refunded.getRefundedAmount().toString());
            }
            return Responder.ok(res, refunded);
        }));

        post("/orders/:id/amend", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            requireAccessible(req, service.getById(id));
            Transaction amended = service.amend(id, AuthenticationFilter.userId(req));
            auditDetail(req, "order.amended", id,
                    Map.<String, Object>of("newOrderId", amended.getId()));
            return Responder.created(res, amended);
        }));

        post("/orders/:id/complete", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            requireAccessible(req, service.getById(id));
            CompleteOrderRequest body = Responder.parse(req, CompleteOrderRequest.class);
            Transaction completed = service.complete(id, body.paymentMethod, body.paymentAmount,
                    body.paymentReference);
            tableService.releaseSeatedTable(id);
            audit(req, "order.completed", id, completed.getTotalAmount().toString());
            return Responder.ok(res, completed);
        }));

        post("/orders/:id/cancel", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            requireAccessible(req, service.getById(id));
            Transaction cancelled = service.cancel(id);
            tableService.releaseSeatedTable(id);
            audit(req, "order.cancelled", id, null);
            return Responder.ok(res, cancelled);
        }));
    }

    private static void requireAccessible(Request req, Transaction tx) {
        if (Roles.MANAGER.equals(AuthenticationFilter.role(req))) {
            return;
        }
        // Bills seated at a table are floor-owned: any cashier can serve and settle them.
        if (tableRepository.findByTransactionId(tx.getId()).isPresent()) {
            return;
        }
        Long uid = AuthenticationFilter.userId(req);
        if (uid == null || tx.getCashier() == null || !uid.equals(tx.getCashier().getId())) {
            throw new AuthorizationException("You may only access your own orders");
        }
    }

    private static void audit(Request req, String action, Long orderId, String amount) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("orderId", orderId);
        fields.put("byUserId", AuthenticationFilter.userId(req));
        if (amount != null) {
            fields.put("amount", amount);
        }
        LoggingManager.audit(action, fields);
    }

    private static void auditDetail(Request req, String action, Long orderId,
            Map<String, Object> extra) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("orderId", orderId);
        fields.put("byUserId", AuthenticationFilter.userId(req));
        fields.putAll(extra);
        LoggingManager.audit(action, fields);
    }

    private static PageResponse<Transaction> paginate(List<Transaction> all, int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int safePage = page <= 0 ? 1 : page;
        int fromIndex = Math.min((safePage - 1) * safeSize, all.size());
        int toIndex = Math.min(fromIndex + safeSize, all.size());
        return new PageResponse<>(all.subList(fromIndex, toIndex), safePage, safeSize, all.size());
    }

    private static Transaction.Status parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Transaction.Status.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown order status: " + raw);
        }
    }

    private static LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(raw)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime();
            } catch (DateTimeParseException e2) {
                throw new ValidationException("Invalid date-time (use ISO-8601): " + raw);
            }
        }
    }

    private static int intOr(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static <T> T parseOptional(Request req, Class<T> type) {
        if (req.body() == null || req.body().isBlank()) {
            return null;
        }
        return Responder.parse(req, type);
    }
}
