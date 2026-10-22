package com.joyeria.clientes.negocio;

import com.joyeria.clientes.dto.CustomerRequest;
import com.joyeria.clientes.dto.CustomerResponse;
import com.joyeria.clientes.entidades.Customer;
import com.joyeria.clientes.repositorio.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

	private final CustomerRepository customerRepository;

	public CustomerService(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	public List<CustomerResponse> getAll() {
		return customerRepository.findAll().stream()
				.map(this::mapToResponse)
				.toList();
	}

	public CustomerResponse getById(Long id) {
		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
		return mapToResponse(customer);
	}

	public CustomerResponse create(CustomerRequest request) {
		if (customerRepository.existsByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
		}
		Customer customer = Customer.builder()
				.nombre(request.nombre())
				.dni(request.dni())
				.telefono(request.telefono())
				.direccion(request.direccion())
				.email(request.email())
				.status("CONFIRMADO")
				.build();
		return mapToResponse(customerRepository.save(customer));
	}

	public CustomerResponse update(Long id, CustomerRequest request) {
		Customer existing = customerRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

		if (!existing.getEmail().equals(request.email())
				&& customerRepository.existsByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
		}

		existing.setNombre(request.nombre());
		existing.setDni(request.dni());
		existing.setTelefono(request.telefono());
		existing.setDireccion(request.direccion());
		existing.setEmail(request.email());
		return mapToResponse(customerRepository.save(existing));
	}

	public void delete(Long id) {
		if (!customerRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
		}
		customerRepository.deleteById(id);
	}

	private CustomerResponse mapToResponse(Customer customer) {
		return new CustomerResponse(
				customer.getCustomerId(),
				customer.getNombre(),
				customer.getDni(),
				customer.getTelefono(),
				customer.getDireccion(),
				customer.getEmail(),
				customer.getStatus(),
				customer.getFechaRegistro()
		);
	}
}
