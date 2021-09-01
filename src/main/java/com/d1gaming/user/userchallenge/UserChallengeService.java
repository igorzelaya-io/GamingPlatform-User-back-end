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
import com.d1gaming.library.team.TeamCodChallenge;
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
		Challenge challengeOnDB = challengeSnapshot.toObject(Challenge.class);
		if(challengeSnapshot.exists() && (challengeOnDB.getChallengeStatus().equals(ChallengeStatus.ACTIVE) 
											|| challengeOnDB.getChallengeStatus().equals(ChallengeStatus.IN_PROGRESS)
											|| challengeOnDB.getChallengeStatus().equals(ChallengeStatus.TERMINATED))) {
			return true;
		}
		return false; 
	}
	
	public Challenge getChallengeById(String challengeId) throws InterruptedException, ExecutionException {
		//If document does not exist, return null.
		if(isActiveChallenge(challengeId)) {
			DocumentReference reference = firestore.collection("challenges").document(challengeId);
			return reference.get().get().toObject(Challenge.class);
		}
		return null;
	}
	
	public List<Challenge> getAllChallengesById(List<String> challengesId) throws InterruptedException, ExecutionException{
		List<Challenge> challengeList = new ArrayList<>();
		for(String challengeId: challengesId) {
			Challenge retrievedChallenge = getChallengeById(challengeId);
			if(retrievedChallenge != null) {
				challengeList.add(retrievedChallenge);
			}
		}
		return challengeList;
	}
	
	public List<Challenge> getAllUserChallenges(String userId) throws InterruptedException, ExecutionException{
		if(isActiveUser(userId)) {
			DocumentReference userReference = getUsersCollection().document(userId);
			DocumentSnapshot userSnapshot = userReference.get().get();
			User userOnDB = userSnapshot.toObject(User.class);
			List<UserChallenge> userChallengesList = userOnDB.getUserChallenges();
			List<String> userChallengesId = userChallengesList
												.stream()
												.map(userChallenge -> userChallenge.getUserChallengeId())
												.collect(Collectors.toList());
			return getAllChallengesById(userChallengesId);
			
		}
		return new ArrayList<>();
	}
	
	public String addChallengeToTeamCodChallengeList(String userId, Team team, Challenge challenge) throws InterruptedException, ExecutionException {
		if(isActiveUser(userId) && isActiveChallenge(challenge.getChallengeId())) {
			DocumentReference teamReference = firestore.collection("teams").document(team.getTeamId());
			DocumentReference challengeReference = firestore.collection("challenges").document(challenge.getChallengeId());
			Team teamOnDB = teamReference.get().get().toObject(Team.class);
			Challenge challengeOnDB = challengeReference.get().get().toObject(Challenge.class);
			//challengeOnDB.setChallengeHostTeam(teamOnDB);
			List<Match> userMatches = new ArrayList<>();
			TeamCodChallenge teamCodChallengeSubdocument = new TeamCodChallenge(challengeOnDB.getChallengeId(), userMatches, 0, 0, 0, 0, 0, 0, TeamTournamentStatus.ACTIVE);
			teamCodChallengeSubdocument.setTeamChallengeTeamId(team.getTeamId());
			DocumentReference addedDocumentToTeamCodChallengesSubcollection = firestore.collection("teams").document(team.getTeamId()).collection("teamCodChallenges").add(teamCodChallengeSubdocument).get();
			String documentId = addedDocumentToTeamCodChallengesSubcollection.getId();
			List<User> hostTeamUsers = teamOnDB.getTeamUsers();
			WriteBatch batch = firestore.batch();
			batch.update(addedDocumentToTeamCodChallengesSubcollection, "teamCodChallengeId", documentId);
			batch.update(challengeReference, "challengeHostTeam", teamOnDB);
			batch.commit().get();
			hostTeamUsers.forEach(teamUser -> {
				try {
					addChallengeToUserChallengeList(teamUser, teamOnDB, challengeOnDB);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			});
			return "Challenge added successfully to team.";
		}
		return "Not found.";
	}
	
	public void addChallengeToUserChallengeList(User user, Team team, Challenge challenge) throws InterruptedException, ExecutionException {		
		DocumentReference userReference = firestore.collection("users").document(user.getUserId());
		User userOnDB = userReference.get().get().toObject(User.class);
		List<Match> challengeMatches = new ArrayList<>();
		UserChallenge userChallenge = new UserChallenge(challenge.getChallengeId(), team, challengeMatches, 0, 0, TeamTournamentStatus.ACTIVE);
		List<UserChallenge> userChallenges = userOnDB.getUserChallenges();
		userChallenges.add(userChallenge);
		WriteBatch batch = firestore.batch();
		batch.update(userReference, "userChallenges", userChallenges);
		batch.commit().get();
	}
	
}
