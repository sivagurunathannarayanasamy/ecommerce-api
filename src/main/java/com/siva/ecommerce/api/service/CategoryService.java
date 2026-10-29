package com.siva.ecommerce.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siva.ecommerce.api.dto.CategoryRequest;
import com.siva.ecommerce.api.dto.CategoryResponse;
import com.siva.ecommerce.api.entity.Category;
import com.siva.ecommerce.api.exception.BusinessRuleException;
import com.siva.ecommerce.api.exception.DuplicateResourceException;
import com.siva.ecommerce.api.exception.ResourceNotFoundException;
import com.siva.ecommerce.api.repository.CategoryRepository;
import com.siva.ecommerce.api.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (categoryRepository.existsByName(request.getName())) {
			throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
		}
		if (categoryRepository.existsBySlug(request.getSlug())) {
			throw new DuplicateResourceException("Category with slug '" + request.getSlug() + "' already exists");
		}
		Category category = Category.builder()
				.name(request.getName())
				.slug(request.getSlug())
				.description(request.getDescription())
				.build();
		return CategoryResponse.from(categoryRepository.save(category));
	}

	public CategoryResponse getById(Long id) {
		return CategoryResponse.from(findCategory(id));
	}

	public Page<CategoryResponse> getAll(Pageable pageable) {
		return categoryRepository.findAll(pageable).map(CategoryResponse::from);
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = findCategory(id);
		if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
			throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
		}
		if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
			throw new DuplicateResourceException("Category with slug '" + request.getSlug() + "' already exists");
		}
		category.setName(request.getName());
		category.setSlug(request.getSlug());
		category.setDescription(request.getDescription());
		return CategoryResponse.from(categoryRepository.saveAndFlush(category));
	}

	@Transactional
	public void delete(Long id) {
		Category category = findCategory(id);
		if (productRepository.existsByCategoryId(id)) {
			throw new BusinessRuleException("Category " + id + " still has products and cannot be deleted");
		}
		categoryRepository.delete(category);
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

}
