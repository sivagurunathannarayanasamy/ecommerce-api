package com.siva.ecommerce.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siva.ecommerce.api.dto.CustomerRequest;
import com.siva.ecommerce.api.dto.CustomerResponse;
import com.siva.ecommerce.api.entity.Customer;
import com.siva.ecommerce.api.exception.BusinessRuleException;
import com.siva.ecommerce.api.exception.DuplicateResourceException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CustomerRepository;
import com.siva.ecommerce.api.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final OrderRepository orderRepository;

	@Transactional
	public CustomerResponse create(CustomerRequest request) {
		if (customerRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("Customer with email '" + request.getEmail() + "' already exists");
		}
		Customer customer = Customer.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.email(request.getEmail())
				.phone(request.getPhone())
				.address(request.getAddress())
				.build();
		return CustomerResponse.from(customerRepository.save(customer));
	}

	public CustomerResponse getById(Long id) {
		return CustomerResponse.from(findCustomer(id));
	}

	public Page<CustomerResponse> getAll(Pageable pageable) {
		return customerRepository.findAll(pageable).map(CustomerResponse::from);
	}

	@Transactional
	public CustomerResponse update(Long id, CustomerRequest request) {
		Customer customer = findCustomer(id);
		if (customerRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
			throw new DuplicateResourceException("Customer with email '" + request.getEmail() + "' already exists");
		}
		customer.setFirstName(request.getFirstName());
		customer.setLastName(request.getLastName());
		customer.setEmail(request.getEmail());
		customer.setPhone(request.getPhone());
		customer.setAddress(request.getAddress());
		return CustomerResponse.from(customerRepository.saveAndFlush(customer));
	}

	@Transactional
	public void delete(Long id) {
		Customer customer = findCustomer(id);
		if (orderRepository.existsByCustomerId(id)) {
			throw new BusinessRuleException("Customer " + id + " has orders and cannot be deleted");
		}
		customerRepository.delete(customer);
	}

	private Customer findCustomer(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer", id));
	}

}
