package com.siva.ecommerce.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.siva.ecommerce.api.dto.ProductRequest;
import com.siva.ecommerce.api.dto.ProductResponse;
import com.siva.ecommerce.api.entity.Category;
import com.siva.ecommerce.api.entity.Product;
import com.siva.ecommerce.api.exception.DuplicateResourceException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CategoryRepository;
import com.siva.ecommerce.api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	private static final Category ELECTRONICS = Category.builder().id(1L).name("Electronics").slug("electronics").build();

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private ProductService productService;

	@Test
	void should_returnActiveProduct_when_createdWithoutActiveFlag() {
		// Arrange
		ProductRequest request = productRequest("ELEC-NEW-001", null);
		when(productRepository.existsBySku("ELEC-NEW-001")).thenReturn(false);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(ELECTRONICS));
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product product = invocation.getArgument(0);
			product.setId(11L);
			return product;
		});

		// Act
		ProductResponse response = productService.create(request);

		// Assert
		assertThat(response)
				.extracting(ProductResponse::getId, ProductResponse::getSku, ProductResponse::getActive,
						ProductResponse::getCategoryName, ProductResponse::getPrice)
				.containsExactly(11L, "ELEC-NEW-001", true, "Electronics", new BigDecimal("49.99"));
	}

	@Test
	void should_throwDuplicateResourceException_when_skuAlreadyExists() {
		// Arrange
		ProductRequest request = productRequest("ELEC-MOU-001", true);
		when(productRepository.existsBySku("ELEC-MOU-001")).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> productService.create(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining("ELEC-MOU-001");
		verify(productRepository, never()).save(any());
	}

	@Test
	void should_throwResourceNotFoundException_when_creatingProductForUnknownCategory() {
		// Arrange
		ProductRequest request = productRequest("ELEC-NEW-001", true);
		request.setCategoryId(99L);
		when(productRepository.existsBySku("ELEC-NEW-001")).thenReturn(false);
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> productService.create(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Category not found with id 99");
		verify(productRepository, never()).save(any());
	}

	@Test
	void should_throwResourceNotFoundException_when_productDoesNotExist() {
		// Arrange
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> productService.getById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product not found with id 99");
	}

	@Test
	void should_listAllActiveProducts_when_noCategoryFilterGiven() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 20);
		when(productRepository.findByActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(product(1L, true))));

		// Act
		Page<ProductResponse> page = productService.getAll(null, pageable);

		// Assert
		assertThat(page.getContent()).extracting(ProductResponse::getId).containsExactly(1L);
	}

	@Test
	void should_listActiveProductsInCategory_when_categoryFilterGiven() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 20);
		when(productRepository.findByCategoryIdAndActiveTrue(1L, pageable))
				.thenReturn(new PageImpl<>(List.of(product(2L, true))));

		// Act
		Page<ProductResponse> page = productService.getAll(1L, pageable);

		// Assert
		assertThat(page.getContent()).extracting(ProductResponse::getId).containsExactly(2L);
	}

	@Test
	void should_keepActiveFlag_when_updateRequestOmitsIt() {
		// Arrange
		Product existing = product(1L, false);
		when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(productRepository.existsBySkuAndIdNot("ELEC-NEW-001", 1L)).thenReturn(false);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(ELECTRONICS));
		when(productRepository.saveAndFlush(existing)).thenAnswer(returnsFirstArg());

		// Act
		ProductResponse response = productService.update(1L, productRequest("ELEC-NEW-001", null));

		// Assert
		assertThat(response)
				.extracting(ProductResponse::getSku, ProductResponse::getActive)
				.containsExactly("ELEC-NEW-001", false);
	}

	@Test
	void should_throwDuplicateResourceException_when_updatedSkuBelongsToAnotherProduct() {
		// Arrange
		when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, true)));
		when(productRepository.existsBySkuAndIdNot("ELEC-KEY-002", 1L)).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> productService.update(1L, productRequest("ELEC-KEY-002", true)))
				.isInstanceOf(DuplicateResourceException.class);
		verify(productRepository, never()).saveAndFlush(any());
	}

	@Test
	void should_deactivateProduct_when_deleted() {
		// Arrange
		Product existing = product(1L, true);
		when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

		// Act
		productService.delete(1L);

		// Assert
		assertThat(existing.getActive()).isFalse();
		verify(productRepository, never()).delete(any());
	}

	private static ProductRequest productRequest(String sku, Boolean active) {
		return ProductRequest.builder()
				.name("USB Microphone")
				.price(new BigDecimal("49.99"))
				.sku(sku)
				.stockQuantity(25)
				.active(active)
				.categoryId(1L)
				.build();
	}

	private static Product product(Long id, boolean active) {
		return Product.builder()
				.id(id)
				.name("Wireless Mouse")
				.price(new BigDecimal("24.99"))
				.sku("ELEC-MOU-001")
				.stockQuantity(150)
				.active(active)
				.category(ELECTRONICS)
				.build();
	}

}
