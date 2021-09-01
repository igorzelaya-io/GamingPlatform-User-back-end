package com.d1gaming.user.userTournament;

import java.util.List;
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

import com.d1gaming.library.request.UserTournamentRequest;
import com.d1gaming.library.response.MessageResponse;
import com.d1gaming.library.tournament.Tournament;

@RestController
@CrossOrigin(origins = "localhost:4200")
@PreAuthorize("permitAll()")
@RequestMapping(value = "/usertournamentsapi")
public class UserTournamentController {

	@Autowired
	private UserTournamentService userTournamentService; 
	
	@GetMapping(value = "/userTournaments")
	public ResponseEntity<List<Tournament>> getAllTournamentsFromUser(@RequestParam(required = true)String userId) throws InterruptedException, ExecutionException{
		List<Tournament> userTournaments = userTournamentService.getAllTournamentsFromUser(userId);
		if(userTournaments.isEmpty()) {
			return new ResponseEntity<List<Tournament>>(userTournaments, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Tournament>>(userTournaments, HttpStatus.OK);
	}
	
	@PostMapping(value = "/userTournaments/add")
	@PreAuthorize("hasRole('PLAYER') or hasRole('ADMIN')")
	public ResponseEntity<MessageResponse> addTournamentToUserTournamentList(@RequestBody(required = true)UserTournamentRequest userTournamentRequest) throws InterruptedException, ExecutionException{
		String response = userTournamentService.addTournamentToUserTournamentList(userTournamentRequest.getUserId(),userTournamentRequest.getTeam(), userTournamentRequest.getTournament());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);
	}
	
}
