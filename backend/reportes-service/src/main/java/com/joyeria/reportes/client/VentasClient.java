package com.joyeria.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ventas-service", url = "${servicios.ventas-url}")
public interface VentasClient {

	@GetMapping("/sales")
	List<Map<String, Object>> getSales();
}
