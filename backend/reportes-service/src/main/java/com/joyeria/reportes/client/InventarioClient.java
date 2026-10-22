package com.joyeria.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "inventario-service", url = "http://localhost:8081")
public interface InventarioClient {

	@GetMapping("/products")
	List<Map<String, Object>> getProducts();
}
