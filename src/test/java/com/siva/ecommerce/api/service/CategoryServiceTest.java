package com.siva.ecommerce.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.siva.ecommerce.api.dto.CategoryRequest;
import com.siva.ecommerce.api.dto.CategoryResponse;
import com.siva.ecommerce.api.entity.Category;
import com.siva.ecommerce.api.exception.BusinessRuleException;
import com.siva.ecommerce.api.exception.DuplicateResourceException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CategoryRepository;
import com.siva.ecommerce.api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	void should_returnCreatedCategory_when_nameAndSlugAreUnique() {
		// Arrange
		CategoryRequest request = categoryRequest("Toys", "toys");
		when(categoryRepository.existsByName("Toys")).thenReturn(false);
		when(categoryRepository.existsBySlug("toys")).thenReturn(false);
		when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
			Category category = invocation.getArgument(0);
			category.setId(5L);
			return category;
		});

		// Act
		CategoryResponse response = categoryService.create(request);

		// Assert
		assertThat(response)
				.extracting(CategoryResponse::getId, CategoryResponse::getName, CategoryResponse::getSlug)
				.containsExactly(5L, "Toys", "toys");
	}

	@Test
	void should_throwDuplicateResourceException_when_categoryNameAlreadyExists() {
		// Arrange
		CategoryRequest request = categoryRequest("Books", "books-2");
		when(categoryRepository.existsByName("Books")).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> categoryService.create(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining("Books");
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void should_throwDuplicateResourceException_when_categorySlugAlreadyExists() {
		// Arrange
		CategoryRequest request = categoryRequest("Novels", "books");
		when(categoryRepository.existsByName("Novels")).thenReturn(false);
		when(categoryRepository.existsBySlug("books")).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> categoryService.create(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining("books");
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void should_returnCategory_when_idExists() {
		// Arrange
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books", "books")));

		// Act
		CategoryResponse response = categoryService.getById(1L);

		// Assert
		assertThat(response.getName()).isEqualTo("Books");
	}

	@Test
	void should_throwResourceNotFoundException_when_categoryDoesNotExist() {
		// Arrange
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> categoryService.getById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Category not found with id 99");
	}

	@Test
	void should_returnPageOfCategories_when_categoriesExist() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 20);
		when(categoryRepository.findAll(pageable))
				.thenReturn(new PageImpl<>(List.of(category(1L, "Books", "books"), category(2L, "Toys", "toys"))));

		// Act
		Page<CategoryResponse> page = categoryService.getAll(pageable);

		// Assert
		assertThat(page.getContent()).extracting(CategoryResponse::getSlug).containsExactly("books", "toys");
	}

	@Test
	void should_updateCategory_when_nameAndSlugAreNotUsedByAnotherCategory() {
		// Arrange
		Category existing = category(1L, "Books", "books");
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(categoryRepository.existsByNameAndIdNot("Reading", 1L)).thenReturn(false);
		when(categoryRepository.existsBySlugAndIdNot("reading", 1L)).thenReturn(false);
		when(categoryRepository.saveAndFlush(existing)).thenAnswer(returnsFirstArg());

		// Act
		CategoryResponse response = categoryService.update(1L, categoryRequest("Reading", "reading"));

		// Assert
		assertThat(response)
				.extracting(CategoryResponse::getName, CategoryResponse::getSlug)
				.containsExactly("Reading", "reading");
	}

	@Test
	void should_throwDuplicateResourceException_when_updatedNameBelongsToAnotherCategory() {
		// Arrange
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books", "books")));
		when(categoryRepository.existsByNameAndIdNot("Toys", 1L)).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> categoryService.update(1L, categoryRequest("Toys", "toys")))
				.isInstanceOf(DuplicateResourceException.class);
		verify(categoryRepository, never()).saveAndFlush(any());
	}

	@Test
	void should_deleteCategory_when_categoryHasNoProducts() {
		// Arrange
		Category existing = category(1L, "Books", "books");
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(productRepository.existsByCategoryId(1L)).thenReturn(false);

		// Act
		categoryService.delete(1L);

		// Assert
		verify(categoryRepository).delete(existing);
	}

	@Test
	void should_throwBusinessRuleException_when_deletingCategoryThatHasProducts() {
		// Arrange
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books", "books")));
		when(productRepository.existsByCategoryId(1L)).thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> categoryService.delete(1L))
				.isInstanceOf(BusinessRuleException.class);
		verify(categoryRepository, never()).delete(any());
	}

	private static CategoryRequest categoryRequest(String name, String slug) {
		return CategoryRequest.builder().name(name).slug(slug).build();
	}

	private static Category category(Long id, String name, String slug) {
		return Category.builder().id(id).name(name).slug(slug).build();
	}

}
