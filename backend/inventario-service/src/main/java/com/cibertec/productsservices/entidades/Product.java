package com.cibertec.productsservices.entidades;

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

// @Entity marca esta clase como tabla persistente administrada por JPA.
// En AWS esto equivale a mapear un modelo que se guarda en RDS.
@Entity
@Table(name = "joyas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 200)
	private String descripcion;

	@Column(length = 30)
	private String tipoProducto;

	@Column(length = 30)
	private String tipoMaterial;

	@Column(precision = 4, scale = 1)
	private BigDecimal quilates;

	@Column(precision = 10, scale = 2)
	private BigDecimal peso;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private Integer stock;

	@Column(length = 30)
	@Builder.Default
	private String estado = "Disponible";
}
