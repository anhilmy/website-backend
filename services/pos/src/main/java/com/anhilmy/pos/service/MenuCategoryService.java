package com.anhilmy.pos.service;

import java.time.LocalDateTime;
import java.util.List;

import com.anhilmy.pos.dto.CategoryRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.util.InputValidator;

/** CRUD and business rules for {@link MenuCategory}. */
public class MenuCategoryService {

    private final MenuCategoryRepository categoryRepository;

    public MenuCategoryService() {
        this(new MenuCategoryRepository());
    }

    public MenuCategoryService(MenuCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<MenuCategory> list(Boolean activeOnly) {
        if (activeOnly != null) {
            return categoryRepository.findByActive(activeOnly);
        }
        return categoryRepository.findAll();
    }

    public MenuCategory getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuCategory", id));
    }

    public MenuCategory create(CategoryRequest request) {
        String name = InputValidator.requireNonBlank(request.name, "name");
        categoryRepository.findByName(name).ifPresent(c -> {
            throw new BusinessRuleException("Category name already exists: " + name);
        });

        MenuCategory category = new MenuCategory();
        category.setName(name);
        category.setDescription(InputValidator.sanitize(request.description));
        category.setActive(request.active == null || request.active);
        category.setParent(resolveParent(request.parentId));
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return categoryRepository.save(category);
    }

    public MenuCategory update(Long id, CategoryRequest request) {
        MenuCategory category = getById(id);
        if (request.name != null) {
            String name = InputValidator.requireNonBlank(request.name, "name");
            categoryRepository.findByName(name).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BusinessRuleException("Category name already exists: " + name);
                }
            });
            category.setName(name);
        }
        if (request.description != null) {
            category.setDescription(InputValidator.sanitize(request.description));
        }
        if (request.active != null) {
            category.setActive(request.active);
        }
        if (request.parentId != null) {
            category.setParent(resolveParent(request.parentId));
        }
        category.setUpdatedAt(LocalDateTime.now());
        return categoryRepository.update(category);
    }

    /** Soft delete: marks the category inactive so existing items keep a valid reference. */
    public void deactivate(Long id) {
        MenuCategory category = getById(id);
        category.setActive(false);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.update(category);
    }

    private MenuCategory resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent MenuCategory", parentId));
    }
}
