package com.d1gaming.user.userteam;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamInviteRequest;

@RestController
@CrossOrigin(origins = "localhost:4200")
@RequestMapping(value = "/userteamapi")
@PreAuthorize("permitAll()")
public class UserTeamController {

	@Autowired
	private UserTeamService userTeamService;
	
	@GetMapping(value = "/userTeamRequests")
	public ResponseEntity<List<TeamInviteRequest>> getAllUserTeamRequests(@RequestParam(required = true)String userId) throws InterruptedException, ExecutionException{
		List<TeamInviteRequest> userTeamRequests = userTeamService.getAllTeamRequests(userId);
		if(userTeamRequests == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		else if(userTeamRequests.isEmpty()) {
			return new ResponseEntity<List<TeamInviteRequest>>(userTeamRequests, HttpStatus.NO_CONTENT);
		}
		else {
			return new ResponseEntity<List<TeamInviteRequest>>(userTeamRequests, HttpStatus.OK);
		}
	}
	
	@GetMapping(value ="/userTeams")
	public ResponseEntity<List<Team>> getAllUserTeams(@RequestParam(required = true)String userId) throws InterruptedException, ExecutionException{
		List<Team> userTeams = userTeamService.getAllUserTeams(userId);
		if(userTeams == null || userTeams.isEmpty()) {
			return new ResponseEntity<List<Team>>(userTeams, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Team>>(userTeams, HttpStatus.OK);
	}
	
	@GetMapping(value = "/userTeams/search", params = "userId")
	public ResponseEntity<Team> getUserTeam(@RequestParam(required = true)String userId,
										 @RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		Optional<Team> userTeam = userTeamService.getUserTeamById(userId, teamId);
		if(userTeam == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Team>(userTeam.get(), HttpStatus.OK);
	}
	
	@PostMapping(value = "/userTeams/exit")
	@PreAuthorize("hasRole('PLAYER') or hasRole('TEAM_ADMIN')")
	public ResponseEntity<MessageResponse> exitTeam(@RequestParam(required = true)String userId,
									  @RequestBody(required = true)String teamId) throws InterruptedException, ExecutionException{
		String response = userTeamService.exitTeam(userId, teamId);
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	@GetMapping(value = "/userTeams/search", params="userName")
	public ResponseEntity<Team> getUserTeamByName(@RequestParam(required = true)String userName,
											   @RequestParam(required = true)String teamId) throws InterruptedException, ExecutionException{
		Optional<Team> userTeam = userTeamService.getUserTeamByName(userName, teamId);
		if(userTeam == null) {
			return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Team>(userTeam.get(), HttpStatus.OK);
	}
	
	@PostMapping(value = "/userTeamRequests/accept")
	@PreAuthorize("hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> acceptUserTeamRequest(@RequestBody(required = true)TeamInviteRequest request) throws InterruptedException, ExecutionException{
		String response = userTeamService.acceptTeamInvite(request);
		switch(response) {
			case "Not found.":
				return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
			case "User is already a member of this team.":
				return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.BAD_REQUEST);
			default:
				return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
		}
	}
	
	@PostMapping(value = "/userTeamRequests/decline")
	@PreAuthorize("hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> declineUserTeamRequest(@RequestBody(required = true)TeamInviteRequest request){
		String response = userTeamService.declineTeamInvite(request);
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
	
}
