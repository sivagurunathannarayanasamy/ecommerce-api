package com.siva.ecommerce.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.siva.ecommerce.api.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findBySku(String sku);

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, Long id);

	boolean existsByCategoryId(Long categoryId);

	Page<Product> findByActiveTrue(Pageable pageable);

	Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

	/**
	 * Atomically reduces stock only if enough is available.
	 * Returns 0 when stock is insufficient, so concurrent orders cannot oversell.
	 */
	@Modifying(flushAutomatically = true)
	@Query("update Product p set p.stockQuantity = p.stockQuantity - :quantity "
			+ "where p.id = :id and p.stockQuantity >= :quantity")
	int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

	@Modifying(flushAutomatically = true)
	@Query("update Product p set p.stockQuantity = p.stockQuantity + :quantity where p.id = :id")
	int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);

}
