package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.dto.CategoryRequest;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.MenuCategory;
import com.anhilmy.pos.repository.MenuCategoryRepository;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceTest {

    @Mock
    private MenuCategoryRepository repository;

    private MenuCategoryService service;

    @BeforeEach
    void setUp() {
        service = new MenuCategoryService(repository);
    }

    @Test
    void createRejectsDuplicateName() {
        MenuCategory existing = new MenuCategory();
        existing.setId(5L);
        existing.setName("Drinks");
        when(repository.findByName("Drinks")).thenReturn(Optional.of(existing));

        CategoryRequest request = new CategoryRequest();
        request.name = "Drinks";

        assertThrows(BusinessRuleException.class, () -> service.create(request));
    }

    @Test
    void createPersistsCategory() {
        when(repository.findByName("Drinks")).thenReturn(Optional.empty());
        when(repository.save(any(MenuCategory.class))).thenAnswer(i -> i.getArgument(0));

        CategoryRequest request = new CategoryRequest();
        request.name = "Drinks";
        request.description = "All beverages";

        MenuCategory created = service.create(request);
        assertEquals("Drinks", created.getName());
        assertEquals(true, created.getActive());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.getById(99L));
    }
}
