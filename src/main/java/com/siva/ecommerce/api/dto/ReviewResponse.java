package com.siva.ecommerce.api.dto;

import java.time.LocalDateTime;

import com.siva.ecommerce.api.entity.ProductReview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

	private Long id;

	private Long productId;

	private Long customerId;

	private String customerName;

	private Integer rating;

	private String comment;

	private LocalDateTime createdAt;

	public static ReviewResponse from(ProductReview review) {
		return ReviewResponse.builder()
				.id(review.getId())
				.productId(review.getProduct().getId())
				.customerId(review.getCustomer().getId())
				.customerName(review.getCustomer().getFirstName() + " " + review.getCustomer().getLastName())
				.rating(review.getRating())
				.comment(review.getComment())
				.createdAt(review.getCreatedAt())
				.build();
	}

}
