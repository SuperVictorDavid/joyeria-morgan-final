package com.joyeria.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "clientes-service", url = "${servicios.clientes-url}")
public interface ClientesClient {

	@GetMapping("/customers")
	List<Map<String, Object>> getCustomers();
}
