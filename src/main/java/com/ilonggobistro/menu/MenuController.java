package com.ilonggobistro.menu;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public String menuPage(
            @RequestParam(name = "category", required = false, defaultValue = "all") String category,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "availableOnly", required = false, defaultValue = "false") boolean availableOnly,
            Model model) {
        populateMenuModel(category, query, availableOnly, model);
        return "pages/menu";
    }

    @GetMapping("/fragment")
    public String menuFragment(
            @RequestParam(name = "category", required = false, defaultValue = "all") String category,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "availableOnly", required = false, defaultValue = "false") boolean availableOnly,
            Model model) {
        populateMenuModel(category, query, availableOnly, model);
        return "fragments/menu-grid :: grid";
    }

    private void populateMenuModel(String category, String query, boolean availableOnly, Model model) {
        List<MenuCategory> categories = menuService.listCategories();
        List<MenuItem> items = menuService.searchMenu(category, query, availableOnly);
        model.addAttribute("categories", categories);
        model.addAttribute("menuItems", items);
        model.addAttribute("activeCategory", category == null || category.isBlank() ? "all" : category);
        model.addAttribute("searchQuery", query == null ? "" : query);
        model.addAttribute("availableOnly", availableOnly);
    }
}
