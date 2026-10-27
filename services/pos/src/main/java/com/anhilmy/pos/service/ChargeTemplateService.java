package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.anhilmy.pos.dto.ChargeTemplateRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.model.ChargeBasis;
import com.anhilmy.pos.model.ChargeCalcType;
import com.anhilmy.pos.model.ChargeTemplate;
import com.anhilmy.pos.repository.ChargeTemplateRepository;
import com.anhilmy.pos.util.InputValidator;

/** CRUD and validation for reusable {@link ChargeTemplate} definitions. */
public class ChargeTemplateService {

    private static final String CODE_PATTERN = "^[A-Z0-9_]{2,32}$";

    private final ChargeTemplateRepository repository;

    public ChargeTemplateService() {
        this(new ChargeTemplateRepository());
    }

    public ChargeTemplateService(ChargeTemplateRepository repository) {
        this.repository = repository;
    }

    public List<ChargeTemplate> list(Boolean activeOnly) {
        if (activeOnly != null) {
            return repository.findByActive(activeOnly);
        }
        return repository.findAllOrdered();
    }

    public ChargeTemplate getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ChargeTemplate", id));
    }

    public ChargeTemplate create(ChargeTemplateRequest request) {
        String code = normalizeCode(request.code);
        repository.findByCode(code).ifPresent(existing -> {
            throw new BusinessRuleException("Charge template code already exists: " + code);
        });

        ChargeCalcType calcType = defaultCalcType(request.calcType);
        BigDecimal value = requireNonNegative(request.value);
        validateValue(calcType, value);

        ChargeTemplate template = new ChargeTemplate();
        template.setCode(code);
        template.setLabel(InputValidator.requireMaxLength(request.label, "label", 64));
        template.setCalcType(calcType);
        template.setValue(value);
        template.setBasis(defaultBasis(request.basis));
        template.setSortOrder(request.sortOrder == null ? 0 : request.sortOrder);
        template.setTaxable(request.taxable != null && request.taxable);
        template.setIsTax(request.isTax != null && request.isTax);
        template.setActive(request.active == null || request.active);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return repository.save(template);
    }

    public ChargeTemplate update(Long id, ChargeTemplateRequest request) {
        ChargeTemplate template = getById(id);
        if (request.code != null) {
            String code = normalizeCode(request.code);
            repository.findByCode(code).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BusinessRuleException("Charge template code already exists: " + code);
                }
            });
            template.setCode(code);
        }
        if (request.label != null) {
            template.setLabel(InputValidator.requireMaxLength(request.label, "label", 64));
        }
        if (request.calcType != null) {
            template.setCalcType(request.calcType);
        }
        if (request.value != null) {
            template.setValue(requireNonNegative(request.value));
        }
        validateValue(template.getCalcType(), template.getValue());
        if (request.basis != null) {
            template.setBasis(request.basis);
        }
        if (request.sortOrder != null) {
            template.setSortOrder(request.sortOrder);
        }
        if (request.taxable != null) {
            template.setTaxable(request.taxable);
        }
        if (request.isTax != null) {
            template.setIsTax(request.isTax);
        }
        if (request.active != null) {
            template.setActive(request.active);
        }
        template.setUpdatedAt(LocalDateTime.now());
        return repository.update(template);
    }

    /** Soft delete: stops the template from being copied onto new orders. */
    public void deactivate(Long id) {
        ChargeTemplate template = getById(id);
        template.setActive(false);
        template.setUpdatedAt(LocalDateTime.now());
        repository.update(template);
    }

    private static String normalizeCode(String raw) {
        String code = InputValidator.requireNonBlank(raw, "code")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (!code.matches(CODE_PATTERN)) {
            throw new ValidationException(
                    "code must be 2-32 characters of A-Z, 0-9 or underscore");
        }
        return code;
    }

    private static BigDecimal requireNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.signum() < 0) {
            throw new ValidationException("value must be zero or greater");
        }
        return value;
    }

    /** A percentage is a fraction: 0.10 is 10%, 1.00 is 100%. */
    private static void validateValue(ChargeCalcType type, BigDecimal value) {
        if (type == ChargeCalcType.PERCENTAGE && value != null
                && value.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException(
                    "percentage value must be between 0 and 1 (0.10 = 10%)");
        }
    }

    private static ChargeCalcType defaultCalcType(ChargeCalcType value) {
        return value == null ? ChargeCalcType.PERCENTAGE : value;
    }

    private static ChargeBasis defaultBasis(ChargeBasis value) {
        return value == null ? ChargeBasis.SUBTOTAL : value;
    }
}
