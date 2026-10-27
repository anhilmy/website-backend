package com.anhilmy.pos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.anhilmy.pos.dto.ModifierGroupRequest;
import com.anhilmy.pos.dto.ModifierGroupResponse;
import com.anhilmy.pos.dto.ModifierOptionRequest;
import com.anhilmy.pos.dto.ModifierOptionResponse;
import com.anhilmy.pos.exception.BusinessRuleException;
import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.model.MenuItem;
import com.anhilmy.pos.model.ModifierGroup;
import com.anhilmy.pos.model.ModifierOption;
import com.anhilmy.pos.model.SelectionType;
import com.anhilmy.pos.repository.MenuItemRepository;
import com.anhilmy.pos.repository.ModifierGroupRepository;
import com.anhilmy.pos.repository.ModifierOptionRepository;
import com.anhilmy.pos.util.InputValidator;

/** CRUD and rule validation for item-scoped modifier groups and options. */
public class ModifierService {

    private final ModifierGroupRepository groupRepository;
    private final ModifierOptionRepository optionRepository;
    private final MenuItemRepository menuItemRepository;

    public ModifierService() {
        this(new ModifierGroupRepository(), new ModifierOptionRepository(),
                new MenuItemRepository());
    }

    public ModifierService(ModifierGroupRepository groupRepository,
            ModifierOptionRepository optionRepository, MenuItemRepository menuItemRepository) {
        this.groupRepository = groupRepository;
        this.optionRepository = optionRepository;
        this.menuItemRepository = menuItemRepository;
    }

    /** Active groups for the public item detail; inactive groups are hidden. */
    public List<ModifierGroupResponse> listForItem(Long menuItemId) {
        requireItem(menuItemId);
        return groupRepository.findByMenuItemId(menuItemId, true).stream()
                .map(ModifierGroupResponse::from)
                .toList();
    }

    /** Manager view: active and inactive groups. */
    public List<ModifierGroupResponse> listForManager(Long menuItemId) {
        List<ModifierGroup> groups = menuItemId == null
                ? groupRepository.findAllOrdered()
                : groupRepository.findByMenuItemId(menuItemId, false);
        return groups.stream().map(ModifierGroupResponse::from).toList();
    }

    public ModifierGroupResponse getGroup(Long id) {
        return ModifierGroupResponse.from(requireGroup(id));
    }

    public ModifierGroupResponse createGroup(ModifierGroupRequest request) {
        Long menuItemId = InputValidator.requireNonNull(request.menuItemId, "menuItemId");
        MenuItem item = requireItem(menuItemId);

        ModifierGroup group = new ModifierGroup();
        group.setMenuItem(item);
        group.setCategory(null);
        group.setName(InputValidator.requireMaxLength(request.name, "name", 64));
        group.setSelectionType(request.selectionType == null
                ? SelectionType.SINGLE
                : request.selectionType);
        group.setRequired(request.required != null && request.required);
        group.setMinSelect(request.minSelect == null ? 0 : request.minSelect);
        group.setMaxSelect(request.maxSelect == null ? 1 : request.maxSelect);
        group.setSortOrder(request.sortOrder == null ? 0 : request.sortOrder);
        group.setActive(request.active == null || request.active);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        normalize(group);
        return ModifierGroupResponse.from(groupRepository.save(group));
    }

    public ModifierGroupResponse updateGroup(Long id, ModifierGroupRequest request) {
        ModifierGroup group = requireGroup(id);
        if (request.menuItemId != null) {
            group.setMenuItem(requireItem(request.menuItemId));
            group.setCategory(null);
        }
        if (request.name != null) {
            group.setName(InputValidator.requireMaxLength(request.name, "name", 64));
        }
        if (request.selectionType != null) {
            group.setSelectionType(request.selectionType);
        }
        if (request.required != null) {
            group.setRequired(request.required);
        }
        if (request.minSelect != null) {
            group.setMinSelect(request.minSelect);
        }
        if (request.maxSelect != null) {
            group.setMaxSelect(request.maxSelect);
        }
        if (request.sortOrder != null) {
            group.setSortOrder(request.sortOrder);
        }
        if (request.active != null) {
            group.setActive(request.active);
        }
        normalize(group);
        group.setUpdatedAt(LocalDateTime.now());
        return ModifierGroupResponse.from(groupRepository.update(group));
    }

