package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.anhilmy.pos.dto.MenuItemRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.ModifierGroupRepository;
import com.anhilmy.pos.util.InputValidator;

/** CRUD, search and availability management for {@link MenuItem}. */
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final ModifierGroupRepository modifierGroupRepository;

    public MenuItemService() {
        this(new MenuItemRepository(), new MenuCategoryRepository(),
                new ModifierGroupRepository());
    }

    public MenuItemService(MenuItemRepository menuItemRepository,
            MenuCategoryRepository menuCategoryRepository,
            ModifierGroupRepository modifierGroupRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.modifierGroupRepository = modifierGroupRepository;
    }

    public List<MenuItem> list(String search, Long categoryId, Boolean available,
            BigDecimal minPrice, BigDecimal maxPrice) {
        List<MenuItem> items = menuItemRepository.search(search, categoryId, available, minPrice,
                maxPrice);
        applyHasModifiers(items);
        return items;
    }

    public List<MenuItem> listPublic() {
        List<MenuItem> items = menuItemRepository.findByAvailability(true);
        applyHasModifiers(items);
        return items;
    }

    public MenuItem getById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem", id));
        item.setHasModifiers(modifierGroupRepository.hasActiveGroups(id));
        return item;
    }

    private void applyHasModifiers(List<MenuItem> items) {
        if (items.isEmpty()) {
            return;
        }
        Set<Long> withModifiers = new HashSet<>(
                modifierGroupRepository.menuItemIdsWithActiveGroups());
        for (MenuItem item : items) {
            item.setHasModifiers(withModifiers.contains(item.getId()));
        }
    }

    public MenuItem create(MenuItemRequest request) {
        String name = InputValidator.requireMaxLength(request.name, "name", 255);
        BigDecimal price = InputValidator.requirePositive(request.price, "price");
        MenuCategory category = resolveCategory(request.categoryId);

        MenuItem item = new MenuItem();
        item.setName(name);
        item.setDescription(InputValidator.sanitizeMax(request.description, "description", 255));
        item.setPrice(price);
        item.setMenuCategory(category);
        item.setAvailable(request.available == null || request.available);
        item.setImageUrl(InputValidator.sanitizeMax(request.imageUrl, "imageUrl", 255));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        return menuItemRepository.save(item);
    }

    public MenuItem update(Long id, MenuItemRequest request) {
        MenuItem item = getById(id);
        if (request.name != null) {
            item.setName(InputValidator.requireMaxLength(request.name, "name", 255));
        }
        if (request.description != null) {
            item.setDescription(InputValidator.sanitizeMax(request.description, "description", 255));
        }
        if (request.price != null) {
            item.setPrice(InputValidator.requirePositive(request.price, "price"));
        }
        if (request.categoryId != null) {
            item.setMenuCategory(resolveCategory(request.categoryId));
        }
        if (request.available != null) {
            item.setAvailable(request.available);
        }
        if (request.imageUrl != null) {
            item.setImageUrl(InputValidator.sanitizeMax(request.imageUrl, "imageUrl", 255));
        }
        item.setUpdatedAt(LocalDateTime.now());
        item.setHasModifiers(modifierGroupRepository.hasActiveGroups(id));
        return menuItemRepository.update(item);
    }

    /** Soft delete: item becomes unavailable but remains referenced by past orders. */
    public void deactivate(Long id) {
        MenuItem item = getById(id);
        item.setAvailable(false);
        item.setUpdatedAt(LocalDateTime.now());
        menuItemRepository.update(item);
    }

    private MenuCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessRuleException("categoryId is required");
        }
        return menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("MenuCategory", categoryId));
    }
}
