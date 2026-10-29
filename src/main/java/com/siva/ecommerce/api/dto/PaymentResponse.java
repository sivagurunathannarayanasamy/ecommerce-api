package com.siva.ecommerce.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.siva.ecommerce.api.entity.Payment;
import com.siva.ecommerce.api.entity.PaymentMethod;
import com.siva.ecommerce.api.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

	private Long id;

	private PaymentMethod paymentMethod;

	private PaymentStatus paymentStatus;

	private BigDecimal amount;

	private LocalDateTime createdAt;

	public static PaymentResponse from(Payment payment) {
		return PaymentResponse.builder()
				.id(payment.getId())
				.paymentMethod(payment.getPaymentMethod())
				.paymentStatus(payment.getPaymentStatus())
				.amount(payment.getAmount())
				.createdAt(payment.getCreatedAt())
				.build();
	}

}
