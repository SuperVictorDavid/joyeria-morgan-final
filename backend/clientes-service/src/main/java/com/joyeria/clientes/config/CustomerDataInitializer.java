package com.joyeria.clientes.config;

import com.joyeria.clientes.entidades.Customer;
import com.joyeria.clientes.repositorio.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerDataInitializer {

	@Bean
	CommandLineRunner seedCustomers(CustomerRepository customerRepository) {
		return args -> {
			if (customerRepository.count() > 0) {
				return;
			}

			customerRepository.save(Customer.builder()
					.nombre("María Morgan")
					.dni("12345678")
					.telefono("999111222")
					.direccion("Av. Larco 123, Lima")
					.email("maria.morgan@mail.com")
					.status("CONFIRMADO")
					.build());
			customerRepository.save(Customer.builder()
					.nombre("Carlos Morgan")
					.dni("87654321")
					.telefono("999333444")
					.direccion("Jr. Unión 456, Lima")
					.email("carlos.morgan@mail.com")
					.status("CONFIRMADO")
					.build());
			customerRepository.save(Customer.builder()
					.nombre("Lucía Morgan")
					.dni("11223344")
					.telefono("999555666")
					.direccion("Av. Brasil 789, Lima")
					.email("lucia.morgan@mail.com")
					.status("CONFIRMADO")
					.build());
		};
	}
}
