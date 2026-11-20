package com.ilonggobistro.gallery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GalleryRepository extends JpaRepository<GalleryPhoto, Long> {
    List<GalleryPhoto> findByVisibleTrueOrderByDisplayOrderAscIdDesc();
    List<GalleryPhoto> findByVisibleTrueAndCategoryIgnoreCaseOrderByDisplayOrderAscIdDesc(String category);
    List<GalleryPhoto> findAllByOrderByDisplayOrderAscIdDesc();
}
