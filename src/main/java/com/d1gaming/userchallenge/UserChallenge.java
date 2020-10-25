package com.d1gaming.userchallenge;

import java.util.Map;

import com.d1gaming.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class UserChallenge {
	UserChallenge(){
		
	}
	
	@JsonProperty
	private Map<String,Object> userChallengeDate;
	
	@JsonProperty
	private String userChallengeId;

	@JsonProperty
	private String userChallengeStatus;

	@JsonProperty
	private String userChallengeTime;
	
	@JsonProperty
	private User userChallengeOpponent;


}
