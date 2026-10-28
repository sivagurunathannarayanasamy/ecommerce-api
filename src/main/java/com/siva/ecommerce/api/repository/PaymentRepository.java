package com.siva.ecommerce.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siva.ecommerce.api.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByOrderId(Long orderId);

}
