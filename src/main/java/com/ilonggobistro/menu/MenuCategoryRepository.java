package com.ilonggobistro.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    List<MenuCategory> findAllByOrderByDisplayOrderAscNameAsc();
    Optional<MenuCategory> findBySlug(String slug);
}
