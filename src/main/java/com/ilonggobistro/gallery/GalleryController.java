package com.ilonggobistro.gallery;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/gallery")
public class GalleryController {

    private final GalleryService galleryService;

    public GalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping
    public String galleryPage(
            @RequestParam(name = "category", required = false, defaultValue = "All") String category,
            Model model) {
        populateModel(category, model);
        return "pages/gallery";
    }

    @GetMapping("/fragment")
    public String galleryFragment(
            @RequestParam(name = "category", required = false, defaultValue = "All") String category,
            Model model) {
        populateModel(category, model);
        return "fragments/gallery-grid :: grid";
    }

    private void populateModel(String category, Model model) {
        model.addAttribute("categories", GalleryService.CATEGORIES);
        model.addAttribute("activeCategory", category == null || category.isBlank() ? "All" : category);
        model.addAttribute("photos", galleryService.listVisible(category));
    }
}
