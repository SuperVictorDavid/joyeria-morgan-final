package com.joyeria.reportes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.joyeria.reportes.client")
public class ReportesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportesServiceApplication.class, args);
	}
}
