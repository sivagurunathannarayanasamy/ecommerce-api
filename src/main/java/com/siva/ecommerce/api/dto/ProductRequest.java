package com.siva.ecommerce.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

	@NotBlank
	@Size(max = 200)
	private String name;

	private String description;

	@NotNull
	@DecimalMin("0.01")
	@Digits(integer = 8, fraction = 2)
	private BigDecimal price;

	@NotBlank
	@Size(max = 100)
	private String sku;

	@NotNull
	@PositiveOrZero
	private Integer stockQuantity;

	// Optional: defaults to true on create, unchanged on update
	private Boolean active;

	@NotNull
	private Long categoryId;

}
