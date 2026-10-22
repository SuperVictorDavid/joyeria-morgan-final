package com.cibertec.productsservices.dto;

import java.math.BigDecimal;

public record ProductResponse(
		Long id,
		String name,
		String descripcion,
		String tipoProducto,
		String tipoMaterial,
		BigDecimal quilates,
		BigDecimal peso,
		BigDecimal price,
		Integer stock,
		String estado
) {
}
