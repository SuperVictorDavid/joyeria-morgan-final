package com.cibertec.salesservices.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaleItemRequest(
		@NotNull(message = "El producto es obligatorio")
		@Positive(message = "El productId debe ser mayor a 0")
		Long productId,

		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor a 0")
		Integer quantity
) {
}
