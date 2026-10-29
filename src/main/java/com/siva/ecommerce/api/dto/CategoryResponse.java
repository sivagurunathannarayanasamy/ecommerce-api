package com.siva.ecommerce.api.dto;

import java.time.LocalDateTime;

import com.siva.ecommerce.api.entity.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

	private Long id;

	private String name;

	private String slug;

	private String description;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	public static CategoryResponse from(Category category) {
		return CategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.slug(category.getSlug())
				.description(category.getDescription())
				.createdAt(category.getCreatedAt())
				.updatedAt(category.getUpdatedAt())
				.build();
	}

}
