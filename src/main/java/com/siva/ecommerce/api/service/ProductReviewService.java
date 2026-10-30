package com.siva.ecommerce.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siva.ecommerce.api.dto.ReviewRequest;
import com.siva.ecommerce.api.dto.ReviewResponse;
import com.siva.ecommerce.api.entity.Customer;
import com.siva.ecommerce.api.entity.Product;
import com.siva.ecommerce.api.entity.ProductReview;
import com.siva.ecommerce.api.exception.BusinessRuleException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CustomerRepository;
import com.siva.ecommerce.api.repository.ProductRepository;
import com.siva.ecommerce.api.repository.ProductReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReviewService {

	private final ProductReviewRepository productReviewRepository;
	private final ProductRepository productRepository;
	private final CustomerRepository customerRepository;

	@Transactional
	public ReviewResponse create(Long productId, ReviewRequest request) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", productId));
		if (!product.getActive()) {
			throw new BusinessRuleException("Product " + product.getSku() + " is not available for review");
		}
		Customer customer = customerRepository.findById(request.getCustomerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));

		ProductReview review = ProductReview.builder()
				.product(product)
				.customer(customer)
				.rating(request.getRating())
				.comment(request.getComment())
				.build();
		return ReviewResponse.from(productReviewRepository.save(review));
	}

	/** Lists reviews even for inactive products, so review history stays visible. */
	public Page<ReviewResponse> getByProduct(Long productId, Pageable pageable) {
		if (!productRepository.existsById(productId)) {
			throw new ResourceNotFoundException("Product", productId);
		}
		return productReviewRepository.findByProductId(productId, pageable).map(ReviewResponse::from);
	}

}
