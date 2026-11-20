package com.ilonggobistro.inquiry;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/contact")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public String contactPage(Model model) {
        if (!model.containsAttribute("inquiryRequest")) {
            model.addAttribute("inquiryRequest", new InquiryRequest());
        }
        model.addAttribute("topics", InquiryService.TOPICS);
        return "pages/contact";
    }

    @PostMapping
    public String submitInquiry(
            @Valid @ModelAttribute("inquiryRequest") InquiryRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("topics", InquiryService.TOPICS);
            return "pages/contact";
        }
        inquiryService.submit(request);
        redirectAttributes.addFlashAttribute("contactSuccess",
                "Salamat! We received your message and our team at Burgos-Osmeña St will get back to you shortly.");
        return "redirect:/contact";
    }
}
