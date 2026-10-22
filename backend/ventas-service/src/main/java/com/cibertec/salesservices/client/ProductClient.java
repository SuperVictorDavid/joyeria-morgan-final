package com.cibertec.salesservices.client;

import com.cibertec.salesservices.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
		name = "inventario-service"
)
public interface ProductClient {

	@GetMapping("/products/{id}")
	ProductResponse getProductById(@PathVariable Long id);
}
