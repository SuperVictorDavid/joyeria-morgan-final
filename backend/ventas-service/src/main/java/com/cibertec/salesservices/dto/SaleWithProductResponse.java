package com.cibertec.salesservices.dto;

import java.math.BigDecimal;

public record SaleWithProductResponse(
		Long saleId,
		String numeroBoleta,
		Long productId,
		Integer quantity,
		Long customerId,
		String vendedor,
		String status,
		BigDecimal total,
		java.time.LocalDateTime fechaVenta,
		ProductResponse product
) {
}
