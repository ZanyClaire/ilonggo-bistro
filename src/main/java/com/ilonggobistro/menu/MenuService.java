package com.ilonggobistro.menu;

import com.ilonggobistro.shared.ImageStorageService;
import com.ilonggobistro.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final ImageStorageService imageStorageService;

    public MenuService(MenuCategoryRepository categoryRepository,
                       MenuItemRepository itemRepository,
                       ImageStorageService imageStorageService) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public List<MenuCategory> listCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countItemsPerCategory() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (MenuCategory category : listCategories()) {
            counts.put(category.getId(), itemRepository.countByCategoryId(category.getId()));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public MenuCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found."));
    }

    @Transactional
    public MenuCategory createCategory(MenuCategoryForm form) {
        String name = form.getName().trim();
        String baseSlug = slugify(name);
        String uniqueSlug = baseSlug;
        int suffix = 2;
        while (categoryRepository.findBySlug(uniqueSlug).isPresent()) {
            uniqueSlug = baseSlug + "-" + suffix++;
        }
        int order = form.getDisplayOrder() > 0 ? form.getDisplayOrder() : (int) categoryRepository.count() + 1;
        MenuCategory category = new MenuCategory(
                name,
                uniqueSlug,
                form.getDaypart().trim(),
                form.getDescription() != null ? form.getDescription().trim() : "",
                order
        );
        return categoryRepository.save(category);
    }

    @Transactional
    public MenuCategory updateCategory(Long id, MenuCategoryForm form) {
        MenuCategory category = getCategoryById(id);
        category.setName(form.getName().trim());
        category.setDaypart(form.getDaypart().trim());
        category.setDescription(form.getDescription() != null ? form.getDescription().trim() : "");
        category.setDisplayOrder(form.getDisplayOrder());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        MenuCategory category = getCategoryById(id);
        long itemCount = itemRepository.countByCategoryId(id);
        if (itemCount > 0) {
            throw new IllegalArgumentException("Cannot delete \"" + category.getName()
                    + "\" while " + itemCount + " dish(es) belong to it. Move or remove those dishes first.");
        }
        categoryRepository.delete(category);
    }

    private String slugify(String input) {
        String slug = input.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "category" : slug;
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listFeaturedAvailable() {
        return itemRepository.findByFeaturedTrueAndAvailableTrueOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listAllItems() {
        return itemRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<MenuItem> searchMenu(String categorySlug, String query, boolean onlyAvailable) {
        String cleanSlug = (categorySlug == null || categorySlug.isBlank() || "all".equalsIgnoreCase(categorySlug))
                ? null : categorySlug.trim();
        String cleanQuery = (query == null || query.isBlank()) ? null : query.trim();
        return itemRepository.searchMenu(cleanSlug, cleanQuery, onlyAvailable);
    }

    @Transactional(readOnly = true)
    public MenuItem getById(Long id) {
        return itemRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
    }

    @Transactional
    public MenuItem createItem(MenuItemForm form, MultipartFile photo) {
        MenuCategory category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found."));

        String uploadedUrl = imageStorageService.storeImage(photo);
        String imageUrl = uploadedUrl != null ? uploadedUrl : cleanImageUrl(form.getImageUrl());

        MenuItem item = new MenuItem(
                category,
                form.getName().trim(),
                form.getDescription().trim(),
                form.getPricePhp(),
                imageUrl,
                form.getBadgeLabel(),
                form.isFeatured(),
                form.isAvailable(),
                form.getDisplayOrder()
        );
        return itemRepository.save(item);
    }

    @Transactional
    public MenuItem updateItem(Long id, MenuItemForm form, MultipartFile photo) {
        MenuItem item = getById(id);
        MenuCategory category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found."));

        String uploadedUrl = imageStorageService.storeImage(photo);
        if (uploadedUrl != null) {
            item.setImageUrl(uploadedUrl);
        } else if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
            item.setImageUrl(cleanImageUrl(form.getImageUrl()));
        }

        item.setCategory(category);
        item.setName(form.getName().trim());
        item.setDescription(form.getDescription().trim());
        item.setPricePhp(form.getPricePhp());
        item.setBadgeLabel(form.getBadgeLabel());
        item.setFeatured(form.isFeatured());
        item.setAvailable(form.isAvailable());
        item.setDisplayOrder(form.getDisplayOrder());
        return itemRepository.save(item);
    }

    @Transactional
    public MenuItem toggleAvailability(Long id) {
        MenuItem item = getById(id);
        item.setAvailable(!item.isAvailable());
        return itemRepository.save(item);
    }

    @Transactional
    public MenuItem toggleFeatured(Long id) {
        MenuItem item = getById(id);
        item.setFeatured(!item.isFeatured());
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        MenuItem item = getById(id);
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public long countAvailable() {
        return itemRepository.countByAvailableTrue();
    }

    @Transactional(readOnly = true)
    public long countSoldOut() {
        return itemRepository.countByAvailableFalse();
    }

    private String cleanImageUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "/images/dish-batchoy.jpg";
        }
        return raw.trim();
    }
}
