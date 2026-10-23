package com.cibertec.salesservices.kafka;

import java.time.Instant;

// Peticion de anulacion: inventario la consume para devolver el stock de la venta.
public record SaleCancellationRequestedEvent(
		Long saleId,
		Long productId,
		Integer quantity,
		Long customerId,
		String currentStatus,
		Instant timestamp
) {
}
