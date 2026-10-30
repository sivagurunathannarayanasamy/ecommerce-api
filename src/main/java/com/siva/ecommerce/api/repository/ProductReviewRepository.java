package com.siva.ecommerce.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.siva.ecommerce.api.entity.ProductReview;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

	@EntityGraph(attributePaths = "customer")
	Page<ProductReview> findByProductId(Long productId, Pageable pageable);

}
