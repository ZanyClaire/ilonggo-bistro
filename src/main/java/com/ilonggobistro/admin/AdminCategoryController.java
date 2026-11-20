package com.ilonggobistro.admin;

import com.ilonggobistro.menu.MenuCategory;
import com.ilonggobistro.menu.MenuCategoryForm;
import com.ilonggobistro.menu.MenuService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final MenuService menuService;

    public AdminCategoryController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public String categoriesPage(Model model) {
        model.addAttribute("categories", menuService.listCategories());
        model.addAttribute("itemCounts", menuService.countItemsPerCategory());
        if (!model.containsAttribute("categoryForm")) {
            MenuCategoryForm form = new MenuCategoryForm();
            form.setDisplayOrder(menuService.listCategories().size() + 1);
            model.addAttribute("categoryForm", form);
        }
        return "admin/categories";
    }

    @PostMapping
    public String createCategory(
            @Valid @ModelAttribute("categoryForm") MenuCategoryForm form,
            BindingResult bindingResult,
            @RequestParam(name = "returnTo", required = false, defaultValue = "/admin/categories") String returnTo,
            Model model,
            RedirectAttributes redirectAttributes) {
        String safeReturn = "/admin/menu".equals(returnTo) ? "/admin/menu" : "/admin/categories";
        if (bindingResult.hasErrors()) {
            if ("/admin/menu".equals(safeReturn)) {
                redirectAttributes.addFlashAttribute("flashError", "Please enter a category name and daypart.");
                return "redirect:/admin/menu";
            }
            model.addAttribute("categories", menuService.listCategories());
            model.addAttribute("itemCounts", menuService.countItemsPerCategory());
            return "admin/categories";
        }
        MenuCategory created = menuService.createCategory(form);
        redirectAttributes.addFlashAttribute("flashSuccess",
                "Created category \"" + created.getName() + "\" — ready for menu items and POS.");
        return "redirect:" + safeReturn;
    }

    @PostMapping("/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute MenuCategoryForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashError", "Please provide a valid category name and daypart.");
            return "redirect:/admin/categories";
        }
        MenuCategory updated = menuService.updateCategory(id, form);
        redirectAttributes.addFlashAttribute("flashSuccess", "Updated category \"" + updated.getName() + "\".");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            menuService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("flashSuccess", "Category deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
