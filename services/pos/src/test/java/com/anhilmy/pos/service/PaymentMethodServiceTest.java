package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.dto.PaymentMethodRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.repository.PaymentMethodRepository;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository repository;

    private PaymentMethodService service;

    @BeforeEach
    void setUp() {
        service = new PaymentMethodService(repository);
    }

    private PaymentMethod method(long id, String code, boolean active) {
        PaymentMethod m = new PaymentMethod();
        m.setId(id);
        m.setCode(code);
        m.setLabel(code);
        m.setActive(active);
        m.setIsCash(false);
        m.setRequiresReference(false);
        m.setSortOrder(0);
        return m;
    }

    @Test
    void createNormalizesCodeAndDefaults() {
        when(repository.findByCode("QRIS")).thenReturn(Optional.empty());
        when(repository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodRequest request = new PaymentMethodRequest();
        request.code = " qris ";
        request.label = "QRIS";

        PaymentMethod created = service.create(request);

        assertEquals("QRIS", created.getCode());
        assertTrue(created.getActive());
        assertFalse(created.getIsCash());
        assertFalse(created.getRequiresReference());
        assertEquals(0, created.getSortOrder());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("CASH")).thenReturn(Optional.of(method(1L, "CASH", true)));

        PaymentMethodRequest request = new PaymentMethodRequest();
        request.code = "cash";
        request.label = "Cash";

        assertThrows(BusinessRuleException.class, () -> service.create(request));
    }

    @Test
    void updateRejectsCodeOwnedByAnotherMethod() {
        when(repository.findById(1L)).thenReturn(Optional.of(method(1L, "CASH", true)));
        when(repository.findByCode("CARD")).thenReturn(Optional.of(method(2L, "CARD", true)));

        PaymentMethodRequest request = new PaymentMethodRequest();
        request.code = "CARD";

        assertThrows(BusinessRuleException.class, () -> service.update(1L, request));
    }

    @Test
    void requireActiveRejectsInactiveMethod() {
        when(repository.findByCode("CARD")).thenReturn(Optional.of(method(1L, "CARD", false)));

        assertThrows(BusinessRuleException.class, () -> service.requireActive("card"));
    }

    @Test
    void requireActiveRejectsUnknownMethod() {
        when(repository.findByCode("BITCOIN")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.requireActive("bitcoin"));
    }

    @Test
    void deactivateMarksMethodInactive() {
        PaymentMethod existing = method(1L, "QRIS", true);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.update(existing)).thenReturn(existing);

        service.deactivate(1L);

        assertFalse(existing.getActive());
    }
}
