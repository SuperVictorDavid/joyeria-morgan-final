package com.joyeria.reportes.rest;

import com.joyeria.reportes.client.ClientesClient;
import com.joyeria.reportes.client.InventarioClient;
import com.joyeria.reportes.client.VentasClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

	private final InventarioClient inventarioClient;
	private final VentasClient ventasClient;
	private final ClientesClient clientesClient;

	public ReporteController(InventarioClient inventarioClient,
							 VentasClient ventasClient,
							 ClientesClient clientesClient) {
		this.inventarioClient = inventarioClient;
		this.ventasClient = ventasClient;
		this.clientesClient = clientesClient;
	}

	@GetMapping("/resumen")
	public Map<String, Object> resumen() {
		List<Map<String, Object>> productos = inventarioCall();
		List<Map<String, Object>> ventas = ventasCall();
		List<Map<String, Object>> clientes = clientesCall();

		BigDecimal valorInventario = productos.stream()
				.map(p -> toBigDecimal(p.get("price")).multiply(BigDecimal.valueOf(toLong(p.get("stock")))))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal montoVentas = ventas.stream()
				.map(v -> toBigDecimal(v.get("total")))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		Map<String, Object> resumen = new LinkedHashMap<>();
		resumen.put("totalProductos", productos.size());
		resumen.put("totalVentas", ventas.size());
		resumen.put("totalClientes", clientes.size());
		resumen.put("valorInventario", valorInventario);
		resumen.put("montoVentas", montoVentas);
		return resumen;
	}

	@GetMapping("/inventario")
	public List<Map<String, Object>> inventario() {
		return inventarioCall();
	}

	@GetMapping("/ventas")
	public List<Map<String, Object>> ventas() {
		return ventasCall();
	}

	// Si una dependencia no responde se devuelve 503 con el motivo; antes se
	// respondia 200 con ceros y una caida parecia un negocio sin datos.
	private List<Map<String, Object>> inventarioCall() {
		try {
			return safe(inventarioClient.getProducts());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Inventario no disponible: no se puede calcular el resumen", e);
		}
	}

	private List<Map<String, Object>> ventasCall() {
		try {
			return safe(ventasClient.getSales());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Ventas no disponible: no se puede calcular el resumen", e);
		}
	}

	private List<Map<String, Object>> clientesCall() {
		try {
			return safe(clientesClient.getCustomers());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Clientes no disponible: no se puede calcular el resumen", e);
		}
	}

	private List<Map<String, Object>> safe(List<Map<String, Object>> list) {
		return list == null ? Collections.emptyList() : list;
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return BigDecimal.ZERO;
		}
		if (value instanceof BigDecimal bd) {
			return bd;
		}
		if (value instanceof Number n) {
			return new BigDecimal(n.toString());
		}
		try {
			return new BigDecimal(value.toString());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private long toLong(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
