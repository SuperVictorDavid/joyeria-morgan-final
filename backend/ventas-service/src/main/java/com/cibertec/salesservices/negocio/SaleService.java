package com.cibertec.salesservices.negocio;

import com.cibertec.salesservices.client.ProductClient;
import com.cibertec.salesservices.dto.ProductResponse;
import com.cibertec.salesservices.dto.SaleBatchRequest;
import com.cibertec.salesservices.dto.SaleBatchResponse;
import com.cibertec.salesservices.dto.SaleItemRequest;
import com.cibertec.salesservices.dto.SaleRequest;
import com.cibertec.salesservices.dto.SaleResponse;
import com.cibertec.salesservices.dto.SaleWithProductResponse;
import com.cibertec.salesservices.entidades.Sale;
import com.cibertec.salesservices.kafka.SaleCancellationProducer;
import com.cibertec.salesservices.kafka.SaleCancellationRequestedEvent;
import com.cibertec.salesservices.kafka.StockMovementEvent;
import com.cibertec.salesservices.kafka.StockMovementProducer;
import com.cibertec.salesservices.rabbitmq.StockReserveEvent;
import com.cibertec.salesservices.rabbitmq.StockReserveProducer;
import com.cibertec.salesservices.repositorio.SaleRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// @Service registra lógica de negocio administrada por Spring.
// En AWS este rol normalmente vive dentro del código de una Lambda o de un microservicio ECS.
@Service
public class SaleService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaleService.class);

	private final SaleRepository saleRepository;
	private final ObjectProvider<StockMovementProducer> stockMovementProducer;
	private final ObjectProvider<StockReserveProducer> stockReserveProducer;
	private final ObjectProvider<SaleCancellationProducer> saleCancellationProducer;
	private final ProductClient productClient;

	public SaleService(
			SaleRepository saleRepository,
			ObjectProvider<StockMovementProducer> stockMovementProducer,
			ObjectProvider<StockReserveProducer> stockReserveProducer,
			ObjectProvider<SaleCancellationProducer> saleCancellationProducer,
			ProductClient productClient
	) {
		this.saleRepository = saleRepository;
		this.stockMovementProducer = stockMovementProducer;
		this.stockReserveProducer = stockReserveProducer;
		this.saleCancellationProducer = saleCancellationProducer;
		this.productClient = productClient;
	}

	public SaleResponse getSaleById(Long saleId) {
		Sale sale = saleRepository.findById(saleId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

		return buildSaleResponse(sale);
	}

	public List<SaleResponse> getAllSales() {
		return saleRepository.findAll().stream()
				.map(this::buildSaleResponse)
				.toList();
	}

	public SaleWithProductResponse getSaleDetailsWithFeign(Long saleId) {
		Sale sale = saleRepository.findById(saleId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
		ProductResponse product = findProduct(sale.getProductId());
		BigDecimal total = product.price().multiply(BigDecimal.valueOf(sale.getQuantity()));

		return new SaleWithProductResponse(
				sale.getSaleId(),
				sale.getNumeroBoleta(),
				sale.getProductId(),
				sale.getQuantity(),
				sale.getCustomerId(),
				sale.getVendedor(),
				sale.getStatus(),
				sale.getTotal() != null ? sale.getTotal() : total,
				sale.getFechaVenta(),
				product
		);
	}

	@CircuitBreaker(name = "inventario-service", fallbackMethod = "fallbackDetalle")
	@Retry(name = "inventario-service")
	public SaleWithProductResponse getSaleDetailsResiliente(Long saleId) {
		return getSaleDetailsWithFeign(saleId);
	}

	@SuppressWarnings("unused")
	public SaleWithProductResponse fallbackDetalle(Long saleId, Throwable e) {
		if (e instanceof ResponseStatusException rse) {
			throw rse;
		}
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Inventario no disponible, intente más tarde", e);
	}

	// Venta por Kafka (T2): el total sale de Feign, la venta se guarda una sola vez y
	// el descuento de stock se confirma con el broker antes de responder 200.
	public SaleResponse createSale(SaleRequest request) {
		ProductResponse product = findProduct(request.productId());
		validateStock(product, request.quantity());
		BigDecimal total = calculateTotal(product, request.quantity());
		Sale storedSale = saveNewSale(request, total);

		try {
			publishStockMovement(storedSale.getSaleId(), storedSale.getProductId(),
					storedSale.getQuantity(), storedSale.getCustomerId());
		} catch (ResponseStatusException ex) {
			// Sin evento no hay descuento de stock: se deshace la venta para no dejar datos falsos.
			saleRepository.delete(storedSale);
			throw ex;
		}

		return buildSaleResponse(storedSale);
	}

	// Venta por RabbitMQ (T2): mismo control de stock y total que la venta por Kafka.
	public SaleResponse createSaleWithRabbitReserve(SaleRequest request) {
		ProductResponse product = findProduct(request.productId());
		validateStock(product, request.quantity());
		BigDecimal total = calculateTotal(product, request.quantity());
		Sale storedSale = saveNewSale(request, total);

		try {
			publishStockReserve(storedSale.getSaleId(), storedSale.getProductId(),
					storedSale.getQuantity(), storedSale.getCustomerId());
		} catch (ResponseStatusException ex) {
			saleRepository.delete(storedSale);
			throw ex;
		}

		return buildSaleResponse(storedSale);
	}

	// Checkout con varios productos: valida todo antes de guardar, comparte una sola
	// boleta entre las filas y publica un evento de stock por producto.
	public SaleBatchResponse createSaleBatch(SaleBatchRequest request) {
		// Si el carrito repite un producto, se suma la cantidad para validar el stock una sola vez.
		Map<Long, Integer> cantidades = new LinkedHashMap<>();
		for (SaleItemRequest item : request.items()) {
			cantidades.merge(item.productId(), item.quantity(), Integer::sum);
		}

		Map<Long, ProductResponse> productos = new LinkedHashMap<>();
		for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
			ProductResponse product = findProduct(entry.getKey());
			validateStock(product, entry.getValue());
			productos.put(entry.getKey(), product);
		}

		LocalDateTime fechaVenta = LocalDateTime.now();
		List<Sale> guardadas = new ArrayList<>();
		String numeroBoleta = null;

		try {
			for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
				ProductResponse product = productos.get(entry.getKey());
				Sale sale = Sale.builder()
						.productId(entry.getKey())
						.quantity(entry.getValue())
						.customerId(request.customerId())
						.vendedor(request.vendedor())
						.total(calculateTotal(product, entry.getValue()))
						.fechaVenta(fechaVenta)
						.status("PROCESADO")
						.build();
				Sale saved = saleRepository.save(sale);
				if (numeroBoleta == null) {
					numeroBoleta = "B001-" + String.format("%06d", saved.getSaleId());
				}
				saved.setNumeroBoleta(numeroBoleta);
				guardadas.add(saleRepository.save(saved));
			}
		} catch (RuntimeException ex) {
			guardadas.forEach(saleRepository::delete);
			throw ex;
		}

		List<Sale> publicadas = new ArrayList<>();
		try {
			for (Sale sale : guardadas) {
				publishStockMovement(sale.getSaleId(), sale.getProductId(),
						sale.getQuantity(), sale.getCustomerId());
				publicadas.add(sale);
			}
		} catch (ResponseStatusException ex) {
			// Si Kafka falla a mitad del checkout, se devuelve el stock de lo ya publicado
			// y se borran todas las filas para no dejar una boleta incompleta.
			for (Sale sale : publicadas) {
				try {
					publishSaleCancellation(sale.getSaleId(), sale.getProductId(), sale.getQuantity(),
							sale.getCustomerId(), sale.getStatus());
				} catch (RuntimeException cancelEx) {
					LOGGER.error("No se pudo revertir el stock. saleId={}, motivo={}",
							sale.getSaleId(), cancelEx.getMessage());
				}
			}
			guardadas.forEach(saleRepository::delete);
			throw ex;
		}

		BigDecimal total = guardadas.stream()
				.map(Sale::getTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<SaleResponse> items = guardadas.stream().map(this::buildSaleResponse).toList();

		return new SaleBatchResponse(numeroBoleta, request.customerId(), request.vendedor(),
				total, fechaVenta, items);
	}

	// Al cambiar producto o cantidad se recalcula el total y se ajusta el stock:
	// se devuelve lo reservado antes y se descuenta lo nuevo.
	public SaleResponse updateSale(Long saleId, SaleRequest request) {
		Sale currentSale = saleRepository.findById(saleId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

		ProductResponse newProduct = findProduct(request.productId());
		int quantityAlreadyReserved = currentSale.getProductId().equals(request.productId())
				? currentSale.getQuantity()
				: 0;
		validateStockForUpdate(newProduct, request.quantity(), quantityAlreadyReserved);

		if (currentSale.getProductId().equals(request.productId())) {
			int difference = request.quantity() - currentSale.getQuantity();
			if (difference > 0) {
				publishStockMovement(saleId, request.productId(), difference, request.customerId());
			} else if (difference < 0) {
				publishSaleCancellation(saleId, currentSale.getProductId(), -difference,
						currentSale.getCustomerId(), currentSale.getStatus());
			}
		} else {
			publishSaleCancellation(saleId, currentSale.getProductId(), currentSale.getQuantity(),
					currentSale.getCustomerId(), currentSale.getStatus());
			publishStockMovement(saleId, request.productId(), request.quantity(), request.customerId());
		}

		currentSale.setProductId(request.productId());
		currentSale.setQuantity(request.quantity());
		currentSale.setCustomerId(request.customerId());
		currentSale.setTotal(calculateTotal(newProduct, request.quantity()));
		if (request.vendedor() != null) {
			currentSale.setVendedor(request.vendedor());
		}
		return buildSaleResponse(saleRepository.save(currentSale));
	}

	// Borrar una venta publica la anulacion para que inventario devuelva el stock
	// antes de eliminar la fila; si Kafka falla, la venta no se borra.
	public SaleResponse deleteSale(Long saleId) {
		Sale sale = saleRepository.findById(saleId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

		publishSaleCancellation(sale.getSaleId(), sale.getProductId(), sale.getQuantity(),
				sale.getCustomerId(), sale.getStatus());

		saleRepository.delete(sale);
		return buildSaleResponse(sale);
	}

	private ProductResponse findProduct(Long productId) {
		try {
			return productClient.getProductById(productId);
		} catch (FeignException.NotFound ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya " + productId + " no existe");
		} catch (FeignException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Inventario no disponible, intente más tarde", ex);
		}
	}

	private void validateStock(ProductResponse product, Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor a 0");
		}
		if (product.stock() == null || product.stock() < quantity) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para procesar la venta");
		}
	}

	private void validateStockForUpdate(ProductResponse product, Integer quantity, int quantityAlreadyReserved) {
		if (quantity == null || quantity <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor a 0");
		}
		int available = (product.stock() != null ? product.stock() : 0) + quantityAlreadyReserved;
		if (available < quantity) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para actualizar la venta");
		}
	}

	private BigDecimal calculateTotal(ProductResponse product, Integer quantity) {
		return product.price().multiply(BigDecimal.valueOf(quantity));
	}

	private void publishStockMovement(Long saleId, Long productId, Integer quantity, Long customerId) {
		StockMovementProducer producer = stockMovementProducer.getIfAvailable();
		if (producer == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Kafka no esta habilitado: la venta no se registro");
		}
		try {
			producer.publish(new StockMovementEvent(saleId, productId, quantity, customerId, Instant.now()));
		} catch (RuntimeException ex) {
			LOGGER.error("Kafka no disponible. saleId={}, motivo={}", saleId, ex.getMessage());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Kafka no disponible: la venta no se registro", ex);
		}
	}

	private void publishStockReserve(Long saleId, Long productId, Integer quantity, Long customerId) {
		StockReserveProducer producer = stockReserveProducer.getIfAvailable();
		if (producer == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"RabbitMQ no esta habilitado: la venta no se registro");
		}
		try {
			producer.publish(new StockReserveEvent(saleId, productId, quantity, customerId));
		} catch (RuntimeException ex) {
			LOGGER.error("RabbitMQ no disponible. saleId={}, motivo={}", saleId, ex.getMessage());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"RabbitMQ no disponible: la venta no se registro", ex);
		}
	}

	private void publishSaleCancellation(Long saleId, Long productId, Integer quantity, Long customerId, String status) {
		SaleCancellationProducer producer = saleCancellationProducer.getIfAvailable();
		if (producer == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Kafka no esta habilitado: no se puede devolver el stock");
		}
		try {
			producer.publish(new SaleCancellationRequestedEvent(
					saleId, productId, quantity, customerId, status, Instant.now()));
		} catch (RuntimeException ex) {
			LOGGER.error("No se pudo publicar la anulacion. saleId={}, motivo={}", saleId, ex.getMessage());
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Kafka no disponible: no se pudo devolver el stock", ex);
		}
	}

	private SaleResponse buildSaleResponse(Sale sale) {
		return new SaleResponse(
				sale.getSaleId(),
				sale.getNumeroBoleta(),
				sale.getProductId(),
				sale.getQuantity(),
				sale.getCustomerId(),
				sale.getVendedor(),
				sale.getTotal(),
				sale.getStatus(),
				sale.getFechaVenta()
		);
	}

	private Sale saveNewSale(SaleRequest request, BigDecimal total) {
		Sale sale = Sale.builder()
				.productId(request.productId())
				.quantity(request.quantity())
				.customerId(request.customerId())
				.vendedor(request.vendedor())
				.total(total)
				.fechaVenta(LocalDateTime.now())
				.status("PROCESADO")
				.build();
		Sale saved = saleRepository.save(sale);
		saved.setNumeroBoleta("B001-" + String.format("%06d", saved.getSaleId()));
		return saleRepository.save(saved);
	}
}
