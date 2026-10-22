package com.joyeria.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ventas-service", url = "http://localhost:8082")
public interface VentasClient {

	@GetMapping("/sales")
	List<Map<String, Object>> getSales();
}
