package com.siva.ecommerce.api.entity;

public enum OrderStatus {
	PENDING,
	CONFIRMED,
	PROCESSING,
	SHIPPED,
	DELIVERED,
	CANCELLED,
	REFUNDED;

	public boolean canTransitionTo(OrderStatus target) {
		return switch (this) {
			case PENDING -> target == CONFIRMED || target == CANCELLED;
			case CONFIRMED -> target == PROCESSING || target == CANCELLED;
			case PROCESSING -> target == SHIPPED || target == CANCELLED;
			case SHIPPED -> target == DELIVERED;
			case DELIVERED, CANCELLED, REFUNDED -> false;
		};
	}
}
