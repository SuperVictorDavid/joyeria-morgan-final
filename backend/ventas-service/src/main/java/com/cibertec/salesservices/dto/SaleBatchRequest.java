package com.cibertec.salesservices.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

// Una sola boleta puede agrupar varios productos: el carrito manda todo junto
// y el servicio guarda una fila por producto con el mismo numeroBoleta.
public record SaleBatchRequest(
		@NotNull(message = "El cliente es obligatorio")
		@Positive(message = "El customerId debe ser mayor a 0")
		Long customerId,

		@Size(max = 80, message = "El vendedor no debe superar 80 caracteres")
		String vendedor,

		@NotEmpty(message = "La venta debe tener al menos un producto")
		@Valid
		List<SaleItemRequest> items
) {
}
