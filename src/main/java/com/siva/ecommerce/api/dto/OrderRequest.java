package com.siva.ecommerce.api.dto;

import java.util.List;

import com.siva.ecommerce.api.entity.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

	@NotNull
	private Long customerId;

	// Optional: falls back to the customer's address
	private String shippingAddress;

	private String notes;

	@NotNull
	private PaymentMethod paymentMethod;

	@NotEmpty
	private List<@Valid @NotNull OrderItemRequest> items;

}
