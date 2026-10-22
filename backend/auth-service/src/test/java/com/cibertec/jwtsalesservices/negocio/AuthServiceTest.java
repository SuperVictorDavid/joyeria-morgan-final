package com.cibertec.jwtsalesservices.negocio;

import com.cibertec.jwtsalesservices.dto.LoginRequest;
import com.cibertec.jwtsalesservices.dto.LoginResponse;
import com.cibertec.jwtsalesservices.entidades.AppUser;
import com.cibertec.jwtsalesservices.repositorio.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:joyeria_auth_test;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.kafka.bootstrap-servers=",
		"spring.rabbitmq.host=",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void passwordCifradoBCrypt() {
		AppUser user = new AppUser(null, "testuser", "test@joyeria.pe",
				passwordEncoder.encode("clave123"), "VENDEDOR");
		AppUser guardado = appUserRepository.save(user);
		assertNotEquals("clave123", guardado.getPassword());
		assertTrue(passwordEncoder.matches("clave123", guardado.getPassword()));
	}

	@Test
	void loginOk() {
		String username = "logintest";
		appUserRepository.save(new AppUser(null, username, "login@joyeria.pe",
				passwordEncoder.encode("admin123"), "ADMIN"));
		LoginResponse res = authService.login(new LoginRequest(username, "admin123"));
		assertNotNull(res.token());
		assertEquals("Bearer", res.tokenType());
	}

	@Test
	void loginFalla() {
		assertThrows(ResponseStatusException.class,
				() -> authService.login(new LoginRequest("noexiste", "xxx")));
	}

	@Test
	void crudUsuarios() {
		AppUser u = appUserRepository.save(new AppUser(null, "crud1", "c1@joyeria.pe",
				passwordEncoder.encode("123"), "ADMIN"));
		List<AppUser> lista = appUserRepository.findAll();
		assertTrue(lista.size() >= 1);
		u.setRole("VENDEDOR");
		assertEquals("VENDEDOR", appUserRepository.save(u).getRole());
		appUserRepository.deleteById(u.getId());
		assertFalse(appUserRepository.findById(u.getId()).isPresent());
	}
}
