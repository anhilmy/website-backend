package com.anhilmy.pos.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.Transaction;
import com.anhilmy.pos.model.TransactionItem;
import com.anhilmy.pos.model.User;

class JsonTest {

    @Test
    void userPasswordHashIsNeverSerialized() {
        User user = new User();
        user.setId(1L);
        user.setUsername("manager");
        user.setPasswordHash("$2a$12$supersecrethash");
        user.setRole("MANAGER");

        String json = Json.gson().toJson(user);
        assertFalse(json.contains("passwordHash"), "passwordHash must not appear in JSON");
        assertFalse(json.contains("supersecrethash"), "hash value must not appear in JSON");
        assertTrue(json.contains("manager"));
    }

    @Test
    void categoryParentChainSerializesWithoutRecursion() {
        MenuCategory parent = new MenuCategory();
        parent.setId(1L);
        parent.setName("Food");
        MenuCategory child = new MenuCategory();
        child.setId(2L);
        child.setName("Desserts");
        child.setParent(parent);

        String json = Json.gson().toJson(child);
        assertTrue(json.contains("Desserts"));
        assertTrue(json.contains("Food"), "parent chain should be exposed for hierarchy");
    }

    @Test
    void transactionWithItemsSerializesWithoutRecursion() {
        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setStatus(Transaction.Status.DRAFT);
        tx.setTotalAmount(new BigDecimal("22.00"));

        MenuItem item = new MenuItem();
        item.setId(5L);
        item.setName("Cake");
        item.setPrice(new BigDecimal("10.00"));

        TransactionItem line = new TransactionItem();
        line.setMenuItem(item);
        line.setQuantity(2);
        line.setUnitPrice(new BigDecimal("10.00"));
        line.setTotalPrice(new BigDecimal("20.00"));
        tx.setItems(List.of(line));
        line.setTransaction(tx);

        String json = Json.gson().toJson(tx);
        assertTrue(json.contains("Cake"));
        // The back-reference is skipped, so no cycle.
        assertFalse(json.contains("\"transaction\""));
    }
}
