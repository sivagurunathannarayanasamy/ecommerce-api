package com.siva.ecommerce.api.dto;

import java.math.BigDecimal;

import com.siva.ecommerce.api.entity.OrderItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

	private Long id;

	private Long productId;

	private String productName;

	private String sku;

	private Integer quantity;

	private BigDecimal unitPrice;

	private BigDecimal subtotal;

	public static OrderItemResponse from(OrderItem item) {
		return OrderItemResponse.builder()
				.id(item.getId())
				.productId(item.getProduct().getId())
				.productName(item.getProduct().getName())
				.sku(item.getProduct().getSku())
				.quantity(item.getQuantity())
				.unitPrice(item.getUnitPrice())
				.subtotal(item.getSubtotal())
				.build();
	}

}