    /** Soft delete: the group stops being offered while historic lines keep their snapshots. */
    public void deactivateGroup(Long id) {
        ModifierGroup group = requireGroup(id);
        group.setActive(false);
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.update(group);
    }

    public ModifierOptionResponse createOption(Long groupId, ModifierOptionRequest request) {
        ModifierGroup group = requireGroup(groupId);
        if (!Boolean.TRUE.equals(group.getActive())) {
            throw new BusinessRuleException("Cannot add options to an inactive group");
        }
        ModifierOption option = new ModifierOption();
        option.setGroup(group);
        option.setName(InputValidator.requireMaxLength(request.name, "name", 64));
        option.setPriceDelta(request.priceDelta == null ? BigDecimal.ZERO : request.priceDelta);
        option.setIsDefault(request.isDefault != null && request.isDefault);
        option.setAvailable(request.available == null || request.available);
        option.setSortOrder(request.sortOrder == null ? 0 : request.sortOrder);
        if (!Boolean.TRUE.equals(option.getAvailable())) {
            option.setIsDefault(false);
        }
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        ModifierOption saved = optionRepository.save(option);
        enforceSingleDefault(saved);
        return ModifierOptionResponse.from(saved);
    }

    public ModifierOptionResponse updateOption(Long id, ModifierOptionRequest request) {
        ModifierOption option = requireOption(id);
        if (request.name != null) {
            option.setName(InputValidator.requireMaxLength(request.name, "name", 64));
        }
        if (request.priceDelta != null) {
            option.setPriceDelta(request.priceDelta);
        }
        if (request.isDefault != null) {
            option.setIsDefault(request.isDefault);
        }
        if (request.available != null) {
            option.setAvailable(request.available);
        }
        if (request.sortOrder != null) {
            option.setSortOrder(request.sortOrder);
        }
        if (Boolean.FALSE.equals(option.getAvailable())) {
            option.setIsDefault(false);
        }
        option.setUpdatedAt(LocalDateTime.now());
        ModifierOption saved = optionRepository.update(option);
        enforceSingleDefault(saved);
        return ModifierOptionResponse.from(saved);
    }

    /** Soft delete: option stays on historic snapshots but is no longer selectable. */
    public void deactivateOption(Long id) {
        ModifierOption option = requireOption(id);
        option.setAvailable(false);
        option.setIsDefault(false);
        option.setUpdatedAt(LocalDateTime.now());
        optionRepository.update(option);
    }

    private ModifierGroup requireGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ModifierGroup", id));
    }

    private ModifierOption requireOption(Long id) {
        return optionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ModifierOption", id));
    }

    private MenuItem requireItem(Long id) {
        Long itemId = InputValidator.requireNonNull(id, "menuItemId");
        return menuItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem", itemId));
    }

    private void enforceSingleDefault(ModifierOption option) {
        if (option.getGroup() == null
                || option.getGroup().getSelectionType() != SelectionType.SINGLE
                || !Boolean.TRUE.equals(option.getIsDefault())) {
            return;
        }
        Long groupId = option.getGroup().getId();
        if (groupId == null) {
            return;
        }
        for (ModifierOption other : optionRepository.findByGroupId(groupId)) {
            if (other.getId() != null && !other.getId().equals(option.getId())
                    && Boolean.TRUE.equals(other.getIsDefault())) {
                other.setIsDefault(false);
                other.setUpdatedAt(LocalDateTime.now());
                optionRepository.update(other);
            }
        }
    }

    /**
     * Enforces the group invariants: SINGLE means at most one choice, MULTI allows a range, and
     * a required group always demands at least one selection.
     */
    private static void normalize(ModifierGroup group) {
        SelectionType type = group.getSelectionType() == null
                ? SelectionType.SINGLE
                : group.getSelectionType();
        int min = group.getMinSelect() == null ? 0 : group.getMinSelect();
        int max = group.getMaxSelect() == null ? 1 : group.getMaxSelect();

        if (type == SelectionType.SINGLE) {
            max = 1;
            if (min > 1) {
                min = 1;
            }
        }
        if (min < 0) {
            throw new ValidationException("minSelect must not be negative");
        }
        if (max < 1) {
            throw new ValidationException("maxSelect must be at least 1");
        }
        if (Boolean.TRUE.equals(group.getRequired()) && min < 1) {
            min = 1;
        }
        if (min > max) {
            throw new ValidationException("minSelect must not exceed maxSelect");
        }
        group.setSelectionType(type);
        group.setMinSelect(min);
        group.setMaxSelect(max);
    }
}
