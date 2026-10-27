package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.dto.PaymentReport;
import com.anhilmy.pos.dto.PaymentSummary;
import com.anhilmy.pos.model.Refund;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(transactionRepository);
    }

    private Transaction tx(long id, Transaction.Status status, String method, String total,
            String refunded, String customer) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setStatus(status);
        tx.setPaymentMethod(method);
        tx.setTotalAmount(new BigDecimal(total));
        tx.setRefundedAmount(new BigDecimal(refunded));
        tx.setPaymentAmount(new BigDecimal(total));
        tx.setCustomerInfo(customer);
        tx.setCreatedAt(LocalDateTime.of(2026, 10, 23, 10, 0));
        User cashier = new User();
        cashier.setUsername("manager");
        tx.setCashier(cashier);
        return tx;
    }

    @Test
    void paymentSummaryGroupsSettledBillsAndRefunds() {
        when(transactionRepository.findFiltered(null, null, null)).thenReturn(List.of(
                tx(1, Transaction.Status.COMPLETED, "CASH", "100.00", "0", "A"),
                tx(2, Transaction.Status.PARTIALLY_REFUNDED, "CARD", "200.00", "50.00", "B"),
                tx(3, Transaction.Status.DRAFT, "CASH", "10.00", "0", "C"),
                tx(4, Transaction.Status.REFUNDED, "CARD", "80.00", "80.00", "D")));

        PaymentReport report = service.paymentSummary(null, null);

        assertEquals(2, report.rows.size());
        PaymentSummary cash = report.rows.stream()
                .filter(row -> row.method.equals("CASH")).findFirst().orElseThrow();
        assertEquals(1, cash.count);
        assertEquals(0, new BigDecimal("100.00").compareTo(cash.net));

        PaymentSummary card = report.rows.stream()
                .filter(row -> row.method.equals("CARD")).findFirst().orElseThrow();
        assertEquals(2, card.count);
        assertEquals(0, new BigDecimal("280.00").compareTo(card.gross));
        assertEquals(0, new BigDecimal("130.00").compareTo(card.refunded));
        assertEquals(0, new BigDecimal("150.00").compareTo(card.net));

        assertEquals(3, report.totals.count);
        assertEquals(0, new BigDecimal("380.00").compareTo(report.totals.gross));
        assertEquals(0, new BigDecimal("130.00").compareTo(report.totals.refunded));
        assertEquals(0, new BigDecimal("250.00").compareTo(report.totals.net));
    }

    @Test
    void ordersCsvIncludesHeaderAndEscapesCustomer() {
        when(transactionRepository.findFiltered(null, null, null)).thenReturn(List.of(
                tx(1, Transaction.Status.COMPLETED, "CASH", "100.00", "0", "Doe, Jane")));

        String csv = service.ordersCsv(null, null, null);
        String[] lines = csv.split("\n");

        assertTrue(lines[0].startsWith("id,createdAt,status,cashier,customer"));
        assertTrue(lines[1].contains("\"Doe, Jane\""));
        assertTrue(lines[1].contains("COMPLETED"));
        assertTrue(lines[1].contains("100.00"));
    }

    @Test
    void refundsAreGroupedByPayoutMethod() {
        Transaction tx = tx(1, Transaction.Status.COMPLETED, "CASH", "100.00", "50.00", "A");
        Refund refund = new Refund();
        refund.setPaymentMethod("CARD");
        refund.setAmount(new BigDecimal("50.00"));
        refund.setQuantity(1);
        tx.addRefund(refund);
        when(transactionRepository.findFiltered(null, null, null)).thenReturn(List.of(tx));

        PaymentReport report = service.paymentSummary(null, null);

        assertEquals(2, report.rows.size());
        PaymentSummary cash = report.rows.stream()
                .filter(row -> row.method.equals("CASH")).findFirst().orElseThrow();
        assertEquals(1, cash.count);
        assertEquals(0, new BigDecimal("100.00").compareTo(cash.net));

        PaymentSummary card = report.rows.stream()
                .filter(row -> row.method.equals("CARD")).findFirst().orElseThrow();
        assertEquals(0, card.count);
        assertEquals(0, new BigDecimal("50.00").compareTo(card.refunded));
        assertEquals(0, new BigDecimal("-50.00").compareTo(card.net));

        assertEquals(1, report.totals.count);
        assertEquals(0, new BigDecimal("100.00").compareTo(report.totals.gross));
        assertEquals(0, new BigDecimal("50.00").compareTo(report.totals.refunded));
        assertEquals(0, new BigDecimal("50.00").compareTo(report.totals.net));
    }

    @Test
    void csvNeutralisesFormulaCells() {
        when(transactionRepository.findFiltered(null, null, null)).thenReturn(List.of(
                tx(1, Transaction.Status.COMPLETED, "CASH", "100.00", "0", "=cmd|calc"),
                tx(2, Transaction.Status.COMPLETED, "CASH", "100.00", "0", "+123"),
                tx(3, Transaction.Status.COMPLETED, "CASH", "100.00", "0", "-123"),
                tx(4, Transaction.Status.COMPLETED, "CASH", "100.00", "0", "@x")));

        String csv = service.ordersCsv(null, null, null);

        assertTrue(csv.contains("'=cmd|calc"));
        assertTrue(csv.contains("'+123"));
        assertTrue(csv.contains("'-123"));
        assertTrue(csv.contains("'@x"));
    }
}
