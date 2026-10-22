package com.joyeria.clientes.repositorio;

import com.joyeria.clientes.entidades.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	boolean existsByEmail(String email);
}
