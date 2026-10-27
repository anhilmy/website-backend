package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.dto.ModifierGroupRequest;
import com.anhilmy.pos.dto.ModifierGroupResponse;
import com.anhilmy.pos.dto.ModifierOptionRequest;
import com.anhilmy.pos.dto.ModifierOptionResponse;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.ModifierGroup;
import com.anhilmy.pos.model.ModifierOption;
import com.anhilmy.pos.model.SelectionType;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.ModifierGroupRepository;
import com.anhilmy.pos.repository.ModifierOptionRepository;

@ExtendWith(MockitoExtension.class)
class ModifierServiceTest {

    @Mock
    private ModifierGroupRepository groupRepository;
    @Mock
    private ModifierOptionRepository optionRepository;
    @Mock
    private MenuItemRepository menuItemRepository;

    private ModifierService service;

    @BeforeEach
    void setUp() {
        service = new ModifierService(groupRepository, optionRepository, menuItemRepository);
    }

    private MenuItem item(long id) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName("Espresso");
        return item;
    }

    private ModifierGroup group(long id, SelectionType type, boolean required, int min, int max) {
        ModifierGroup group = new ModifierGroup();
        group.setId(id);
        group.setName("Group");
        group.setSelectionType(type);
        group.setRequired(required);
        group.setMinSelect(min);
        group.setMaxSelect(max);
        group.setSortOrder(0);
        group.setActive(true);
        group.setMenuItem(item(10L));
        return group;
    }

    @Test
    void createGroupNormalizesSingleAndRequiredRules() {
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item(10L)));
        when(groupRepository.save(any(ModifierGroup.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ModifierGroupRequest request = new ModifierGroupRequest();
        request.menuItemId = 10L;
        request.name = "Temperature";
        request.selectionType = SelectionType.SINGLE;
        request.required = true;
        request.minSelect = 0;
        request.maxSelect = 5;

        ModifierGroupResponse created = service.createGroup(request);

        assertEquals("SINGLE", created.selectionType);
        assertEquals(1, created.minSelect);
        assertEquals(1, created.maxSelect);
        assertTrue(created.required);
        assertTrue(created.active);
    }

    @Test
    void createGroupRejectsMinGreaterThanMax() {
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item(10L)));

        ModifierGroupRequest request = new ModifierGroupRequest();
        request.menuItemId = 10L;
        request.name = "Extras";
        request.selectionType = SelectionType.MULTI;
        request.minSelect = 3;
        request.maxSelect = 2;

        assertThrows(ValidationException.class, () -> service.createGroup(request));
    }

    @Test
    void createGroupRequiresExistingItem() {
        when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

        ModifierGroupRequest request = new ModifierGroupRequest();
        request.menuItemId = 99L;
        request.name = "Temperature";

        assertThrows(com.anhilmy.pos.exception.EntityNotFoundException.class,
                () -> service.createGroup(request));
    }

    @Test
    void createOptionClearsOtherDefaultsInSingleGroup() {
        ModifierGroup group = group(5L, SelectionType.SINGLE, false, 0, 1);
        ModifierOption existingDefault = new ModifierOption();
        existingDefault.setId(1L);
        existingDefault.setGroup(group);
        existingDefault.setName("Hot");
        existingDefault.setIsDefault(true);
        existingDefault.setAvailable(true);

        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(optionRepository.save(any(ModifierOption.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(optionRepository.findByGroupId(5L)).thenReturn(List.of(existingDefault));

        ModifierOptionRequest request = new ModifierOptionRequest();
        request.name = "Iced";
        request.isDefault = true;

        ModifierOptionResponse created = service.createOption(5L, request);

        assertTrue(created.isDefault);
        assertFalse(existingDefault.getIsDefault());
    }

    @Test
    void createOptionRejectsInactiveGroup() {
        ModifierGroup group = group(5L, SelectionType.SINGLE, false, 0, 1);
        group.setActive(false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));

        ModifierOptionRequest request = new ModifierOptionRequest();
        request.name = "Iced";

        assertThrows(BusinessRuleException.class, () -> service.createOption(5L, request));
    }

    @Test
    void deactivateGroupAndOptionAreSoft() {
        ModifierGroup group = group(5L, SelectionType.SINGLE, false, 0, 1);
        ModifierOption option = new ModifierOption();
        option.setId(2L);
        option.setGroup(group);
        option.setAvailable(true);
        option.setIsDefault(true);

        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(groupRepository.update(group)).thenReturn(group);
        when(optionRepository.findById(2L)).thenReturn(Optional.of(option));
        when(optionRepository.update(option)).thenReturn(option);

        service.deactivateGroup(5L);
        service.deactivateOption(2L);

        assertFalse(group.getActive());
        assertFalse(option.getAvailable());
        assertFalse(option.getIsDefault());
    }

    @Test
    void createOptionNeverDefaultsAnUnavailableOption() {
        ModifierGroup group = group(5L, SelectionType.SINGLE, false, 0, 1);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(optionRepository.save(any(ModifierOption.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ModifierOptionRequest request = new ModifierOptionRequest();
        request.name = "Sold out";
        request.isDefault = true;
        request.available = false;

        ModifierOptionResponse created = service.createOption(5L, request);

        assertFalse(created.isDefault);
        assertFalse(created.available);
    }

    @Test
    void updatingAnOptionToUnavailableClearsItsDefaultFlag() {
        ModifierGroup group = group(5L, SelectionType.SINGLE, false, 0, 1);
        ModifierOption option = new ModifierOption();
        option.setId(2L);
        option.setGroup(group);
        option.setName("Hot");
        option.setAvailable(true);
        option.setIsDefault(true);

        when(optionRepository.findById(2L)).thenReturn(Optional.of(option));
        when(optionRepository.update(option)).thenReturn(option);

        ModifierOptionRequest request = new ModifierOptionRequest();
        request.available = false;

        ModifierOptionResponse updated = service.updateOption(2L, request);

        assertFalse(updated.available);
        assertFalse(updated.isDefault);
    }

    @Test
    void listForItemMapsOptionsSortedBySortOrder() {
        ModifierGroup group = group(5L, SelectionType.SINGLE, false, 0, 1);
        ModifierOption second = new ModifierOption();
        second.setId(2L);
        second.setGroup(group);
        second.setName("Iced");
        second.setSortOrder(20);
        second.setPriceDelta(BigDecimal.ZERO);
        second.setAvailable(true);
        ModifierOption first = new ModifierOption();
        first.setId(1L);
        first.setGroup(group);
        first.setName("Hot");
        first.setSortOrder(10);
        first.setPriceDelta(BigDecimal.ZERO);
        first.setAvailable(true);
        group.setOptions(List.of(second, first));

        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item(10L)));
        when(groupRepository.findByMenuItemId(10L, true)).thenReturn(List.of(group));

        List<ModifierGroupResponse> groups = service.listForItem(10L);

        assertEquals(1, groups.size());
        assertEquals("Hot", groups.get(0).options.get(0).name);
        assertEquals("Iced", groups.get(0).options.get(1).name);
    }
}
