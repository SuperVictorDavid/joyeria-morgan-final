package com.joyeria.clientes.rest;

import com.joyeria.clientes.dto.CustomerRequest;
import com.joyeria.clientes.dto.CustomerResponse;
import com.joyeria.clientes.negocio.CustomerService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	public List<CustomerResponse> getAll() {
		return customerService.getAll();
	}

	@GetMapping("/{id}")
	public CustomerResponse getById(@PathVariable Long id) {
		return customerService.getById(id);
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
	}

	@PutMapping("/{id}")
	public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
		return customerService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		customerService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
