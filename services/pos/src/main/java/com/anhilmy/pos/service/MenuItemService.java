package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.anhilmy.pos.dto.MenuItemRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.util.InputValidator;

/** CRUD, search and availability management for {@link MenuItem}. */
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public MenuItemService() {
        this(new MenuItemRepository(), new MenuCategoryRepository());
    }

    public MenuItemService(MenuItemRepository menuItemRepository,
            MenuCategoryRepository menuCategoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    public List<MenuItem> list(String search, Long categoryId, Boolean available,
            BigDecimal minPrice, BigDecimal maxPrice) {
        return menuItemRepository.search(search, categoryId, available, minPrice, maxPrice);
    }

    public List<MenuItem> listPublic() {
        return menuItemRepository.findByAvailability(true);
    }

    public MenuItem getById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem", id));
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
