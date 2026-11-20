package com.ilonggobistro.admin;

import com.ilonggobistro.menu.MenuCategoryForm;
import com.ilonggobistro.menu.MenuItem;
import com.ilonggobistro.menu.MenuItemForm;
import com.ilonggobistro.menu.MenuService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/menu")
public class AdminMenuController {

    private final MenuService menuService;

    public AdminMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public String listMenu(Model model) {
        model.addAttribute("items", menuService.listAllItems());
        model.addAttribute("categories", menuService.listCategories());
        if (!model.containsAttribute("menuItemForm")) {
            model.addAttribute("menuItemForm", new MenuItemForm());
        }
        if (!model.containsAttribute("categoryForm")) {
            MenuCategoryForm catForm = new MenuCategoryForm();
            catForm.setDisplayOrder(menuService.listCategories().size() + 1);
            model.addAttribute("categoryForm", catForm);
        }
        return "admin/menu";
    }

    @GetMapping("/{id}/edit")
    public String editMenuForm(@PathVariable Long id, Model model) {
        MenuItem item = menuService.getById(id);
        model.addAttribute("editingItem", item);
        model.addAttribute("menuItemForm", MenuItemForm.fromEntity(item));
        model.addAttribute("categories", menuService.listCategories());
        return "admin/menu-edit";
    }

    @PostMapping
    public String createMenuItem(
            @Valid @ModelAttribute("menuItemForm") MenuItemForm form,
            BindingResult bindingResult,
            @RequestParam(name = "photo", required = false) MultipartFile photo,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("items", menuService.listAllItems());
            model.addAttribute("categories", menuService.listCategories());
            return "admin/menu";
        }
        try {
            menuService.createItem(form, photo);
            redirectAttributes.addFlashAttribute("flashSuccess", "Added \"" + form.getName() + "\" to the menu.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/menu";
    }

    @PostMapping("/{id}")
    public String updateMenuItem(
            @PathVariable Long id,
            @Valid @ModelAttribute("menuItemForm") MenuItemForm form,
            BindingResult bindingResult,
            @RequestParam(name = "photo", required = false) MultipartFile photo,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editingItem", menuService.getById(id));
            model.addAttribute("categories", menuService.listCategories());
            return "admin/menu-edit";
        }
        try {
            menuService.updateItem(id, form, photo);
            redirectAttributes.addFlashAttribute("flashSuccess", "Updated \"" + form.getName() + "\".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/menu";
    }

    @PostMapping("/{id}/toggle-availability")
    public String toggleAvailability(
            @PathVariable Long id,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        MenuItem updated = menuService.toggleAvailability(id);
        if ("true".equals(hxRequest)) {
            model.addAttribute("item", updated);
            return "admin/fragments/menu-row :: row";
        }
        redirectAttributes.addFlashAttribute("flashSuccess",
                updated.getName() + " marked " + (updated.isAvailable() ? "Available" : "Sold Out") + ".");
        return "redirect:/admin/menu";
    }

    @PostMapping("/{id}/toggle-featured")
    public String toggleFeatured(
            @PathVariable Long id,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        MenuItem updated = menuService.toggleFeatured(id);
        if ("true".equals(hxRequest)) {
            model.addAttribute("item", updated);
            return "admin/fragments/menu-row :: row";
        }
        redirectAttributes.addFlashAttribute("flashSuccess",
                updated.getName() + (updated.isFeatured() ? " pinned to Home." : " unpinned from Home."));
        return "redirect:/admin/menu";
    }

    @PostMapping("/{id}/delete")
    public String deleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        menuService.deleteItem(id);
        redirectAttributes.addFlashAttribute("flashSuccess", "Menu item removed.");
        return "redirect:/admin/menu";
    }
}
