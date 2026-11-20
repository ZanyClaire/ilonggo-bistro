package com.ilonggobistro.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<ContactInquiry, Long> {
    List<ContactInquiry> findAllByOrderByCreatedAtDesc();
    long countByReadFalse();
}
