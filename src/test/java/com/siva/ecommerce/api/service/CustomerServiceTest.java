package com.siva.ecommerce.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.siva.ecommerce.api.dto.CustomerRequest;
import com.siva.ecommerce.api.dto.CustomerResponse;
import com.siva.ecommerce.api.entity.Customer;
import com.siva.ecommerce.api.exception.BusinessRuleException;
import com.siva.ecommerce.api.exception.DuplicateResourceException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CustomerRepository;
import com.siva.ecommerce.api.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private OrderRepository orderRepository;

	@InjectMocks
	private CustomerService customerService;

	@Test
	void should_returnRegisteredCustomer_when_emailIsUnique() {
		// Arrange
		CustomerRequest request = customerRequest("alice@example.com");
		when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer customer = invocation.getArgument(0);
			customer.setId(4L);
			return customer;
		});

		// Act
		CustomerResponse response = customerService.create(request);

		// Assert
		assertThat(response)
				.extracting(CustomerResponse::getId, CustomerResponse::getEmail, CustomerResponse::getFirstName)
				.containsExactly(4L, "alice@example.com", "Alice");
	}

	@Test
	void should_throwDuplicateResourceException_when_emailAlreadyRegistered() {
		// Arrange
		CustomerRequest request = customerRequest("john.doe@example.com");
		when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> customerService.create(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining("john.doe@example.com");
		verify(customerRepository, never()).save(any());
	}

	@Test
	void should_throwResourceNotFoundException_when_customerDoesNotExist() {
		// Arrange
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> customerService.getById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Customer not found with id 99");
	}

	@Test
	void should_updateCustomer_when_emailNotUsedByAnotherCustomer() {
		// Arrange
		Customer existing = customer(1L, "john.doe@example.com");
		when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(customerRepository.existsByEmailAndIdNot("alice@example.com", 1L)).thenReturn(false);
		when(customerRepository.saveAndFlush(existing)).thenAnswer(returnsFirstArg());

		// Act
		CustomerResponse response = customerService.update(1L, customerRequest("alice@example.com"));

		// Assert
		assertThat(response)
				.extracting(CustomerResponse::getId, CustomerResponse::getEmail)
				.containsExactly(1L, "alice@example.com");
	}

	@Test
	void should_throwDuplicateResourceException_when_updatedEmailBelongsToAnotherCustomer() {
		// Arrange
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "john.doe@example.com")));
		when(customerRepository.existsByEmailAndIdNot("jane.smith@example.com", 1L)).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> customerService.update(1L, customerRequest("jane.smith@example.com")))
				.isInstanceOf(DuplicateResourceException.class);
		verify(customerRepository, never()).saveAndFlush(any());
	}

	@Test
	void should_deleteCustomer_when_customerHasNoOrders() {
		// Arrange
		Customer existing = customer(1L, "john.doe@example.com");
		when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(orderRepository.existsByCustomerId(1L)).thenReturn(false);

		// Act
		customerService.delete(1L);

		// Assert
		verify(customerRepository).delete(existing);
	}

	@Test
	void should_throwBusinessRuleException_when_deletingCustomerWithOrders() {
		// Arrange
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "john.doe@example.com")));
		when(orderRepository.existsByCustomerId(1L)).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> customerService.delete(1L))
				.isInstanceOf(BusinessRuleException.class);
		verify(customerRepository, never()).delete(any());
	}

	private static CustomerRequest customerRequest(String email) {
		return CustomerRequest.builder()
				.firstName("Alice")
				.lastName("Brown")
				.email(email)
				.phone("+1-202-555-0199")
				.address("7 Pine Road, Springfield")
				.build();
	}

	private static Customer customer(Long id, String email) {
		return Customer.builder().id(id).firstName("John").lastName("Doe").email(email).build();
	}

}
