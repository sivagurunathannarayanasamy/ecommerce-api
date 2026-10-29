package com.siva.ecommerce.api.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.siva.ecommerce.api.dto.OrderItemRequest;
import com.siva.ecommerce.api.dto.OrderRequest;
import com.siva.ecommerce.api.dto.OrderResponse;
import com.siva.ecommerce.api.entity.Customer;
import com.siva.ecommerce.api.entity.Order;
import com.siva.ecommerce.api.entity.OrderItem;
import com.siva.ecommerce.api.entity.OrderStatus;
import com.siva.ecommerce.api.entity.Payment;
import com.siva.ecommerce.api.entity.PaymentStatus;
import com.siva.ecommerce.api.entity.Product;
import com.siva.ecommerce.api.exception.BusinessRuleException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CustomerRepository;
import com.siva.ecommerce.api.repository.OrderRepository;
import com.siva.ecommerce.api.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;
	private final CustomerRepository customerRepository;
	private final ProductRepository productRepository;

	/**
	 * Places an order and reduces stock immediately.
	 * Any failure rolls back the whole transaction, including stock already reduced for earlier items.
	 */
	@Transactional
	public OrderResponse placeOrder(OrderRequest request) {
		Customer customer = customerRepository.findById(request.getCustomerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));

		// Merge duplicate product lines; sorted by id so concurrent orders lock product rows in the same order
		SortedMap<Long, Integer> quantities = request.getItems().stream()
				.collect(Collectors.toMap(OrderItemRequest::getProductId, OrderItemRequest::getQuantity,
						Integer::sum, TreeMap::new));
		Map<Long, Product> products = productRepository.findAllById(quantities.keySet()).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		Order order = Order.builder()
				.customer(customer)
				.shippingAddress(resolveShippingAddress(request, customer))
				.notes(request.getNotes())
				.build();

		BigDecimal total = BigDecimal.ZERO;
		for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
			Product product = products.get(line.getKey());
			if (product == null) {
				throw new ResourceNotFoundException("Product", line.getKey());
			}
			if (!product.getActive()) {
				throw new BusinessRuleException("Product " + product.getSku() + " is not available for sale");
			}
			if (productRepository.decrementStock(product.getId(), line.getValue()) == 0) {
				throw new BusinessRuleException("Insufficient stock for product " + product.getSku()
						+ ": requested " + line.getValue());
			}

			OrderItem item = OrderItem.builder()
					.product(product)
					.quantity(line.getValue())
					.unitPrice(product.getPrice())
					.build();
			item.calculateSubtotal();
			order.addItem(item);
			total = total.add(item.getSubtotal());
		}

		order.setTotalAmount(total);
		order.setPayment(Payment.builder()
				.paymentMethod(request.getPaymentMethod())
				.amount(total)
				.build());
		return OrderResponse.from(orderRepository.save(order));
	}

	public OrderResponse getById(Long id) {
		return OrderResponse.from(findOrder(id));
	}

	public Page<OrderResponse> getAll(Long customerId, OrderStatus status, Pageable pageable) {
		Page<Order> orders;
		if (customerId != null && status != null) {
			orders = orderRepository.findByCustomerIdAndStatus(customerId, status, pageable);
		} else if (customerId != null) {
			orders = orderRepository.findByCustomerId(customerId, pageable);
		} else if (status != null) {
			orders = orderRepository.findByStatus(status, pageable);
		} else {
			orders = orderRepository.findAll(pageable);
		}
		return orders.map(OrderResponse::from);
	}

	@Transactional
	public OrderResponse updateStatus(Long id, OrderStatus target) {
		Order order = findOrder(id);
		OrderStatus current = order.getStatus();
		if (!current.canTransitionTo(target)) {
			throw new BusinessRuleException("Cannot change order status from " + current + " to " + target);
		}
		if (target == OrderStatus.CANCELLED) {
			restoreStock(order);
			refundIfPaid(order.getPayment());
		}
		order.setStatus(target);
		return OrderResponse.from(orderRepository.saveAndFlush(order));
	}

	@Transactional
	public OrderResponse updatePaymentStatus(Long id, PaymentStatus target) {
		Order order = findOrder(id);
		if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
			throw new BusinessRuleException("Cannot update payment for an order in status " + order.getStatus());
		}
		Payment payment = order.getPayment();
		boolean allowed = payment.getPaymentStatus() == PaymentStatus.PENDING
				&& (target == PaymentStatus.COMPLETED || target == PaymentStatus.FAILED);
		if (!allowed) {
			throw new BusinessRuleException(
					"Cannot change payment status from " + payment.getPaymentStatus() + " to " + target);
		}
		payment.setPaymentStatus(target);
		return OrderResponse.from(orderRepository.saveAndFlush(order));
	}

	private void restoreStock(Order order) {
		SortedMap<Long, Integer> quantities = order.getItems().stream()
				.collect(Collectors.toMap(item -> item.getProduct().getId(), OrderItem::getQuantity,
						Integer::sum, TreeMap::new));
		quantities.forEach(productRepository::incrementStock);
	}

	private void refundIfPaid(Payment payment) {
		if (payment != null && payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
			payment.setPaymentStatus(PaymentStatus.REFUNDED);
		}
	}

	private String resolveShippingAddress(OrderRequest request, Customer customer) {
		if (StringUtils.hasText(request.getShippingAddress())) {
			return request.getShippingAddress();
		}
		if (StringUtils.hasText(customer.getAddress())) {
			return customer.getAddress();
		}
		throw new BusinessRuleException("Shipping address is required: customer " + customer.getId() + " has no address on file");
	}

	private Order findOrder(Long id) {
		return orderRepository.findWithDetailsById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order", id));
	}

}
