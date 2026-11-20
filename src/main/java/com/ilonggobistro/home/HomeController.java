package com.ilonggobistro.home;

import com.ilonggobistro.gallery.GalleryService;
import com.ilonggobistro.menu.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private final MenuService menuService;
    private final GalleryService galleryService;

    public HomeController(MenuService menuService, GalleryService galleryService) {
        this.menuService = menuService;
        this.galleryService = galleryService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredItems", menuService.listFeaturedAvailable().stream().limit(6).toList());
        model.addAttribute("categories", menuService.listCategories());
        model.addAttribute("galleryHighlights", galleryService.listVisible("All").stream().limit(4).toList());
        return "pages/home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("categories", menuService.listCategories());
        return "pages/about";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                ? "" : ":" + request.getServerPort());
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url><loc>%s/</loc><priority>1.0</priority></url>
                  <url><loc>%s/menu</loc><priority>0.9</priority></url>
                  <url><loc>%s/reservations</loc><priority>0.9</priority></url>
                  <url><loc>%s/gallery</loc><priority>0.7</priority></url>
                  <url><loc>%s/events</loc><priority>0.8</priority></url>
                  <url><loc>%s/about</loc><priority>0.7</priority></url>
                  <url><loc>%s/contact</loc><priority>0.7</priority></url>
                </urlset>
                """.formatted(baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);
    }
}
