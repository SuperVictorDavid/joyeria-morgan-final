package com.cibertec.salesservices.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaleRequest(
		@NotNull(message = "El producto es obligatorio")
		@Positive(message = "El productId debe ser mayor a 0")
		Long productId,

		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor a 0")
		Integer quantity,

		@NotNull(message = "El cliente es obligatorio")
		@Positive(message = "El customerId debe ser mayor a 0")
		Long customerId,

		@Size(max = 80, message = "El vendedor no debe superar 80 caracteres")
		String vendedor
) {
}
