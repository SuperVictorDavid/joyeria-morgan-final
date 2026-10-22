package com.cibertec.salesservices.dto;

import java.math.BigDecimal;

public record ProductResponse(
		Long id,
		String name,
		String descripcion,
		String tipoProducto,
		String tipoMaterial,
		java.math.BigDecimal quilates,
		java.math.BigDecimal peso,
		java.math.BigDecimal price,
		Integer stock,
		String estado
) {
}
