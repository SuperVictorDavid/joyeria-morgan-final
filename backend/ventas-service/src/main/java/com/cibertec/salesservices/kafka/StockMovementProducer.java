package com.cibertec.salesservices.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// @Component registra esta clase como dependencia de Spring.
// En AWS el equivalente práctico sería una pieza reutilizable dentro de una Lambda productora.
@Component
@ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true")
public class StockMovementProducer {

	private static final Logger LOGGER = LoggerFactory.getLogger(StockMovementProducer.class);
	private static final long SEND_TIMEOUT_SECONDS = 5;

	private final KafkaTemplate<String, StockMovementEvent> kafkaTemplate;

	public StockMovementProducer(KafkaTemplate<String, StockMovementEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	// Espera el ack del broker: si Kafka esta caido lanza excepcion y la venta no se
	// guarda a medias (antes se perdia el descuento de stock en silencio).
	public void publish(StockMovementEvent event) {
		// Usar productId como key equivale a usar PartitionKey en Kinesis.
		// Eso garantiza que todos los movimientos del mismo producto caigan en la misma particion
		// y, por tanto, Kafka preserve el orden de esos eventos dentro de esa particion.
		String key = String.valueOf(event.productId());
		try {
			kafkaTemplate.send(KafkaTopicConfig.STOCK_MOVEMENTS_TOPIC, key, event)
					.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Envio a Kafka interrumpido", ex);
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo publicar el movimiento de stock en Kafka", ex);
		}
		LOGGER.info("Evento publicado en Kafka. topic={}, key={}, payload={}",
				KafkaTopicConfig.STOCK_MOVEMENTS_TOPIC, key, event);
	}
}
