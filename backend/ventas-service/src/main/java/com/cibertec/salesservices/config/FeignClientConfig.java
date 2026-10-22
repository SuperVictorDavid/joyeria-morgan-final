package com.cibertec.salesservices.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propaga el header Authorization de la petición entrante hacia las llamadas Feign
 * salientes (por ejemplo ventas-service -> inventario-service), para que el servicio
 * destino también pueda validar el JWT.
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
