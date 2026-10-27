package com.anhilmy.pos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.anhilmy.pos.dto.PaymentMethodRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.model.PaymentMethod;
import com.anhilmy.pos.repository.PaymentMethodRepository;
import com.anhilmy.pos.util.InputValidator;

/** CRUD and validation for the accepted {@link PaymentMethod} set. */
public class PaymentMethodService {

    private static final String CODE_PATTERN = "^[A-Z0-9_]{2,32}$";

    private final PaymentMethodRepository repository;

    public PaymentMethodService() {
        this(new PaymentMethodRepository());
    }

    public PaymentMethodService(PaymentMethodRepository repository) {
        this.repository = repository;
    }

    public List<PaymentMethod> list(Boolean activeOnly) {
        if (activeOnly != null) {
            return repository.findByActive(activeOnly);
        }
        return repository.findAllOrdered();
    }

    public PaymentMethod getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PaymentMethod", id));
    }

    public PaymentMethod create(PaymentMethodRequest request) {
        String code = normalizeCode(request.code);
        String label = InputValidator.requireMaxLength(request.label, "label", 64);
        repository.findByCode(code).ifPresent(existing -> {
            throw new BusinessRuleException("Payment method code already exists: " + code);
        });

        PaymentMethod method = new PaymentMethod();
        method.setCode(code);
        method.setLabel(label);
        method.setActive(request.active == null || request.active);
        method.setIsCash(request.isCash != null && request.isCash);
        method.setRequiresReference(
                request.requiresReference != null && request.requiresReference);
        method.setSortOrder(request.sortOrder == null ? 0 : request.sortOrder);
        method.setCreatedAt(LocalDateTime.now());
        method.setUpdatedAt(LocalDateTime.now());
        return repository.save(method);
    }

    public PaymentMethod update(Long id, PaymentMethodRequest request) {
        PaymentMethod method = getById(id);
        if (request.code != null) {
            String code = normalizeCode(request.code);
            repository.findByCode(code).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BusinessRuleException("Payment method code already exists: " + code);
                }
            });
            method.setCode(code);
        }
        if (request.label != null) {
            method.setLabel(InputValidator.requireMaxLength(request.label, "label", 64));
        }
        if (request.active != null) {
            method.setActive(request.active);
        }
        if (request.isCash != null) {
            method.setIsCash(request.isCash);
        }
        if (request.requiresReference != null) {
            method.setRequiresReference(request.requiresReference);
        }
        if (request.sortOrder != null) {
            method.setSortOrder(request.sortOrder);
        }
        method.setUpdatedAt(LocalDateTime.now());
        return repository.update(method);
    }

    /** Soft delete: keeps the method on historic bills but removes it from the till. */
    public void deactivate(Long id) {
        PaymentMethod method = getById(id);
        method.setActive(false);
        method.setUpdatedAt(LocalDateTime.now());
        repository.update(method);
    }

    /** Resolves a settlement method, rejecting unknown or inactive codes. */
    public PaymentMethod requireActive(String rawCode) {
        String code = normalizeCode(rawCode);
        return repository.findByCode(code)
                .filter(method -> Boolean.TRUE.equals(method.getActive()))
                .orElseThrow(() -> new BusinessRuleException(
                        "Unknown or inactive payment method: " + code));
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
}
