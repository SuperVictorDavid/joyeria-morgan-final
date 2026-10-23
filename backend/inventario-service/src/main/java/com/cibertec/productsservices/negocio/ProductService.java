package com.cibertec.productsservices.negocio;

import com.cibertec.productsservices.dto.ProductRequest;
import com.cibertec.productsservices.dto.ProductResponse;
import com.cibertec.productsservices.entidades.Product;
import com.cibertec.productsservices.repositorio.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// @Service registra lógica de negocio del catálogo y stock.
// En AWS este código viviría dentro de una Lambda o un servicio contenedorizado consumidor.
@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public ProductResponse getProductById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

		return mapToResponse(product);
	}

	public List<ProductResponse> getAllProducts() {
		return productRepository.findAll().stream()
				.map(this::mapToResponse)
				.toList();
	}

	public ProductResponse createProduct(ProductRequest request) {
		Product product = Product.builder()
				.name(request.name())
				.descripcion(request.descripcion())
				.tipoProducto(request.tipoProducto())
				.tipoMaterial(request.tipoMaterial())
				.quilates(request.quilates())
				.peso(request.peso())
				.price(request.price())
				.stock(request.stock())
				.estado(request.estado() != null ? request.estado() : "Disponible")
				.build();
		return mapToResponse(productRepository.save(product));
	}

	public ProductResponse updateProduct(Long id, ProductRequest request) {
		Product existing = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya no encontrada"));

		existing.setName(request.name());
		existing.setDescripcion(request.descripcion());
		existing.setTipoProducto(request.tipoProducto());
		existing.setTipoMaterial(request.tipoMaterial());
		existing.setQuilates(request.quilates());
		existing.setPeso(request.peso());
		existing.setPrice(request.price());
		existing.setStock(request.stock());
		if (request.estado() != null) {
			existing.setEstado(request.estado());
		}
		return mapToResponse(productRepository.save(existing));
	}

	public void deleteProduct(Long id) {
		if (!productRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya no encontrada");
		}
		productRepository.deleteById(id);
	}

	// Descuenta stock por una venta. Rechaza cantidades nulas, cero o negativas:
	// una cantidad negativa terminaria SUMANDO stock en lugar de restarlo.
	@Transactional
	public synchronized int decreaseStock(Long productId, Integer quantity) {
		validateQuantity(quantity);

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya no encontrada"));

		if (product.getStock() < quantity) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para procesar la venta");
		}

		product.setStock(product.getStock() - quantity);
		if (product.getStock() == 0 && !"Vendido".equals(product.getEstado())) {
			product.setEstado("Vendido");
		}
		productRepository.save(product);
		return product.getStock();
	}

	// Devuelve el stock cuando se anula una venta (evento de compensacion).
	@Transactional
	public synchronized int increaseStock(Long productId, Integer quantity) {
		validateQuantity(quantity);

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya no encontrada"));

		product.setStock(product.getStock() + quantity);
		if (product.getStock() > 0 && "Vendido".equals(product.getEstado())) {
			product.setEstado("Disponible");
		}
		productRepository.save(product);
		return product.getStock();
	}

	private void validateQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor a 0");
		}
	}

	private ProductResponse mapToResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescripcion(),
				product.getTipoProducto(),
				product.getTipoMaterial(),
				product.getQuilates(),
				product.getPeso(),
				product.getPrice(),
				product.getStock(),
				product.getEstado()
		);
	}
}
