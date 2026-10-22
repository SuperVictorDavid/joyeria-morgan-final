package com.cibertec.salesservices.negocio;

import com.cibertec.salesservices.dto.SaleRequest;
import com.cibertec.salesservices.dto.SaleResponse;
import com.cibertec.salesservices.dto.SaleWithProductResponse;
import com.cibertec.salesservices.dto.ProductResponse;
import com.cibertec.salesservices.client.ProductClient;
import com.cibertec.salesservices.entidades.Sale;
import com.cibertec.salesservices.kafka.StockMovementProducer;
import com.cibertec.salesservices.kafka.StockMovementEvent;
import com.cibertec.salesservices.rabbitmq.StockReserveEvent;
import com.cibertec.salesservices.rabbitmq.StockReserveProducer;
import com.cibertec.salesservices.repositorio.SaleRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

// @Service registra lógica de negocio administrada por Spring.
// En AWS este rol normalmente vive dentro del código de una Lambda o de un microservicio ECS.
@Service
public class SaleService {

	private final SaleRepository saleRepository;
	private final StockMovementProducer stockMovementProducer;
	private final StockReserveProducer stockReserveProducer;
	private final ProductClient productClient;

	public SaleService(
			SaleRepository saleRepository,
			StockMovementProducer stockMovementProducer,
			StockReserveProducer stockReserveProducer,
			ProductClient productClient
	) {
		this.saleRepository = saleRepository;
		this.stockMovementProducer = stockMovementProducer;
		this.stockReserveProducer = stockReserveProducer;
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
		ProductResponse product;
		try {
			product = productClient.getProductById(sale.getProductId());
		} catch (FeignException.NotFound e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya " + sale.getProductId() + " no existe");
		}
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
		Sale sale = saleRepository.findById(saleId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Inventario no disponible, intente más tarde", e);
	}

	public SaleResponse createSale(SaleRequest request) {
		// Validación de stock vía Feign: único fallo de negocio permitido (409).
		// Si inventario no responde, se sigue (Resilience4j/CB lo reporta en details-resiliente).
		BigDecimal totalCalculado = null;
		try {
			ProductResponse product = productClient.getProductById(request.productId());
			if (product.stock() != null && product.stock() < request.quantity()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para procesar la venta");
			}
			totalCalculado = product.price().multiply(BigDecimal.valueOf(request.quantity()));
		} catch (ResponseStatusException rse) {
			throw rse;
		} catch (Exception ignored) {
		}

		Sale storedSale = saveNewSale(request);
		storedSale.setFechaVenta(LocalDateTime.now());
		if (totalCalculado != null) {
			storedSale.setTotal(totalCalculado);
			storedSale.setNumeroBoleta("B001-" + String.format("%06d", storedSale.getSaleId()));
		}
		storedSale = saleRepository.save(storedSale);

		// Best-effort: si Kafka está caído, la venta igual se guarda (no bloquea).
		try {
			stockMovementProducer.publish(new StockMovementEvent(
					storedSale.getSaleId(),
					storedSale.getProductId(),
					storedSale.getQuantity(),
					storedSale.getCustomerId(),
					Instant.now()
			));
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger(SaleService.class)
					.warn("Kafka no disponible, venta {} guardada sin evento: {}", storedSale.getSaleId(), e.toString());
		}

		return buildSaleResponse(storedSale);
	}

	public SaleResponse createSaleWithRabbitReserve(SaleRequest request) {
		try {
			ProductResponse product = productClient.getProductById(request.productId());
			if (product.stock() != null && product.stock() < request.quantity()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para procesar la venta");
			}
		} catch (ResponseStatusException rse) {
			throw rse;
		} catch (Exception ignored) {
		}
		Sale storedSale = saveNewSale(request);

		try {
			stockReserveProducer.publish(new StockReserveEvent(
					storedSale.getSaleId(),
					storedSale.getProductId(),
					storedSale.getQuantity(),
					storedSale.getCustomerId()
			));
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger(SaleService.class)
					.warn("RabbitMQ no disponible, venta {} guardada sin evento: {}", storedSale.getSaleId(), e.toString());
		}

		return buildSaleResponse(storedSale);
	}

	public SaleResponse updateSale(Long saleId, SaleRequest request) {
		Sale currentSale = saleRepository.findById(saleId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

		currentSale.setProductId(request.productId());
		currentSale.setQuantity(request.quantity());
		currentSale.setCustomerId(request.customerId());
		if (request.vendedor() != null) {
			currentSale.setVendedor(request.vendedor());
		}
		return buildSaleResponse(saleRepository.save(currentSale));
	}

	public void deleteSale(Long saleId) {
		if (!saleRepository.existsById(saleId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada");
		}
		saleRepository.deleteById(saleId);
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

	private Sale saveNewSale(SaleRequest request) {
		Sale sale = Sale.builder()
				.productId(request.productId())
				.quantity(request.quantity())
				.customerId(request.customerId())
				.vendedor(request.vendedor())
				.status("PROCESADO")
				.build();
		Sale saved = saleRepository.save(sale);
		saved.setNumeroBoleta("B001-" + String.format("%06d", saved.getSaleId()));
		return saleRepository.save(saved);
	}
}
