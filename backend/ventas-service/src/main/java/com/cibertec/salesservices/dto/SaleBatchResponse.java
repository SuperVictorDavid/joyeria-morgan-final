package com.cibertec.salesservices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Respuesta del checkout completo: una boleta y el detalle de cada producto.
public record SaleBatchResponse(
		String numeroBoleta,
		Long customerId,
		String vendedor,
		BigDecimal total,
		LocalDateTime fechaVenta,
		List<SaleResponse> items
) {
}
