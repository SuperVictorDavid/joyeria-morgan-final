package com.joyeria.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(

		@NotBlank(message = "El nombre es obligatorio")
		String nombre,

		@Size(min = 8, max = 8, message = "El DNI debe tener 8 caracteres")
		String dni,

		String telefono,

		String direccion,

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es válido")
		String email
) {
}
