package com.d1gaming.user.userchallenge;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.challenge.Challenge;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserChallenge;
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
	
	public List<Challenge> getAllUserChallenges(String userId) throws InterruptedException, ExecutionException{
		DocumentReference userReference = getUsersCollection().document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists()) {
			User userOnDB = userSnapshot.toObject(User.class);
			List<UserChallenge> userChallengesList = userOnDB.getUserChallenges();
			List<Challenge> challengeList = null;
			try {
				challengeList = userChallengesList.stream()
						   .map(userChallenge -> userChallenge.getUserChallenge())
						   .collect(Collectors.toList());
			}
			catch(NullPointerException e) {
				return new ArrayList<>();
			}
			return challengeList;
		}
		return new ArrayList<>();
	}
	
	//public Optional<Challenge> getChallengeById(String challengeId) throws InterruptedException, ExecutionException{
		
	//}
	
	//TODO:
//	public String postChallenge(Challenge challenge, User user) throws InterruptedException, ExecutionException {
//		//Add a document onto challenges subcollection with given body and auto-generated ID.
//		DocumentReference reference = getUserChallengesCollection().add(challenge).get();
//		String challengeId = reference.getId();
//		WriteBatch batch = firestore.batch();
//		//Assign auto-generated id to challengeId field.
//		batch.update(reference, "challengeId", challengeId);
//		//Assign given user to challengeUserAdmin 
//		batch.update(reference, "challengeUserAdmin", user);
//		return " ";	
//	
//	}
	
	public String joinChallenge(Challenge challenge) {
		return " ";
	}

	//Add challenge moderator role to user.
	private String addChallengeModeratorRole(User user) throws InterruptedException, ExecutionException {
		String userId = user.getUserId();
		DocumentReference reference = firestore.collection("users").document(userId);
		//Validating if user exists before making changes.
		if(!reference.get().get().exists()) {
			return "User not found.";
		}
		WriteBatch batch = firestore.batch();
		//batch.update(reference, "", value, moreFieldsAndValues)
		//TODO
		return " ";
	}
}
