package com.cibertec.productsservices.rest;

import com.cibertec.productsservices.dto.ProductRequest;
import com.cibertec.productsservices.dto.ProductResponse;
import com.cibertec.productsservices.negocio.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/products")
	public List<ProductResponse> getAllProducts() {
		return productService.getAllProducts();
	}

	@GetMapping("/products/{id}")
	public ProductResponse getProductById(@PathVariable Long id) {
		return productService.getProductById(id);
	}

	@PostMapping("/products")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
	}

	@PutMapping("/products/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return productService.updateProduct(id, request);
	}

	@DeleteMapping("/products/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}
}
