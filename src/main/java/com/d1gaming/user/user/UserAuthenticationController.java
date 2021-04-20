package com.d1gaming.user.user;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.request.UserLoginRequest;
import com.d1gaming.library.request.UserRegisterRequest;
import com.d1gaming.library.response.JwtResponse;
import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.role.Role;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamInviteRequest;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserDetailsImpl;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.library.user.UserTournament;
import com.d1gaming.user.role.RoleService;
import com.d1gaming.user.security.JwtTokenUtil;

@RestController
@RequestMapping("/auth")
@CrossOrigin
@PreAuthorize("permitAll()")
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
	public ResponseEntity<Object> login(@Valid @RequestBody UserLoginRequest request){
		Authentication authentication = authenticationManager.authenticate(
											new UsernamePasswordAuthenticationToken(request.getUserName(),
																					request.getUserPassword()
											));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		User authenticatedUser = UserDetailsImpl.toUser((UserDetailsImpl) authentication.getPrincipal());
		String jwt = jwtUtils.generateAccessToken(authenticatedUser);
		return new ResponseEntity<Object>(new JwtResponse(jwt, authenticatedUser.getUserId()), HttpStatus.OK);
	}
	
	@PostMapping("/register")
	public ResponseEntity<Object> registerUser(@RequestBody UserRegisterRequest registerRequest) throws InterruptedException, ExecutionException {
		if(userService.getUserByUserName(registerRequest.getUserName()) != null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Username is already taken."));
		}
		if(userService.getUserByEmail(registerRequest.getUserEmail()) != null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Email is already taken."));
		}
		
		List<Team> userTeams = new ArrayList<Team>();
		List<TeamInviteRequest> userTeamInvites = new ArrayList<TeamInviteRequest>();
		User user = new User(registerRequest.getUserRealName(),registerRequest.getUserName()
							,registerRequest.getUserPassword(),registerRequest.getUserEmail(),UserStatus.ACTIVE,
							userTeams, userTeamInvites,
							registerRequest.getUserCountry(),registerRequest.getUserBirthDate(), 0.0, 0,0,0);
		
		List<String> strRoles = registerRequest.getUserRoles();
		List<Role> roles = new ArrayList<>();
		if(strRoles == null) {
			Role userRole = new Role( Role.PLAYER );
			roles.add(userRole);
		}
		else {
			strRoles.forEach(role -> {
				switch(role) {
					case "Administrator":
						try {
							Role adminRole = new Role( Role.ADMIN );
							roles.add(adminRole);
							break;
						} 
						catch (RuntimeException e) {
							e.printStackTrace();
						}
					case "ChallengeModerator":
						try {
							Role moderator = new Role( Role.CHALLENGE_ADMIN );
							roles.add(moderator);
							break;
						}
						catch(RuntimeException e) {
							e.printStackTrace();
						}
					case "TourneyModerator":
						try {
							Role moderator = new Role( Role.TOURNEY_ADMIN );	
							roles.add(moderator);
							break;
						}
						catch(RuntimeException  e) {
							e.printStackTrace();
						}
					case "Player":
						try {
							Role player = new Role( Role.PLAYER );
							roles.add(player);
							break;
						}
						catch(RuntimeException e) {
							e.printStackTrace();
						}
					default:
						throw new RuntimeException("Error: Role does not exist.");
					}
			});
		}
		user.setUserRoles(roles);
		try {
			userService.saveUser(user);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return new ResponseEntity<Object>(new MessageResponse("Something went wrong. Please try again later."), HttpStatus.EXPECTATION_FAILED);
		}
		return new ResponseEntity<Object>(new MessageResponse("User created Successfully."), HttpStatus.OK);
	}
}