package com.cibertec.productsservices.kafka;

import com.cibertec.productsservices.negocio.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Consumidor del topico de anulaciones: devuelve el stock de la venta eliminada.
@Component
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true")
public class SaleCancellationConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaleCancellationConsumer.class);

	private final ProductService productService;

	public SaleCancellationConsumer(ProductService productService) {
		this.productService = productService;
	}

	@KafkaListener(
			topics = "sale-cancellation-requests",
			groupId = "sale-cancellation-cg",
			containerFactory = "saleCancellationKafkaListenerContainerFactory"
	)
	public void handleSaleCancellationRequest(SaleCancellationRequestedEvent event) {
		try {
			int remainingStock = productService.increaseStock(event.productId(), event.quantity());
			LOGGER.info("[sale-cancellation-cg] Stock devuelto. saleId={}, productId={}, quantity={}, stock={}",
					event.saleId(), event.productId(), event.quantity(), remainingStock);
		} catch (ResponseStatusException ex) {
			// No se relanza: el mensaje quedaria reintentandose para siempre y frenaria la particion.
			LOGGER.error("[sale-cancellation-cg] No se pudo devolver stock. saleId={}, motivo={}",
					event.saleId(), ex.getReason());
		}
	}
}
