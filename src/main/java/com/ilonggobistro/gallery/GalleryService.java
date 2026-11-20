package com.ilonggobistro.gallery;

import com.ilonggobistro.shared.ImageStorageService;
import com.ilonggobistro.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class GalleryService {

    public static final List<String> CATEGORIES = List.of("Food & Drink", "Vibe", "Events");

    private final GalleryRepository repository;
    private final ImageStorageService imageStorageService;

    public GalleryService(GalleryRepository repository, ImageStorageService imageStorageService) {
        this.repository = repository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public List<GalleryPhoto> listVisible(String category) {
        if (category == null || category.isBlank() || "All".equalsIgnoreCase(category)) {
            return repository.findByVisibleTrueOrderByDisplayOrderAscIdDesc();
        }
        return repository.findByVisibleTrueAndCategoryIgnoreCaseOrderByDisplayOrderAscIdDesc(category.trim());
    }

    @Transactional(readOnly = true)
    public List<GalleryPhoto> listAll() {
        return repository.findAllByOrderByDisplayOrderAscIdDesc();
    }

    @Transactional
    public GalleryPhoto addPhoto(String title, String caption, String category, String imageUrl, MultipartFile file) {
        String uploadedUrl = imageStorageService.storeImage(file);
        String resolvedUrl = uploadedUrl != null ? uploadedUrl : imageUrl;
        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            throw new IllegalArgumentException("Please upload a photo or provide an image URL.");
        }
        GalleryPhoto photo = new GalleryPhoto(
                title.trim(),
                caption != null ? caption.trim() : "",
                category.trim(),
                resolvedUrl.trim(),
                0,
                true
        );
        return repository.save(photo);
    }

    @Transactional
    public GalleryPhoto toggleVisibility(Long id) {
        GalleryPhoto photo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery photo not found."));
        photo.setVisible(!photo.isVisible());
        return repository.save(photo);
    }

    @Transactional
    public void deletePhoto(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Gallery photo not found.");
        }
        repository.deleteById(id);
    }
}
