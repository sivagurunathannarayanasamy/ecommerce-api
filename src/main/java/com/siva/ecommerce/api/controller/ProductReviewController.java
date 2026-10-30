package com.siva.ecommerce.api.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.siva.ecommerce.api.dto.ReviewRequest;
import com.siva.ecommerce.api.dto.ReviewResponse;
import com.siva.ecommerce.api.service.ProductReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

	private final ProductReviewService productReviewService;

	@PostMapping
	public ResponseEntity<ReviewResponse> create(@PathVariable Long productId,
			@Valid @RequestBody ReviewRequest request) {
		ReviewResponse response = productReviewService.create(productId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(response.getId()).toUri();
		return ResponseEntity.created(location).body(response);
	}

	// id breaks ties: created_at is DATETIME (whole seconds), so reviews posted together share a timestamp
	@GetMapping
	public Page<ReviewResponse> getByProduct(@PathVariable Long productId,
			@PageableDefault(size = 20, sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
		return productReviewService.getByProduct(productId, pageable);
	}

}
