package com.cibertec.salesservices.config;

import com.cibertec.salesservices.entidades.Sale;
import com.cibertec.salesservices.repositorio.SaleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaleDataInitializer {

	@Bean
	CommandLineRunner seedSales(SaleRepository saleRepository) {
		return args -> {
			if (saleRepository.count() > 0) {
				return;
			}

			saleRepository.save(Sale.builder()
					.numeroBoleta("B001-000001")
					.productId(1L)
					.quantity(2)
					.customerId(9001L)
					.vendedor("vendedor")
					.total(new java.math.BigDecimal("5000.00"))
					.status("PROCESADO")
					.build());
			saleRepository.save(Sale.builder()
					.numeroBoleta("B001-000002")
					.productId(2L)
					.quantity(1)
					.customerId(9002L)
					.vendedor("vendedor")
					.total(new java.math.BigDecimal("850.00"))
					.status("PROCESADO")
					.build());
		};
	}
}
