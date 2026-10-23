package com.cibertec.productsservices.kafka;

import java.time.Instant;

// Evento de compensacion: ventas lo publica cuando se anula una venta y aqui
// se usa para devolver el stock que esa venta habia descontado.
public record SaleCancellationRequestedEvent(
		Long saleId,
		Long productId,
		Integer quantity,
		Long customerId,
		String currentStatus,
		Instant timestamp
) {
}
