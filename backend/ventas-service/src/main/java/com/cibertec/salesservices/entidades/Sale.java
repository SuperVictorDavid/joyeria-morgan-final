package com.cibertec.salesservices.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// @Entity marca la venta de joyeria como una fila persistida en base de datos.
@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long saleId;

	@Column(length = 20)
	private String numeroBoleta;

	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false)
	private Long customerId;

	@Column(length = 80)
	private String vendedor;

	@Column(precision = 10, scale = 2)
	private BigDecimal total;

	@Column(length = 30)
	@Builder.Default
	private String status = "PROCESADO";

	@Column(name = "fecha_venta")
	private LocalDateTime fechaVenta;
}
