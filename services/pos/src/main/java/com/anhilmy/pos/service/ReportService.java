package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.anhilmy.pos.dto.PaymentReport;
import com.anhilmy.pos.dto.PaymentSummary;
import com.anhilmy.pos.model.Refund;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.repository.TransactionRepository;

/** Reconciliation summaries and bill-level CSV export. */
public class ReportService {

    private static final List<Transaction.Status> SETTLED = List.of(
            Transaction.Status.COMPLETED,
            Transaction.Status.PARTIALLY_REFUNDED,
            Transaction.Status.REFUNDED);

    private final TransactionRepository transactionRepository;

    public ReportService() {
        this(new TransactionRepository());
    }

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Totals grouped by payment method for settled bills in the range. Sales count against the
     * method that was charged; refunds count against the method the money went back through.
     */
    public PaymentReport paymentSummary(LocalDateTime from, LocalDateTime to) {
        List<Transaction> transactions = transactionRepository.findFiltered(null, from, to);
        Map<String, PaymentSummary> byMethod = new TreeMap<>();
        PaymentReport report = new PaymentReport();
        report.from = from;
        report.to = to;

        for (Transaction tx : transactions) {
            if (!SETTLED.contains(tx.getStatus())) {
                continue;
            }
            String saleMethod = methodOf(tx);
            BigDecimal gross = amount(tx.getTotalAmount());
            byMethod.computeIfAbsent(saleMethod, PaymentSummary::new).addBill(gross);
            report.totals.addBill(gross);

            List<Refund> refunds = tx.getRefunds();
            if (refunds == null || refunds.isEmpty()) {
                // Bills settled before the refunds table existed only have the aggregate.
                BigDecimal legacy = amount(tx.getRefundedAmount());
                if (legacy.signum() > 0) {
                    byMethod.computeIfAbsent(saleMethod, PaymentSummary::new).addRefund(legacy);
                    report.totals.addRefund(legacy);
                }
            } else {
                for (Refund refund : refunds) {
                    String method = refund.getPaymentMethod() == null
                            || refund.getPaymentMethod().isBlank()
                            ? saleMethod
                            : refund.getPaymentMethod();
                    BigDecimal refunded = amount(refund.getAmount());
                    byMethod.computeIfAbsent(method, PaymentSummary::new).addRefund(refunded);
                    report.totals.addRefund(refunded);
                }
            }
        }

        report.rows = new ArrayList<>(byMethod.values());
        report.rows.sort(Comparator.comparing(row -> row.method));
        return report;
    }

    /** Bill-level CSV for reconciliation. */
    public String ordersCsv(LocalDateTime from, LocalDateTime to, Transaction.Status status) {
        List<Transaction> transactions = transactionRepository.findFiltered(status, from, to);
        StringBuilder csv = new StringBuilder();
        csv.append("id,createdAt,status,cashier,customer,subtotal,charges,total,refunded,")
                .append("refundMethods,net,paymentMethod,paymentReference,tendered,change\n");

        for (Transaction tx : transactions) {
            BigDecimal total = amount(tx.getTotalAmount());
            BigDecimal refunded = amount(tx.getRefundedAmount());
            BigDecimal net = total.subtract(refunded);
            BigDecimal tendered = tx.getPaymentAmount();
            BigDecimal change = tendered == null ? BigDecimal.ZERO : tendered.subtract(total);

            csv.append(cell(tx.getId())).append(',')
                    .append(cell(tx.getCreatedAt() == null ? null
                            : tx.getCreatedAt().atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                    .append(',')
                    .append(cell(tx.getStatus())).append(',')
                    .append(cell(tx.getCashier() == null ? null : tx.getCashier().getUsername()))
                    .append(',')
                    .append(cell(tx.getCustomerInfo())).append(',')
                    .append(cell(tx.getSubtotalAmount())).append(',')
                    .append(cell(tx.getChargesAmount())).append(',')
                    .append(cell(total)).append(',')
                    .append(cell(refunded)).append(',')
                    .append(cell(refundMethods(tx))).append(',')
                    .append(cell(net)).append(',')
                    .append(cell(tx.getPaymentMethod())).append(',')
                    .append(cell(tx.getPaymentReference())).append(',')
                    .append(cell(tendered)).append(',')
                    .append(cell(change)).append('\n');
        }
        return csv.toString();
    }

    private static String refundMethods(Transaction tx) {
        if (tx.getRefunds() == null || tx.getRefunds().isEmpty()) {
            return "";
        }
        Map<String, BigDecimal> byMethod = new TreeMap<>();
        for (Refund refund : tx.getRefunds()) {
            String method = refund.getPaymentMethod() == null
                    || refund.getPaymentMethod().isBlank()
                    ? methodOf(tx)
                    : refund.getPaymentMethod();
            byMethod.merge(method, amount(refund.getAmount()), BigDecimal::add);
        }
        StringBuilder out = new StringBuilder();
        byMethod.forEach((method, value) -> {
            if (!out.isEmpty()) {
                out.append("; ");
            }
            out.append(method).append('=').append(value);
        });
        return out.toString();
    }

    private static String methodOf(Transaction tx) {
        String method = tx.getPaymentMethod();
        return method == null || method.isBlank() ? "UNKNOWN" : method;
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** CSV cell with formula-injection neutralisation. */
    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\u0000", "");
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n")
                || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
