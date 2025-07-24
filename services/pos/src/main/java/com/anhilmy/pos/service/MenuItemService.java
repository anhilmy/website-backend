package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.util.List;

import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuRepository;

public class MenuItemService {

    private final MenuRepository menuRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public MenuItemService() {
        this.menuRepository = new MenuRepository();
        this.menuCategoryRepository = new MenuCategoryRepository();
    }

    public List<MenuItem> getAllMenuItems() {
        return menuRepository.findAll();
    }

    public MenuItem getMenuItemById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MenuItem with id " + id + " not found."));
    }

    public MenuItem createMenuItem(String name, String description, BigDecimal price, int categoryId,
            boolean isAvailable) {
        MenuCategory menuCategory = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("MenuCategory with id " + categoryId + " not found."));

        MenuItem menuItem = new MenuItem(name, description, price, menuCategory, isAvailable);
        menuRepository.save(menuItem);
        return menuItem;
    }

    public MenuItem updateMenuItem(Long id, String name, String description, BigDecimal price, int categoryId,
            boolean isAvailable) {
        MenuItem existingMenuItem = getMenuItemById(id);

        MenuCategory menuCategory = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("MenuCategory with id " + categoryId + " not found."));

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