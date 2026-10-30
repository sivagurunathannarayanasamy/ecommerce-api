package com.siva.ecommerce.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.siva.ecommerce.api.dto.OrderItemRequest;
import com.siva.ecommerce.api.dto.OrderItemResponse;
import com.siva.ecommerce.api.dto.OrderRequest;
import com.siva.ecommerce.api.dto.OrderResponse;
import com.siva.ecommerce.api.dto.PaymentResponse;
import com.siva.ecommerce.api.entity.Customer;
import com.siva.ecommerce.api.entity.Order;
import com.siva.ecommerce.api.entity.OrderItem;
import com.siva.ecommerce.api.entity.OrderStatus;
import com.siva.ecommerce.api.entity.Payment;
import com.siva.ecommerce.api.entity.PaymentMethod;
import com.siva.ecommerce.api.entity.PaymentStatus;
import com.siva.ecommerce.api.entity.Product;
import com.siva.ecommerce.api.exception.BusinessRuleException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CustomerRepository;
import com.siva.ecommerce.api.repository.OrderRepository;
import com.siva.ecommerce.api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private OrderService orderService;

	// ---- placeOrder ----

	@Test
	void should_createPendingOrderWithCalculatedTotals_when_stockIsAvailable() {
		// Arrange
		Customer customer = customer("123 Maple Street, Springfield");
		Product mouse = product(1L, "ELEC-MOU-001", "24.99", true);
		Product book = product(5L, "BOOK-CLN-001", "32.50", true);
		OrderRequest request = orderRequest(null, line(1L, 2), line(5L, 1), line(1L, 1));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(productRepository.findAllById(any())).thenReturn(List.of(mouse, book));
		when(productRepository.decrementStock(1L, 3)).thenReturn(1);
		when(productRepository.decrementStock(5L, 1)).thenReturn(1);
		when(orderRepository.save(any(Order.class))).thenAnswer(returnsFirstArg());

		// Act
		OrderResponse response = orderService.placeOrder(request);

		// Assert
		assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(response.getTotalAmount()).isEqualByComparingTo("107.47");
		assertThat(response.getShippingAddress()).isEqualTo("123 Maple Street, Springfield");
		assertThat(response.getItems())
				.extracting(OrderItemResponse::getProductId, OrderItemResponse::getQuantity, OrderItemResponse::getSubtotal)
				.containsExactly(
						tuple(1L, 3, new BigDecimal("74.97")),
						tuple(5L, 1, new BigDecimal("32.50")));
		assertThat(response.getPayment())
				.extracting(PaymentResponse::getPaymentStatus, PaymentResponse::getAmount)
				.containsExactly(PaymentStatus.PENDING, new BigDecimal("107.47"));
		verify(productRepository).decrementStock(1L, 3);
		verify(productRepository).decrementStock(5L, 1);
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void should_logLowStockWarning_when_remainingStockBelowThreshold(CapturedOutput output) {
		// Arrange
		OrderRequest request = orderRequest(null, line(1L, 3));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer("123 Maple Street")));
		when(productRepository.findAllById(any())).thenReturn(List.of(product(1L, "ELEC-MOU-001", "24.99", true)));
		when(productRepository.decrementStock(1L, 3)).thenReturn(1);
		when(orderRepository.save(any(Order.class))).thenAnswer(returnsFirstArg());
		when(productRepository.findStockLevelsBelow(anyCollection(), eq(10)))
				.thenReturn(List.of(stockLevel("Wireless Mouse", 7)));

		// Act
		OrderResponse response = orderService.placeOrder(request);

		// Assert
		assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(output).contains("Low stock alert: Wireless Mouse has 7 units left");
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void should_notLogLowStockWarning_when_remainingStockAtOrAboveThreshold(CapturedOutput output) {
		// Arrange
		OrderRequest request = orderRequest(null, line(1L, 3));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer("123 Maple Street")));
		when(productRepository.findAllById(any())).thenReturn(List.of(product(1L, "ELEC-MOU-001", "24.99", true)));
		when(productRepository.decrementStock(1L, 3)).thenReturn(1);
		when(orderRepository.save(any(Order.class))).thenAnswer(returnsFirstArg());
		when(productRepository.findStockLevelsBelow(anyCollection(), eq(10))).thenReturn(List.of());

		// Act
		orderService.placeOrder(request);

		// Assert
		assertThat(output).doesNotContain("Low stock alert");
	}

	@Test
	void should_throwResourceNotFoundException_when_customerNotFound() {
		// Arrange
		OrderRequest request = orderRequest(null, line(1L, 1));
		when(customerRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> orderService.placeOrder(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Customer not found with id 1");
		verifyNoInteractions(productRepository, orderRepository);
	}

	@Test
	void should_throwBusinessRuleException_when_stockIsInsufficient() {
		// Arrange
		OrderRequest request = orderRequest(null, line(3L, 999));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer("12 MG Road")));
		when(productRepository.findAllById(any())).thenReturn(List.of(product(3L, "ELEC-HUB-003", "39.99", true)));
		when(productRepository.decrementStock(3L, 999)).thenReturn(0);

		// Act & Assert
		assertThatThrownBy(() -> orderService.placeOrder(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Insufficient stock")
				.hasMessageContaining("ELEC-HUB-003");
		verify(orderRepository, never()).save(any());
	}

	@Test
	void should_throwResourceNotFoundException_when_productNotFound() {
		// Arrange
		OrderRequest request = orderRequest(null, line(42L, 1));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer("12 MG Road")));
		when(productRepository.findAllById(any())).thenReturn(List.of());

		// Act & Assert
		assertThatThrownBy(() -> orderService.placeOrder(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product not found with id 42");
		verify(orderRepository, never()).save(any());
	}

	@Test
	void should_throwBusinessRuleException_when_productIsInactive() {
		// Arrange
		OrderRequest request = orderRequest(null, line(1L, 1));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer("12 MG Road")));
		when(productRepository.findAllById(any())).thenReturn(List.of(product(1L, "ELEC-MOU-001", "24.99", false)));

		// Act & Assert
		assertThatThrownBy(() -> orderService.placeOrder(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not available");
		verify(productRepository, never()).decrementStock(anyLong(), anyInt());
	}

	@Test
	void should_useRequestShippingAddress_when_provided() {
		// Arrange
		OrderRequest request = orderRequest("1 Office Park", line(1L, 1));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer("123 Maple Street")));
		when(productRepository.findAllById(any())).thenReturn(List.of(product(1L, "ELEC-MOU-001", "24.99", true)));
		when(productRepository.decrementStock(1L, 1)).thenReturn(1);
		when(orderRepository.save(any(Order.class))).thenAnswer(returnsFirstArg());

		// Act
		OrderResponse response = orderService.placeOrder(request);

		// Assert
		assertThat(response.getShippingAddress()).isEqualTo("1 Office Park");
	}

	@Test
	void should_throwBusinessRuleException_when_noShippingAddressAvailable() {
		// Arrange
		OrderRequest request = orderRequest(null, line(1L, 1));
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(null)));

		// Act & Assert
		assertThatThrownBy(() -> orderService.placeOrder(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Shipping address is required");
		verifyNoInteractions(productRepository, orderRepository);
	}

	// ---- updateStatus ----

	@Test
	void should_updateStatus_when_transitionIsAllowed() {
		// Arrange
		Order order = order(OrderStatus.PENDING, PaymentStatus.PENDING);
		when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));
		when(orderRepository.saveAndFlush(order)).thenAnswer(returnsFirstArg());

		// Act
		OrderResponse response = orderService.updateStatus(10L, OrderStatus.CONFIRMED);

		// Assert
		assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		verify(productRepository, never()).incrementStock(anyLong(), anyInt());
	}

	@Test
	void should_throwBusinessRuleException_when_transitionSkipsAStep() {
		// Arrange
		when(orderRepository.findWithDetailsById(10L))
				.thenReturn(Optional.of(order(OrderStatus.PENDING, PaymentStatus.PENDING)));

		// Act & Assert
		assertThatThrownBy(() -> orderService.updateStatus(10L, OrderStatus.SHIPPED))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Cannot change order status from PENDING to SHIPPED");
		verify(orderRepository, never()).saveAndFlush(any());
	}

	@Test
	void should_restoreStockAndRefundPayment_when_cancellingOrderWithCompletedPayment() {
		// Arrange
		Order order = order(OrderStatus.CONFIRMED, PaymentStatus.COMPLETED);
		when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));
		when(orderRepository.saveAndFlush(order)).thenAnswer(returnsFirstArg());

		// Act
		OrderResponse response = orderService.updateStatus(10L, OrderStatus.CANCELLED);

		// Assert
		assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(response.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
		verify(productRepository).incrementStock(1L, 3);
		verify(productRepository).incrementStock(5L, 1);
	}

	@Test
	void should_leavePaymentPending_when_cancellingOrderWithPendingPayment() {
		// Arrange
		Order order = order(OrderStatus.PENDING, PaymentStatus.PENDING);
		when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));
		when(orderRepository.saveAndFlush(order)).thenAnswer(returnsFirstArg());

		// Act
		OrderResponse response = orderService.updateStatus(10L, OrderStatus.CANCELLED);

		// Assert
		assertThat(response.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
		verify(productRepository).incrementStock(1L, 3);
	}

	@Test
	void should_throwBusinessRuleException_when_cancellingDeliveredOrder() {
		// Arrange
		Order order = order(OrderStatus.DELIVERED, PaymentStatus.COMPLETED);
		when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));

		// Act & Assert
		assertThatThrownBy(() -> orderService.updateStatus(10L, OrderStatus.CANCELLED))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Cannot change order status from DELIVERED to CANCELLED");
		verify(productRepository, never()).incrementStock(anyLong(), anyInt());
		assertThat(order.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
	}

	@Test
	void should_throwBusinessRuleException_when_cancellingAlreadyCancelledOrder() {
		// Arrange
		when(orderRepository.findWithDetailsById(10L))
				.thenReturn(Optional.of(order(OrderStatus.CANCELLED, PaymentStatus.REFUNDED)));

		// Act & Assert
		assertThatThrownBy(() -> orderService.updateStatus(10L, OrderStatus.CANCELLED))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Cannot change order status from CANCELLED to CANCELLED");
		verify(productRepository, never()).incrementStock(anyLong(), anyInt());
	}

	@Test
	void should_throwResourceNotFoundException_when_orderDoesNotExist() {
		// Arrange
		when(orderRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> orderService.updateStatus(99L, OrderStatus.CONFIRMED))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Order not found with id 99");
	}

	// ---- updatePaymentStatus ----

	@Test
	void should_completePayment_when_paymentIsPending() {
		// Arrange
		Order order = order(OrderStatus.PENDING, PaymentStatus.PENDING);
		when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));
		when(orderRepository.saveAndFlush(order)).thenAnswer(returnsFirstArg());

		// Act
		OrderResponse response = orderService.updatePaymentStatus(10L, PaymentStatus.COMPLETED);

		// Assert
		assertThat(response.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
	}

	@Test
	void should_throwBusinessRuleException_when_paymentAlreadyCompleted() {
		// Arrange
		when(orderRepository.findWithDetailsById(10L))
				.thenReturn(Optional.of(order(OrderStatus.CONFIRMED, PaymentStatus.COMPLETED)));

		// Act & Assert
		assertThatThrownBy(() -> orderService.updatePaymentStatus(10L, PaymentStatus.FAILED))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessage("Cannot change payment status from COMPLETED to FAILED");
	}

	@Test
	void should_throwBusinessRuleException_when_updatingPaymentOfCancelledOrder() {
		// Arrange
		when(orderRepository.findWithDetailsById(10L))
				.thenReturn(Optional.of(order(OrderStatus.CANCELLED, PaymentStatus.PENDING)));

		// Act & Assert
		assertThatThrownBy(() -> orderService.updatePaymentStatus(10L, PaymentStatus.COMPLETED))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CANCELLED");
	}

	// ---- fixtures ----

	private static OrderItemRequest line(Long productId, int quantity) {
		return OrderItemRequest.builder().productId(productId).quantity(quantity).build();
	}

	private static OrderRequest orderRequest(String shippingAddress, OrderItemRequest... items) {
		return OrderRequest.builder()
				.customerId(1L)
				.shippingAddress(shippingAddress)
				.paymentMethod(PaymentMethod.UPI)
				.items(List.of(items))
				.build();
	}

	private static Customer customer(String address) {
		return Customer.builder().id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
				.address(address).build();
	}

	private static Product product(Long id, String sku, String price, boolean active) {
		return Product.builder().id(id).name(sku).sku(sku).price(new BigDecimal(price))
				.stockQuantity(100).active(active).build();
	}

	/** Order 10 with 3 x product 1 and 1 x product 5, in the given order and payment states. */
	private static Order order(OrderStatus status, PaymentStatus paymentStatus) {
		Order order = Order.builder()
				.id(10L)
				.orderNumber("ORD-ABCD1234")
				.customer(customer("123 Maple Street"))
				.status(status)
				.totalAmount(new BigDecimal("107.47"))
				.shippingAddress("123 Maple Street")
				.build();
		order.addItem(orderItem(product(1L, "ELEC-MOU-001", "24.99", true), 3));
		order.addItem(orderItem(product(5L, "BOOK-CLN-001", "32.50", true), 1));
		order.setPayment(Payment.builder()
				.paymentMethod(PaymentMethod.UPI)
				.paymentStatus(paymentStatus)
				.amount(new BigDecimal("107.47"))
				.build());
		return order;
	}

	private static ProductRepository.StockLevel stockLevel(String name, int stockQuantity) {
		return new ProductRepository.StockLevel() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Integer getStockQuantity() {
				return stockQuantity;
			}

		};
	}

	private static OrderItem orderItem(Product product, int quantity) {
		OrderItem item = OrderItem.builder().product(product).quantity(quantity).unitPrice(product.getPrice()).build();
		item.calculateSubtotal();
		return item;
	}

}
