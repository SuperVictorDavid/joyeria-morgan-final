package com.cibertec.jwtsalesservices.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long customerId;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(length = 8)
	private String dni;

	@Column(length = 15)
	private String telefono;

	@Column(length = 200)
	private String direccion;

	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@Column(nullable = false, length = 30)
	private String status;

	public Customer() {
	}

	public Customer(Long customerId, String nombre, String email, String status) {
		this.customerId = customerId;
		this.nombre = nombre;
		this.email = email;
		this.status = status;
	}

	public Customer(Long customerId, String nombre, String dni, String telefono, String direccion, String email, String status) {
		this.customerId = customerId;
		this.nombre = nombre;
		this.dni = dni;
		this.telefono = telefono;
		this.direccion = direccion;
		this.email = email;
		this.status = status;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDni() {
		return dni;
	}

	public void setDni(String dni) {
		this.dni = dni;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
