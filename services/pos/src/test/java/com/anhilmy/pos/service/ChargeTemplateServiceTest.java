package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.dto.ChargeTemplateRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.model.ChargeBasis;
import com.anhilmy.pos.model.ChargeCalcType;
import com.anhilmy.pos.model.ChargeTemplate;
import com.anhilmy.pos.repository.ChargeTemplateRepository;

@ExtendWith(MockitoExtension.class)
class ChargeTemplateServiceTest {

    @Mock
    private ChargeTemplateRepository repository;

    private ChargeTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ChargeTemplateService(repository);
    }

    private ChargeTemplate template(long id, String code) {
        ChargeTemplate t = new ChargeTemplate();
        t.setId(id);
        t.setCode(code);
        t.setLabel(code);
        t.setCalcType(ChargeCalcType.PERCENTAGE);
        t.setValue(new BigDecimal("0.10"));
        t.setBasis(ChargeBasis.SUBTOTAL);
        t.setSortOrder(0);
        t.setTaxable(false);
        t.setIsTax(false);
        t.setActive(true);
        return t;
    }

    @Test
    void createNormalizesCodeAndDefaults() {
        when(repository.findByCode("SERVICE")).thenReturn(Optional.empty());
        when(repository.save(any(ChargeTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        ChargeTemplateRequest request = new ChargeTemplateRequest();
        request.code = " service ";
        request.label = "Service";
        request.value = new BigDecimal("0.05");

        ChargeTemplate created = service.create(request);

        assertEquals("SERVICE", created.getCode());
        assertEquals(ChargeCalcType.PERCENTAGE, created.getCalcType());
        assertEquals(ChargeBasis.SUBTOTAL, created.getBasis());
        assertTrue(created.getActive());
        assertFalse(created.getTaxable());
        assertFalse(created.getIsTax());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("TAX")).thenReturn(Optional.of(template(1L, "TAX")));

        ChargeTemplateRequest request = new ChargeTemplateRequest();
        request.code = "tax";
        request.label = "Tax";

        assertThrows(BusinessRuleException.class, () -> service.create(request));
    }

    @Test
    void createRejectsPercentageAboveOne() {
        when(repository.findByCode("BIG")).thenReturn(Optional.empty());

        ChargeTemplateRequest request = new ChargeTemplateRequest();
        request.code = "BIG";
        request.label = "Big";
        request.calcType = ChargeCalcType.PERCENTAGE;
        request.value = new BigDecimal("10");

        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void createRejectsNegativeValue() {
        when(repository.findByCode("TAX")).thenReturn(Optional.empty());

        ChargeTemplateRequest request = new ChargeTemplateRequest();
        request.code = "TAX";
        request.label = "Tax";
        request.value = new BigDecimal("-1");

        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void updateRejectsCodeOwnedByAnotherTemplate() {
        when(repository.findById(1L)).thenReturn(Optional.of(template(1L, "TAX")));
        when(repository.findByCode("SERVICE")).thenReturn(Optional.of(template(2L, "SERVICE")));

        ChargeTemplateRequest request = new ChargeTemplateRequest();
        request.code = "SERVICE";

        assertThrows(BusinessRuleException.class, () -> service.update(1L, request));
    }

    @Test
    void deactivateMarksTemplateInactive() {
        ChargeTemplate existing = template(1L, "TAX");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.update(existing)).thenReturn(existing);

        service.deactivate(1L);

        assertFalse(existing.getActive());
    }
}
