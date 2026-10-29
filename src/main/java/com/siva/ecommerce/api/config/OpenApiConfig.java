package com.siva.ecommerce.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI ecommerceOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("E-commerce API")
						.version("v1")
						.description("Categories, products, customers and orders. "
								+ "Errors use RFC 9457 Problem Details (application/problem+json)."));
	}

}
