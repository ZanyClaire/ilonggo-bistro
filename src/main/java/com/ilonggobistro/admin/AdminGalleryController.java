package com.ilonggobistro.admin;

import com.ilonggobistro.gallery.GalleryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/gallery")
public class AdminGalleryController {

    private final GalleryService galleryService;

    public AdminGalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping
    public String listGallery(Model model) {
        model.addAttribute("photos", galleryService.listAll());
        model.addAttribute("categories", GalleryService.CATEGORIES);
        return "admin/gallery";
    }

    @PostMapping
    public String addPhoto(
            @RequestParam("title") String title,
            @RequestParam(name = "caption", required = false) String caption,
            @RequestParam("category") String category,
            @RequestParam(name = "imageUrl", required = false) String imageUrl,
            @RequestParam(name = "file", required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            galleryService.addPhoto(title, caption, category, imageUrl, file);
            redirectAttributes.addFlashAttribute("flashSuccess", "Photo added to gallery.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/gallery";
    }

    @PostMapping("/{id}/toggle")
    public String toggleVisibility(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        galleryService.toggleVisibility(id);
        redirectAttributes.addFlashAttribute("flashSuccess", "Photo visibility updated.");
        return "redirect:/admin/gallery";
    }

    @PostMapping("/{id}/delete")
    public String deletePhoto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        galleryService.deletePhoto(id);
        redirectAttributes.addFlashAttribute("flashSuccess", "Photo deleted.");
        return "redirect:/admin/gallery";
    }
}
