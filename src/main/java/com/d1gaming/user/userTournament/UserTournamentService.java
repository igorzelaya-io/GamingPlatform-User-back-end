package com.d1gaming.user.userTournament;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.library.user.UserTournament;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

@Service
public class UserTournamentService {

	private final String USER_TOURNAMENT_SUBCOLLECTION = "userTournaments";
	
	private final String USER_COLLECTION = "users";
	
	@Autowired
	private Firestore firestore;
	
	private CollectionReference getUsersCollection() {
		return firestore.collection(USER_COLLECTION);
	}
	
	private CollectionReference getTournamentsSubcollectionFromUser(String userId) {
		return firestore.collection(USER_COLLECTION)
						.document(userId)
						.collection(USER_TOURNAMENT_SUBCOLLECTION);	
	}
	
	private DocumentReference getUserReference(String userId) {
		return getUsersCollection().document(userId);
	}
	
	private DocumentReference getUserTournamentReferenceFromUser(String userId, String tournamentId) {
		return getTournamentsSubcollectionFromUser(userId).document(tournamentId);
	}
	
	private DocumentReference getTournamentReferenceFromTournamentCollection(String tournamentId) {
		return firestore.collection("tournaments").document(tournamentId);
	}
	
	private boolean isActive(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = getUsersCollection().document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists() && userSnapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	private boolean isActiveTournament(String tournamentId) throws InterruptedException, ExecutionException {
		DocumentReference tournamentReference = firestore.collection("tournaments").document(tournamentId);
		DocumentSnapshot tournamentSnapshot = tournamentReference.get().get();
		if(tournamentSnapshot.exists() && tournamentSnapshot.toObject(Tournament.class).getTournamentStatus().equals(TournamentStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	public List<Tournament> getAllTournamentsFromUser(String userId) throws InterruptedException, ExecutionException{
		if(isActive(userId)) {
			ApiFuture<QuerySnapshot> queryForTournaments = getTournamentsSubcollectionFromUser(userId).get();
			return queryForTournaments.get().getDocuments()
											.stream()
											.map(document -> document.toObject(UserTournament.class))
										    .map(userTournament -> userTournament.getUserTournaments())
										    .filter( tournament -> {
												try {
													return isActive(tournament.getTournamentId());
												} catch (InterruptedException | ExecutionException e) { 
													e.printStackTrace();
												} 
												return false;
											} )
										    .collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
}