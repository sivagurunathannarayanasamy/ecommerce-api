package com.siva.ecommerce.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siva.ecommerce.api.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrderId(Long orderId);

}
