package com.joyeria.reportes.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propaga el header Authorization de la petición entrante hacia las llamadas Feign
 * salientes (reportes-service -> inventario / ventas / clientes), para que los
 * servicios destino también puedan validar el JWT.
 */
@Configuration
public class FeignClientConfig {

	@Bean
	public RequestInterceptor authForwardingInterceptor() {
		return template -> {
			ServletRequestAttributes attributes =
					(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attributes != null) {
				String token = attributes.getRequest().getHeader("Authorization");
				if (token != null && !token.isBlank()) {
					template.header("Authorization", token);
				}
			}
		};
	}
}
