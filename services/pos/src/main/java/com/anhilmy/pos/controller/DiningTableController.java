package com.anhilmy.pos.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.anhilmy.pos.dto.AssignTableRequest;
import com.anhilmy.pos.dto.JoinTableRequest;
import com.anhilmy.pos.dto.OpenTableRequest;
import com.anhilmy.pos.dto.TableRequest;
import com.anhilmy.pos.dto.TableResponse;
import com.anhilmy.pos.dto.TableStatusRequest;
import com.anhilmy.pos.exception.AuthorizationException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.TableStatus;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.service.DiningTableService;
import com.anhilmy.pos.service.TransactionService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.patch;
import static spark.Spark.post;
import static spark.Spark.put;

/**
 * Floor-table endpoints. Cashiers operate tables (open, assign, release, join); layout
 * changes (create/update/delete) are manager-only. Responses use {@link TableResponse} so no
 * full order (customer data, line items) leaks through the floor view.
 */
public class DiningTableController {

    private static final DiningTableService service = new DiningTableService();
    private static final TransactionService transactionService = new TransactionService();

    public static void registerRoutes() {
        get("/tables", Routes.wrap((req, res) -> {
            String area = req.queryParams("area");
            TableStatus status = parseStatusOrNull(req.queryParams("status"));
            List<TableResponse> body = service.list(area, status).stream()
                    .map(TableResponse::from)
                    .collect(Collectors.toList());
            return Responder.ok(res, body);
        }));

        get("/tables/:id", Routes.wrap((req, res) ->
                Responder.ok(res, TableResponse.from(
                        service.getById(InputValidator.parseLongId(req.params(":id"), "id"))))));

        post("/tables", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            return Responder.created(res,
                    TableResponse.from(service.create(Responder.parse(req, TableRequest.class))));
        }));

        put("/tables/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res,
                    TableResponse.from(service.update(id, Responder.parse(req, TableRequest.class))));
        }));

        delete("/tables/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            service.delete(InputValidator.parseLongId(req.params(":id"), "id"));
            return Responder.noContent(res);
        }));

        patch("/tables/:id/status", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            TableStatusRequest body = Responder.parse(req, TableStatusRequest.class);
            return Responder.ok(res, TableResponse.from(service.setStatus(id, parseStatus(body.status))));
        }));

        post("/tables/:id/open", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            OpenTableRequest body = parseOptional(req, OpenTableRequest.class);
            Long cashierId = AuthenticationFilter.userId(req);
            return Responder.ok(res, TableResponse.from(
                    service.open(id, cashierId, body == null ? null : body.customerInfo)));
        }));

        post("/tables/:id/assign", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            AssignTableRequest body = Responder.parse(req, AssignTableRequest.class);
            Long transactionId = InputValidator.requireNonNull(body.transactionId, "transactionId");
            requireOrderAccess(req, transactionId);
            return Responder.ok(res, TableResponse.from(service.assign(id, transactionId)));
        }));

        post("/tables/:id/release", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res, TableResponse.from(service.release(id)));
        }));

        post("/tables/:id/join", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            JoinTableRequest body = Responder.parse(req, JoinTableRequest.class);
            Long targetId = InputValidator.requireNonNull(body.targetTableId, "targetTableId");
            return Responder.ok(res, TableResponse.from(service.join(id, targetId)));
        }));

        post("/tables/:id/unjoin", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res, TableResponse.from(service.unjoin(id)));
        }));
    }

    /** A cashier may only seat their own orders; managers may seat any. */
    private static void requireOrderAccess(spark.Request req, Long transactionId) {
        if (Roles.MANAGER.equals(AuthenticationFilter.role(req))) {
            return;
        }
        Transaction order = transactionService.getById(transactionId);
        Long uid = AuthenticationFilter.userId(req);
        if (uid == null || order.getCashier() == null || !uid.equals(order.getCashier().getId())) {
            throw new AuthorizationException("You may only seat your own orders");
        }
    }

    private static TableStatus parseStatusOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseStatus(raw);
    }

    private static TableStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("status is required");
        }
        try {
            return TableStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown table status: " + raw);
        }
    }

    private static <T> T parseOptional(spark.Request req, Class<T> type) {
        if (req.body() == null || req.body().isBlank()) {
            return null;
        }
        return Responder.parse(req, type);
    }
}
