package com.cibertec.salesservices.repositorio;

import com.cibertec.salesservices.entidades.Sale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SaleRepositoryTest {

	@Autowired
	private SaleRepository saleRepository;

	private Sale nuevaVenta(String boleta) {
		return Sale.builder()
				.numeroBoleta(boleta)
				.productId(1L)
				.quantity(1)
				.customerId(9001L)
				.vendedor("vendedor")
				.total(new BigDecimal("2500.00"))
				.status("PENDING")
				.build();
	}

	@Test
	void insertarYListar() {
		saleRepository.save(nuevaVenta("B001-000101"));
		saleRepository.save(nuevaVenta("B001-000102"));
		List<Sale> lista = saleRepository.findAll();
		assertTrue(lista.size() >= 2);
	}

	@Test
	void actualizar() {
		Sale s = saleRepository.save(nuevaVenta("B001-000103"));
		s.setQuantity(3);
		s.setStatus("COMPLETED");
		Sale actualizado = saleRepository.save(s);
		assertEquals(3, actualizado.getQuantity());
		assertEquals("COMPLETED", actualizado.getStatus());
	}

	@Test
	void eliminar() {
		Sale s = saleRepository.save(nuevaVenta("B001-000104"));
		Long id = s.getSaleId();
		saleRepository.deleteById(id);
		assertFalse(saleRepository.findById(id).isPresent());
	}
}
