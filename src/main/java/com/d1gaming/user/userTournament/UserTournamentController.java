package com.d1gaming.user.userTournament;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
	
}
