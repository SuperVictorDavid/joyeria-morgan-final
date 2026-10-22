package com.cibertec.jwtsalesservices.config;

import com.cibertec.jwtsalesservices.entidades.AppUser;
import com.cibertec.jwtsalesservices.repositorio.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserDataInitializer {

	@Bean
	CommandLineRunner seedUsers(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (appUserRepository.count() > 0) {
				return;
			}
			appUserRepository.save(new AppUser(null, "admin", "admin@joyeriamorgan.pe",
					passwordEncoder.encode("admin123"), "ADMIN"));
			appUserRepository.save(new AppUser(null, "vendedor", "vendedor@joyeriamorgan.pe",
					passwordEncoder.encode("vendedor123"), "VENDEDOR"));
		};
	}
}
