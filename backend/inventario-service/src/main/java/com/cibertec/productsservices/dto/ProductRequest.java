package com.cibertec.productsservices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// DTO de entrada del catalogo: evita que el cliente escriba campos que no debe
// (por ejemplo el id) y valida precio y stock antes de tocar la base de datos.
public record ProductRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 120, message = "El nombre no debe superar 120 caracteres")
		String name,

		@Size(max = 200, message = "La descripcion no debe superar 200 caracteres")
		String descripcion,

		@Size(max = 30, message = "El tipo de producto no debe superar 30 caracteres")
		String tipoProducto,

		@Size(max = 30, message = "El tipo de material no debe superar 30 caracteres")
		String tipoMaterial,

		@PositiveOrZero(message = "Los quilates no pueden ser negativos")
		BigDecimal quilates,

		@PositiveOrZero(message = "El peso no puede ser negativo")
		BigDecimal peso,

		@NotNull(message = "El precio es obligatorio")
		@Positive(message = "El precio debe ser mayor a 0")
		BigDecimal price,

		@NotNull(message = "El stock es obligatorio")
		@PositiveOrZero(message = "El stock no puede ser negativo")
		Integer stock,

		@Size(max = 30, message = "El estado no debe superar 30 caracteres")
		String estado
) {
}
