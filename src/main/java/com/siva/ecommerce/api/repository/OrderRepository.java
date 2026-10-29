package com.siva.ecommerce.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.siva.ecommerce.api.entity.Order;
import com.siva.ecommerce.api.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = { "customer", "items", "items.product", "payment" })
	Optional<Order> findWithDetailsById(Long id);

	@EntityGraph(attributePaths = { "customer", "items", "items.product", "payment" })
	Optional<Order> findByOrderNumber(String orderNumber);

	Page<Order> findByCustomerId(Long customerId, Pageable pageable);

	Page<Order> findByStatus(OrderStatus status, Pageable pageable);

	Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

	boolean existsByCustomerId(Long customerId);

}
