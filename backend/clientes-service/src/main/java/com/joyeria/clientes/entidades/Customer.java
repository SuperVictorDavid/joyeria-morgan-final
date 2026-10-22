package com.joyeria.clientes.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

	@Column(length = 30)
	@Builder.Default
	private String status = "CONFIRMADO";

	@Column
	private LocalDateTime fechaRegistro;

	@PrePersist
	public void prePersist() {
		if (fechaRegistro == null) {
			fechaRegistro = LocalDateTime.now();
		}
		if (status == null) {
			status = "CONFIRMADO";
		}
	}
}
