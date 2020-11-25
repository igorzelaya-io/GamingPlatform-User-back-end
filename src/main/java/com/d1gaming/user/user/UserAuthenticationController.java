package com.d1gaming.user.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.response.JwtResponse;
import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.role.ERole;
import com.d1gaming.library.role.Role;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserLoginRequest;
import com.d1gaming.library.user.UserRegisterRequest;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.user.role.RoleService;
import com.d1gaming.user.security.JwtTokenUtil;
import com.d1gaming.user.security.UserDetailsImpl;

@CrossOrigin(origins = "localhost:4200")
@RestController
@RequestMapping(path = "/auth")
public class UserAuthenticationController {
	
	@Autowired
	AuthenticationManager authenticationManager;
	
	@Autowired
	RoleService roleService;
	
	@Autowired
	UserService userService;
	
	@Autowired
	JwtTokenUtil jwtUtils;
		
	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest request){
		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUserName(),request.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());
		return ResponseEntity.ok(new JwtResponse(
					jwt, userDetails.getUserId(),userDetails.getUsername(),userDetails.getUserEmail(),roles
				));
	}
	
	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegisterRequest registerRequest) throws InterruptedException, ExecutionException {
		if(userService.getUserByUserName(registerRequest.getUserName()) != null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Username is already taken."));
		}
		if(userService.getUserByEmail(registerRequest.getUserEmail()) != null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Email is already taken."));
		}
		User user = new User(registerRequest.getUserRealName(),registerRequest.getUserName()
						,	registerRequest.getUserPassword(),registerRequest.getUserEmail(),UserStatus.ACTIVE,
						registerRequest.getUserCountry(),registerRequest.getUserBirthDate()
						);
		Set<String> strRoles = registerRequest.getAuthorities();
		List<Role> roles = new ArrayList<>();
		if(strRoles == null) {
			Role userRole = roleService.getRoleByType(ERole.ROLE_PLAYER)
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			roles.add(userRole);
		}
		else {
			strRoles.forEach(role -> {
				switch(role) {
					case "Administrator":
						try {
							Role adminRole = roleService.getRoleByType(ERole.ROLE_ADMIN)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
							roles.add(adminRole);
							break;
						} 
						catch (RuntimeException | InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					case "ChallengeModerator":
						try {
							Role moderator = roleService.getRoleByType(ERole.ROLE_CHALLENGE_MODERATOR)
									.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
							roles.add(moderator);
							break;
						}
						catch(RuntimeException | InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					case "TourneyModerator":
						try {
							Role moderator = roleService.getRoleByType(ERole.ROLE_TOURNEY_MODERATOR)
									.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
							roles.add(moderator);
							break;
						}
						catch(RuntimeException | ExecutionException | InterruptedException e) {
							e.printStackTrace();
						}
					default:
						try {
							Role userRole = roleService.getRoleByType(ERole.ROLE_PLAYER)
								.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
							roles.add(userRole);
							break;
						}
						catch(RuntimeException | ExecutionException | InterruptedException e) {
							e.printStackTrace();
						}
					}
			});
		}
		user.setUserRoles(roles);
		try {
			userService.saveUser(user);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return ResponseEntity.ok(new MessageResponse("User created successfully."));
	}
}