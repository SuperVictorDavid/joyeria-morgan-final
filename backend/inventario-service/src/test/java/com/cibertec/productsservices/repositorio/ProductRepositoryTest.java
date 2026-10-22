package com.cibertec.productsservices.repositorio;

import com.cibertec.productsservices.entidades.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	private Product nuevaJoya(String nombre) {
		return Product.builder()
				.name(nombre)
				.descripcion("Joya de prueba Morgan")
				.tipoProducto("Anillo")
				.tipoMaterial("Oro")
				.quilates(new BigDecimal("18.0"))
				.peso(new BigDecimal("5.20"))
				.price(new BigDecimal("2500.00"))
				.stock(10)
				.estado("Disponible")
				.build();
	}

	@Test
	void insertarYListar() {
		productRepository.save(nuevaJoya("Anillo de Oro 18K"));
		productRepository.save(nuevaJoya("Cadena de Plata 925"));
		List<Product> lista = productRepository.findAll();
		assertTrue(lista.size() >= 2);
	}

	@Test
	void actualizar() {
		Product p = productRepository.save(nuevaJoya("Pulsera Test"));
		p.setPrice(new BigDecimal("999.99"));
		p.setStock(3);
		Product actualizado = productRepository.save(p);
		assertEquals(new BigDecimal("999.99"), actualizado.getPrice());
		assertEquals(3, actualizado.getStock());
	}

	@Test
	void eliminar() {
		Product p = productRepository.save(nuevaJoya("Dije Test"));
		Long id = p.getId();
		productRepository.deleteById(id);
		assertFalse(productRepository.findById(id).isPresent());
	}
}
