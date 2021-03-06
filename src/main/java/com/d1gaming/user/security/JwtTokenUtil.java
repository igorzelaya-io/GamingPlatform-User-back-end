package com.d1gaming.user.security;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
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
		Map<String, Object> claims = getUserClaims(userPrincipal);
		return Jwts.builder()
				.setClaims(claims)
				.setSubject((userPrincipal.getUsername()))
				.setIssuedAt(new Date((new Date()).getTime() + jwtExpirationMs))
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}
	
	public String generateRefreshJwtToken(Map<String, Object> claims, String username) {
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(username)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}
	
	private Map<String, Object> getUserClaims(UserDetailsImpl userDetails){
		Map<String, Object> claims = new HashMap<>();
		Collection<? extends GrantedAuthority> roles = userDetails.getAuthorities();
		if(roles.contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
			claims.put("isAdmin", true);
		}
		if(roles.contains(new SimpleGrantedAuthority("ROLE_PLAYER"))) {
			claims.put("isPlayer", true);
		}
		if(roles.contains(new SimpleGrantedAuthority("ROLE_TOURNEY_MODERATOR"))) {
			claims.put("isTourneyModerator", true);
		}
		if(roles.contains(new SimpleGrantedAuthority("ROLE_CHALLENGE_MODERATOR"))) {
			claims.put("isChallengeModerator", true);
		}
		return claims;
	}
	
	public String getUserNameFromJwtToken(String token) {
		return Jwts.parser().setSigningKey(jwtSecret)
				.parseClaimsJws(token).getBody().getSubject();
	}
	
	public List<SimpleGrantedAuthority> getRolesFromToken(String token){
		List<SimpleGrantedAuthority> roles = new ArrayList<>();
		Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
		Map<String, Object> map = new HashMap<>();
		Boolean isAdmin = (Boolean) claims.get(map.put("isAdmin", true));
		Boolean isPlayer = (Boolean) claims.get(map.put("isPlayer", true));
		Boolean isTourneyModerator = (Boolean) claims.get(map.put("isTourneyModerator", true));
		Boolean isChallengeModerator = (Boolean) claims.get(map.put("isChallengeModerator", true));
		if(isAdmin != null && isAdmin == true) {
			roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		
		if(isPlayer != null && isPlayer == true) {
			roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_PLAYER"));
		}
		
		if(isTourneyModerator != null && isTourneyModerator == true) {
			roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_TOURNEY_MODERATOR"));
		}
		
		if(isChallengeModerator != null && isChallengeModerator == true) {
			roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_CHALLENGE_MODERATOR"));
		}
		
		return roles;
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
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
			return true;
		}
		
		catch(SignatureException e) {
			logger.error("Invalid JWT signature - {}",e.getMessage());
		}
		
		catch(MalformedJwtException e) {
			logger.error("Invalid JWT token - {}", e.getMessage());
		}
		
//		catch(ExpiredJwtException e) {
//			logger.error("Invalid JWT token - {}",e.getMessage());
//		}
		
		catch(UnsupportedJwtException e) {
			logger.error("Invalid JWT token - {}", e.getMessage());		
		}
		
		catch(IllegalArgumentException e) {
			logger.error("Invalid JWT token - {}", e.getMessage());
		}
		
		return false;
	}
}
