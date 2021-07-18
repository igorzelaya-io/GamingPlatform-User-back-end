package com.d1gaming.user.userchallenge;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.challenge.ChallengeStatus;
import com.d1gaming.library.match.Match;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserChallenge;
import com.d1gaming.library.user.UserStatus;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;

@Service
public class UserChallengeService {

	private final String USER_COLLECTION = "users";
	
	@Autowired
	private Firestore firestore;
	
	private CollectionReference getUsersCollection() {
		return firestore.collection(USER_COLLECTION);
	}
	
	private boolean isActiveUser(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists() && userSnapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	private boolean isActiveChallenge(String challengeId) throws InterruptedException, ExecutionException {
		DocumentReference challengeReference = firestore.collection("challenges").document(challengeId);
		DocumentSnapshot challengeSnapshot = challengeReference.get().get();
		if(challengeSnapshot.exists() && challengeSnapshot.toObject(Challenge.class).getChallengeStatus().equals(ChallengeStatus.ACTIVE)) {
			return true;
		}
		return false; 
	}
	
	public List<Challenge> getAllUserChallenges(String userId) throws InterruptedException, ExecutionException{
		if(isActiveUser(userId)) {
			DocumentReference userReference = getUsersCollection().document(userId);
			DocumentSnapshot userSnapshot = userReference.get().get();
			User userOnDB = userSnapshot.toObject(User.class);
			List<UserChallenge> userChallengesList = userOnDB.getUserChallenges();
			return userChallengesList.stream()
						   .map(userChallenge -> userChallenge.getUserChallenge())
						   .collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public String addChallengeToUserChallengeList(User user, Team team, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveUser(user.getUserId()) && isActiveChallenge(challenge.getChallengeId())) {
			DocumentReference userReference = firestore.collection("users").document(user.getUserId());
			List<Match> userMatches = new ArrayList<>();
			UserChallenge userChallenge = new UserChallenge(challenge, team, userMatches, 0, 0, TeamTournamentStatus.ACTIVE);
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserChallenge> userChallenges = userOnDB.getUserChallenges();
			userChallenges.add(userChallenge);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userChallenges", userChallenges);
			batch.commit().get();
			return "Challenge added to list";
			
		}
		return "Not found.";
	}
	
}
