package com.d1gaming.user.security;


import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;


@Component
public class JwtTokenUtil {

	@Value("${d1gaming.app.jwtSecret}")
	private String jwtSecret;
	
	@Value("${d1gaming.app.jwtExpirationMs}")
	private int jwtExpirationMs;
	
	
	private final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
	
	public String generateJwtToken(Authentication authentication) {
		UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
		return Jwts.builder()
				.setSubject(userPrincipal.getUsername())
				.setIssuedAt(new Date())
				.setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}
	
	public String getUserNameFromJwtToken(String token) {
		return Jwts.parser().setSigningKey(jwtSecret)
				.parseClaimsJws(token).getBody().getSubject();
	}
	
	
	public String getUserId(String token) {
		Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();
		return claims.getSubject().split(",")[0];
	}

	
	public String getUsername(String token) {
		Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();
		return claims.getSubject().split(",")[0];
	}

	public Date getExpirationDate(String token) {
		Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();
		return claims.getExpiration();
	}

	public boolean validate(String token) {
		try {
			Jwts.parser()
				.setSigningKey(jwtSecret).parseClaimsJws(token);
			return true;
		}
		catch(SignatureException e) {
			logger.error("Invalid JWT signature - {}",e.getMessage());
		}
		catch(MalformedJwtException e) {
			logger.error("Invalid JWT token - {}", e.getMessage());
		}
		catch(ExpiredJwtException e) {
			logger.error("Invalid JWT token - {}",e.getMessage());
		}
		catch(UnsupportedJwtException e) {
			logger.error("Invalid JWT token - {}", e.getMessage());		
		}
		catch(IllegalArgumentException e) {
			logger.error("Invalid JWT token - {}", e.getMessage());
		}
		return false;
	}
}
