package com.cibertec.productsservices.negocio;

import com.cibertec.productsservices.dto.ProductResponse;
import com.cibertec.productsservices.entidades.Product;
import com.cibertec.productsservices.repositorio.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

	public ProductResponse createProduct(Product product) {
		return mapToResponse(productRepository.save(product));
	}

	public ProductResponse updateProduct(Long id, Product product) {
		Product existing = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya no encontrada"));

		existing.setName(product.getName());
		existing.setDescripcion(product.getDescripcion());
		existing.setTipoProducto(product.getTipoProducto());
		existing.setTipoMaterial(product.getTipoMaterial());
		existing.setQuilates(product.getQuilates());
		existing.setPeso(product.getPeso());
		existing.setPrice(product.getPrice());
		existing.setStock(product.getStock());
		if (product.getEstado() != null) {
			existing.setEstado(product.getEstado());
		}
		return mapToResponse(productRepository.save(existing));
	}

	public void deleteProduct(Long id) {
		if (!productRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Joya no encontrada");
		}
		productRepository.deleteById(id);
	}

	public synchronized int decreaseStock(Long productId, Integer quantity) {
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
