package com.siva.ecommerce.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.siva.ecommerce.api.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findBySku(String sku);

	boolean existsBySku(String sku);

	Page<Product> findByActiveTrue(Pageable pageable);

	Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

}
