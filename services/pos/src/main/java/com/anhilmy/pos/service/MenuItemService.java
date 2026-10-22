package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuRepository;

public class MenuItemService {

    private final MenuRepository menuRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public MenuItemService(MenuRepository menuRepository,
            MenuCategoryRepository menuCategoryRepository) {
        this.menuRepository = menuRepository;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    public List<MenuItem> getAllMenuItems() {
        return menuRepository.findAll();
    }

    public MenuItem getMenuItemById(Long id) {
        return menuRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("MenuItem with id " + id + " not found."));
    }

    public MenuItem createMenuItem(String name, String description, BigDecimal price,
            int categoryId, boolean isAvailable) {
        MenuCategory menuCategory = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MenuCategory with id " + categoryId + " not found."));

        MenuItem menuItem = new MenuItem();
        menuItem.setName(name);
        menuItem.setDescription(description);
        menuItem.setPrice(price);
        menuItem.setMenuCategory(menuCategory);
        menuItem.setAvailable(isAvailable);

        // Set created and updated timestamps
        menuItem.setCreatedAt(LocalDateTime.now());
        menuItem.setUpdatedAt(LocalDateTime.now());
        menuRepository.save(menuItem);
        return menuItem;
    }

    public MenuItem updateMenuItem(Long id, String name, String description, BigDecimal price,
            int categoryId, boolean isAvailable) {
        MenuItem existingMenuItem = getMenuItemById(id);

        MenuCategory menuCategory = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MenuCategory with id " + categoryId + " not found."));

        existingMenuItem.setName(name);
        existingMenuItem.setDescription(description);
        existingMenuItem.setPrice(price);
        existingMenuItem.setMenuCategory(menuCategory);
        existingMenuItem.setAvailable(isAvailable);

        menuRepository.update(existingMenuItem);
        return existingMenuItem;
    }

    public void deleteMenuItem(Long id) {
        MenuItem menuItemToDelete = getMenuItemById(id);
        menuRepository.delete(menuItemToDelete);
    }
}
