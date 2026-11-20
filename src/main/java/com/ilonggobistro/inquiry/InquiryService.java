package com.ilonggobistro.inquiry;

import com.ilonggobistro.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InquiryService {

    public static final List<String> TOPICS = List.of(
            "General Question",
            "Group / Family Celebration (20+ Guests)",
            "Private Event / Catering",
            "Feedback & Suggestions"
    );

    private final InquiryRepository repository;

    public InquiryService(InquiryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ContactInquiry submit(InquiryRequest request) {
        ContactInquiry inquiry = new ContactInquiry(
                request.getName().trim(),
                request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null,
                request.getEmail().trim(),
                request.getInquiryType().trim(),
                request.getMessage().trim()
        );
        return repository.save(inquiry);
    }

    @Transactional(readOnly = true)
    public List<ContactInquiry> listAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        return repository.countByReadFalse();
    }

    @Transactional
    public ContactInquiry toggleRead(Long id) {
        ContactInquiry inquiry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found."));
        inquiry.setRead(!inquiry.isRead());
        return repository.save(inquiry);
    }
}
