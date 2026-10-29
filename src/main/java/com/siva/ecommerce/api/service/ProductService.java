package com.siva.ecommerce.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siva.ecommerce.api.dto.ProductRequest;
import com.siva.ecommerce.api.dto.ProductResponse;
import com.siva.ecommerce.api.entity.Category;
import com.siva.ecommerce.api.entity.Product;
import com.siva.ecommerce.api.exception.DuplicateResourceException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CategoryRepository;
import com.siva.ecommerce.api.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	@Transactional
	public ProductResponse create(ProductRequest request) {
		if (productRepository.existsBySku(request.getSku())) {
			throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
		}
		Product product = Product.builder()
				.name(request.getName())
				.description(request.getDescription())
				.price(request.getPrice())
				.sku(request.getSku())
				.stockQuantity(request.getStockQuantity())
				.active(request.getActive() == null || request.getActive())
				.category(findCategory(request.getCategoryId()))
				.build();
		return ProductResponse.from(productRepository.save(product));
	}

	public ProductResponse getById(Long id) {
		return ProductResponse.from(findProduct(id));
	}

	/** Lists active products, optionally filtered by category. */
	public Page<ProductResponse> getAll(Long categoryId, Pageable pageable) {
		Page<Product> products = categoryId == null
				? productRepository.findByActiveTrue(pageable)
				: productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
		return products.map(ProductResponse::from);
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = findProduct(id);
		if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
			throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
		}
		product.setName(request.getName());
		product.setDescription(request.getDescription());
		product.setPrice(request.getPrice());
		product.setSku(request.getSku());
		product.setStockQuantity(request.getStockQuantity());
		if (request.getActive() != null) {
			product.setActive(request.getActive());
		}
		product.setCategory(findCategory(request.getCategoryId()));
		return ProductResponse.from(productRepository.saveAndFlush(product));
	}

	/** Soft delete: products are referenced by historical order items, so they are deactivated, not removed. */
	@Transactional
	public void delete(Long id) {
		findProduct(id).setActive(false);
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

}
