package com.cibertec.salesservices.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Valida el JWT emitido por auth-service usando el mismo secreto compartido.
 * No consulta base de datos: solo verifica firma y expiración.
 */
@Service
public class JwtService {

	private final SecretKey secretKey;

	public JwtService(@Value("${app.jwt.secret}") String secret) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public boolean isTokenValid(String token) {
		return parseClaims(token).getExpiration().after(new Date());
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public String extractRole(String token) {
		Object role = parseClaims(token).get("role");
		return role != null ? role.toString() : "OPERADOR";
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
