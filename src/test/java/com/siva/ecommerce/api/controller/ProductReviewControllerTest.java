package com.siva.ecommerce.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.siva.ecommerce.api.dto.ReviewRequest;
import com.siva.ecommerce.api.dto.ReviewResponse;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.service.ProductReviewService;

@WebMvcTest(ProductReviewController.class)
class ProductReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductReviewService productReviewService;

	@Test
	void should_return201WithLocation_when_reviewIsValid() throws Exception {
		// Arrange
		when(productReviewService.create(eq(1L), any(ReviewRequest.class))).thenReturn(reviewResponse());

		// Act & Assert
		mockMvc.perform(post("/api/v1/products/1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customerId": 2, "rating": 5, "comment": "Great mouse"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/v1/products/1/reviews/7"))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.rating").value(5))
				.andExpect(jsonPath("$.customerName").value("Jane Smith"));
	}

	@Test
	void should_return400WithAllFieldErrors_when_ratingOutOfRangeAndCustomerMissing() throws Exception {
		// Act & Assert
		mockMvc.perform(post("/api/v1/products/1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"rating": 6}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.errors.length()").value(2))
				.andExpect(jsonPath("$.errors[?(@.field == 'rating')].message").value("must be less than or equal to 5"))
				.andExpect(jsonPath("$.errors[?(@.field == 'customerId')].message").value("must not be null"));
		verifyNoInteractions(productReviewService);
	}

	@Test
	void should_return400_when_ratingBelowMinimum() throws Exception {
		// Act & Assert
		mockMvc.perform(post("/api/v1/products/1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customerId": 2, "rating": 0}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("rating"))
				.andExpect(jsonPath("$.errors[0].message").value("must be greater than or equal to 1"));
		verifyNoInteractions(productReviewService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "5.5", "5.9", "4.5", "0.5" })
	void should_return400_when_ratingIsNotAWholeNumber(String rating) throws Exception {
		// Act & Assert
		mockMvc.perform(post("/api/v1/products/1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customerId": 2, "rating": %s}
						""".formatted(rating)))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(productReviewService);
	}

	@Test
	void should_return404ProblemDetail_when_productNotFound() throws Exception {
		// Arrange
		when(productReviewService.create(eq(99L), any(ReviewRequest.class)))
				.thenThrow(new ResourceNotFoundException("Product", 99L));

		// Act & Assert
		mockMvc.perform(post("/api/v1/products/99/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"customerId": 2, "rating": 4}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Resource not found"))
				.andExpect(jsonPath("$.detail").value("Product not found with id 99"));
	}

	@Test
	void should_return200WithPage_when_listingReviews() throws Exception {
		// Arrange
		when(productReviewService.getByProduct(eq(1L), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(reviewResponse())));

		// Act & Assert
		mockMvc.perform(get("/api/v1/products/1/reviews"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(7))
				.andExpect(jsonPath("$.content[0].comment").value("Great mouse"))
				.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	private static ReviewResponse reviewResponse() {
		return ReviewResponse.builder()
				.id(7L)
				.productId(1L)
				.customerId(2L)
				.customerName("Jane Smith")
				.rating(5)
				.comment("Great mouse")
				.build();
	}

}
