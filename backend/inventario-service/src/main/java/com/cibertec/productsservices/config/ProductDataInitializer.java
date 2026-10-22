package com.cibertec.productsservices.config;

import com.cibertec.productsservices.entidades.Product;
import com.cibertec.productsservices.repositorio.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class ProductDataInitializer {

	@Bean
	CommandLineRunner seedProducts(ProductRepository productRepository) {
		return args -> {
			if (productRepository.count() > 0) {
				return;
			}

			productRepository.save(Product.builder()
					.name("Anillo de Oro 18K")
					.descripcion("Anillo de compromiso oro 18 quilates")
					.tipoProducto("Anillo")
					.tipoMaterial("Oro")
					.quilates(new BigDecimal("18.0"))
					.peso(new BigDecimal("5.20"))
					.price(new BigDecimal("2500.00"))
					.stock(10)
					.estado("Disponible")
					.build());
			productRepository.save(Product.builder()
					.name("Cadena de Plata 925")
					.descripcion("Cadena tipo collar plata ley 925")
					.tipoProducto("Cadena")
					.tipoMaterial("Plata")
					.quilates(new BigDecimal("0.0"))
					.peso(new BigDecimal("12.50"))
					.price(new BigDecimal("850.00"))
					.stock(25)
					.estado("Disponible")
					.build());
			productRepository.save(Product.builder()
					.name("Pulsera de Platino")
					.descripcion("Pulsera elegante platino")
					.tipoProducto("Pulsera")
					.tipoMaterial("Platino")
					.quilates(new BigDecimal("0.0"))
					.peso(new BigDecimal("8.00"))
					.price(new BigDecimal("3200.00"))
					.stock(5)
					.estado("Disponible")
					.build());
		};
	}
}
