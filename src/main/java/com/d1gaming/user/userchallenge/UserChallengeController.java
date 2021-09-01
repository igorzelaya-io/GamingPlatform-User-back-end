package com.d1gaming.user.userchallenge;

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

import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.request.UserChallengeRequest;
import com.d1gaming.library.response.MessageResponse;

@RestController
@PreAuthorize("permitAll()")
@CrossOrigin(origins = "http://34.122.97.231")
@RequestMapping(value = "/userchallengesapi")
public class UserChallengeController {

	@Autowired
	private UserChallengeService userChallengesService;
	
	@GetMapping(value="/userChallenges/challenges")
	public ResponseEntity<List<Challenge>> getAllUserChallenges(@RequestParam(required = true)String userId) throws InterruptedException, ExecutionException{
		List<Challenge> challengeList = userChallengesService.getAllUserChallenges(userId);
		if(challengeList.isEmpty()) {
 			return new ResponseEntity<List<Challenge>>(challengeList, HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<List<Challenge>>(challengeList, HttpStatus.OK);
	}
	
	@PostMapping(value="/userChallenges/add")
	@PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
	public ResponseEntity<MessageResponse> addChallengeToTeamCodChallengeList(@RequestBody(required = true)UserChallengeRequest userChallengeRequest) throws InterruptedException, ExecutionException{
		String response = userChallengesService.addChallengeToTeamCodChallengeList(userChallengeRequest.getUserId(), userChallengeRequest.getTeam(), userChallengeRequest.getChallenge());
		if(response.equals("Not found.")) {
			return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<MessageResponse>(new MessageResponse(response), HttpStatus.OK);		
	}
	
}
