package com.anhilmy.pos.controller;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.service.ReportService;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.get;

/** Manager-only reconciliation summary and bill-level CSV export. */
public class ReportController {

    private static final ReportService service = new ReportService();

    public static void registerRoutes() {
        get("/reports/payments", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            return Responder.ok(res, service.paymentSummary(
                    parseDate(req.queryParams("from")),
                    parseDate(req.queryParams("to"))));
        }));

        // Registered before OrderController's /orders/:id so "export" is not read as an id.
        get("/orders/export", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            String format = req.queryParams("format");
            if (format != null && !format.isBlank() && !format.equalsIgnoreCase("csv")) {
                throw new ValidationException("Only format=csv is supported");
            }
            String csv = service.ordersCsv(
                    parseDate(req.queryParams("from")),
                    parseDate(req.queryParams("to")),
                    parseStatus(req.queryParams("status")));
            res.type("text/csv");
            res.header("Content-Disposition", "attachment; filename=\"orders.csv\"");
            return csv;
        }));
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
}
