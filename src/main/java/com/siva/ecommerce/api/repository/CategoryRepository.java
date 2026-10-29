package com.siva.ecommerce.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siva.ecommerce.api.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findBySlug(String slug);

	boolean existsByName(String name);

	boolean existsBySlug(String slug);

	boolean existsByNameAndIdNot(String name, Long id);

	boolean existsBySlugAndIdNot(String slug, Long id);

}
