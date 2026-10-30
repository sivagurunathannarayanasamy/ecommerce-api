package com.siva.ecommerce.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

	@Mock
	private ProductReviewRepository productReviewRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CustomerRepository customerRepository;

	@InjectMocks
	private ProductReviewService productReviewService;

	@Test
	void should_returnCreatedReview_when_productActiveAndCustomerExists() {
		// Arrange
		when(productRepository.findById(1L)).thenReturn(Optional.of(product(true)));
		when(customerRepository.findById(2L)).thenReturn(Optional.of(customer()));
		when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> {
			ProductReview review = invocation.getArgument(0);
			review.setId(7L);
			return review;
		});

		// Act
		ReviewResponse response = productReviewService.create(1L, reviewRequest(5, "Great mouse"));

		// Assert
		assertThat(response)
				.extracting(ReviewResponse::getId, ReviewResponse::getProductId, ReviewResponse::getCustomerId,
						ReviewResponse::getCustomerName, ReviewResponse::getRating, ReviewResponse::getComment)
				.containsExactly(7L, 1L, 2L, "Jane Smith", 5, "Great mouse");
	}

	@Test
	void should_throwResourceNotFoundException_when_reviewedProductNotFound() {
		// Arrange
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> productReviewService.create(99L, reviewRequest(4, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product not found with id 99");
		verifyNoInteractions(customerRepository, productReviewRepository);
	}

	@Test
	void should_throwBusinessRuleException_when_reviewedProductInactive() {
		// Arrange
		when(productRepository.findById(1L)).thenReturn(Optional.of(product(false)));

		// Act & Assert
		assertThatThrownBy(() -> productReviewService.create(1L, reviewRequest(4, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not available for review");
		verify(productReviewRepository, never()).save(any());
	}

	@Test
	void should_throwResourceNotFoundException_when_reviewingCustomerNotFound() {
		// Arrange
		when(productRepository.findById(1L)).thenReturn(Optional.of(product(true)));
		when(customerRepository.findById(2L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> productReviewService.create(1L, reviewRequest(4, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Customer not found with id 2");
		verify(productReviewRepository, never()).save(any());
	}

	@Test
	void should_returnPageOfReviews_when_productExists() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 20);
		ProductReview review = ProductReview.builder()
				.id(7L).product(product(true)).customer(customer()).rating(4).comment("Solid").build();
		when(productRepository.existsById(1L)).thenReturn(true);
		when(productReviewRepository.findByProductId(1L, pageable)).thenReturn(new PageImpl<>(List.of(review)));

		// Act
		Page<ReviewResponse> page = productReviewService.getByProduct(1L, pageable);

		// Assert
		assertThat(page.getContent())
				.extracting(ReviewResponse::getId, ReviewResponse::getRating)
				.containsExactly(tuple(7L, 4));
	}

	@Test
	void should_throwResourceNotFoundException_when_listingReviewsForUnknownProduct() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 20);
		when(productRepository.existsById(99L)).thenReturn(false);

		// Act & Assert
		assertThatThrownBy(() -> productReviewService.getByProduct(99L, pageable))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product not found with id 99");
		verifyNoInteractions(productReviewRepository);
	}

	private static ReviewRequest reviewRequest(int rating, String comment) {
		return ReviewRequest.builder().customerId(2L).rating(rating).comment(comment).build();
	}

	private static Product product(boolean active) {
		return Product.builder().id(1L).name("Wireless Mouse").sku("ELEC-MOU-001").active(active).build();
	}

	private static Customer customer() {
		return Customer.builder().id(2L).firstName("Jane").lastName("Smith").email("jane.smith@example.com").build();
	}

}
