package com.d1gaming.user.userTournament;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.team.Team;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.library.user.UserTournament;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

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
	
	public String addTournamentToUserTournamentList(User user, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
			UserTournament userTournament = new UserTournament(tournament.getTournamentId(),tournament, team, 0, 0 );
			WriteResult result = getTournamentsSubcollectionFromUser(user.getUserId()).document(tournament.getTournamentId()).set(userTournament).get();
			System.out.println("Update Time: " + result.getUpdateTime());
			return "Tournament added to user list.";
		}
		return "Not Found.";
	}
	
	public String deleteTournamentFromUserTournamentList(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
			WriteResult resultFromDeletion = getTournamentsSubcollectionFromUser(user.getUserId()).document(tournament.getTournamentId()).delete().get();
			System.out.println("Update Time: " + resultFromDeletion.getUpdateTime());
			return "Tournament deleted from user list.";
		}
		return "Not found.";
	}
	
	public String addWinToUserTournaments(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
			DocumentReference tournamentSubDocumentReference = getTournamentsSubcollectionFromUser(user.getUserId()).document(tournament.getTournamentId());
			DocumentReference userReference = getUserReference(user.getUserId());
			WriteBatch batch = firestore.batch();
			batch.update(tournamentSubDocumentReference, "userTournamentMatchesWins", FieldValue.increment(1));
			batch.update(userReference, "userTotalWs", FieldValue.increment(1));
			batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Added W to record.";
		}
		return "Not found.";
	}
	
	public String addLossToUserTournament(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
			DocumentReference tournamentSubDocumentReference = getTournamentsSubcollectionFromUser(user.getUserId()).document(tournament.getTournamentId());
			DocumentReference userReference = getUserReference(user.getUserId());
			WriteBatch batch = firestore.batch();
			batch.update(tournamentSubDocumentReference, "userTournamentMatchesLosses", FieldValue.increment(-1));
			batch.update(userReference, "userTotalLs", FieldValue.increment(-1));
			batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Added L to record.";
		}
		return "Not found.";
	}
	
}

