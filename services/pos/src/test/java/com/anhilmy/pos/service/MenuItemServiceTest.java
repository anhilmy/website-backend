package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.repository.MenuCategoryRepository;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.ModifierGroupRepository;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private MenuCategoryRepository menuCategoryRepository;
    @Mock
    private ModifierGroupRepository modifierGroupRepository;

    private MenuItemService service;

    @BeforeEach
    void setUp() {
        service = new MenuItemService(menuItemRepository, menuCategoryRepository,
                modifierGroupRepository);
    }

    private MenuItem item(long id) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName("Item " + id);
        return item;
    }

    @Test
    void listFlagsOnlyItemsWithActiveGroups() {
        MenuItem customizable = item(1L);
        MenuItem plain = item(2L);
        when(menuItemRepository.search(null, null, null, null, null))
                .thenReturn(List.of(customizable, plain));
        when(modifierGroupRepository.menuItemIdsWithActiveGroups()).thenReturn(List.of(1L));

        List<MenuItem> items = service.list(null, null, null, null, null);

        assertTrue(items.get(0).getHasModifiers());
        assertFalse(items.get(1).getHasModifiers());
    }

    @Test
    void getByIdFlagsModifiers() {
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(item(1L)));
        when(modifierGroupRepository.hasActiveGroups(1L)).thenReturn(true);

        assertTrue(service.getById(1L).getHasModifiers());
    }
}
