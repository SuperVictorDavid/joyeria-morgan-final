package com.joyeria.clientes.dto;

public record CustomerResponse(

		Long customerId,
		String nombre,
		String dni,
		String telefono,
		String direccion,
		String email,
		String status,
		java.time.LocalDateTime fechaRegistro
) {
}
