package com.d1gaming.user.security;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.d1gaming.user.user.UserService;
import com.google.common.net.HttpHeaders;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	@Autowired
	private UserService userService;
	
	//Get authorization header and validate it.
	@Override
	protected void doFilterInternal(HttpServletRequest request, 
									HttpServletResponse response, 
									FilterChain chain)throws ServletException, IOException, NullPointerException {
		
		final String jwtHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if(!StringUtils.hasText(jwtHeader) && !jwtHeader.startsWith("Bearer ")) {
			chain.doFilter(request, response);
			return;
		}
		final String jwtToken = jwtHeader.split(" ")[1].trim();
		if(!jwtTokenUtil.validate(jwtToken)) {
			chain.doFilter(request, response);
			return;
		}
		UserDetails userDetails = null;
		try {
			userDetails = userService.getUserDetailsByUserName(jwtToken)
					.orElse(null);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userDetails, null, userDetails == null ? List.of() : userDetails.getAuthorities()
			);
		
		authentication.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		chain.doFilter(request, response);
	
	}
}
