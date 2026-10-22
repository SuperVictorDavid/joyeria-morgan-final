package com.cibertec.salesservices.dto;

public record SaleResponse(
		Long saleId,
		String numeroBoleta,
		Long productId,
		Integer quantity,
		Long customerId,
		String vendedor,
		java.math.BigDecimal total,
		String status,
		java.time.LocalDateTime fechaVenta
) {
}
