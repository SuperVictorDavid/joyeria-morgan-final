package com.cibertec.salesservices.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// Publica la anulacion de una venta para que inventario devuelva el stock.
@Component
@ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true")
public class SaleCancellationProducer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaleCancellationProducer.class);
	private static final long SEND_TIMEOUT_SECONDS = 5;

	private final KafkaTemplate<String, SaleCancellationRequestedEvent> kafkaTemplate;

	public SaleCancellationProducer(KafkaTemplate<String, SaleCancellationRequestedEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publish(SaleCancellationRequestedEvent event) {
		String key = String.valueOf(event.saleId());
		try {
			kafkaTemplate.send(KafkaTopicConfig.SALE_CANCELLATION_REQUESTS_TOPIC, key, event)
					.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Envio de anulacion a Kafka interrumpido", ex);
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo publicar la anulacion en Kafka", ex);
		}
		LOGGER.info("Solicitud de anulacion publicada. topic={}, key={}, payload={}",
				KafkaTopicConfig.SALE_CANCELLATION_REQUESTS_TOPIC, key, event);
	}
}
