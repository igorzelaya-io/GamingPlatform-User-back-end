package com.d1gaming.user.userTournament;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.match.Match;
import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamTournamentStatus;
import com.d1gaming.library.tournament.Tournament;
import com.d1gaming.library.tournament.TournamentStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.library.user.UserTournament;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;

@Service
public class UserTournamentService {
	
	private final String USER_COLLECTION = "users";
	
	@Autowired
	private Firestore firestore;
	
	private CollectionReference getUsersCollection() {
		return firestore.collection(USER_COLLECTION);
	}
	
	private DocumentReference getUserReference(String userId) {
		return getUsersCollection().document(userId);
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
			List<UserTournament> userTournaments = getUserReference(userId).get().get().toObject(User.class).getUserTournaments()
															.stream()
															.filter(userTournament -> userTournament.getUserTournamentStatus().equals(TeamTournamentStatus.ACTIVE))
															.collect(Collectors.toList());
			return userTournaments
					.stream()
					.map(userTournament -> userTournament.getUserTournament())
					.filter(tournament -> {
						try {
							return isActiveTournament(tournament.getTournamentId());
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
						return false;
					})
					.collect(Collectors.toList());
					
		}
		return new ArrayList<>();
	}
	
	//TODO:
//	public List<Match> getAllActiveMatchesFromUser(User user, Tournament tournament) throws InterruptedException, ExecutionException{
//		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
//			
//		}
//	}
//	
//	public List<Match> getAllInactiveMatchesFromUser(User user, Tournament tournament){
//		
//	}
	
	public String addTournamentToUserTournamentList(User user, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId())) {
			DocumentReference userReference = getUserReference(user.getUserId());
			List<Match> userTournamentMatches = new ArrayList<>();
			UserTournament userTournament = new UserTournament(tournament, team, 0, 0, userTournamentMatches, TeamTournamentStatus.ACTIVE );
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournaments = userOnDB.getUserTournaments();
			userTournaments.add(userTournament);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userTournaments", userTournaments);
			batch.commit().get();
			return "Tournament added to user list.";
		}
		return "Not Found.";
	}
	
	public String deleteTournamentFromUserTournamentList(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
			DocumentReference userReference = getUserReference(user.getUserId());
			User userOnDB = userReference.get().get().toObject(User.class);
			List<UserTournament> userTournaments = userOnDB.getUserTournaments()
																.stream()
																.filter(userTournament -> userTournament.getUserTournamentStatus().equals(TeamTournamentStatus.ACTIVE))
																.filter(userTournament -> userTournament.getUserTournament().getTournamentId().equals(tournament.getTournamentId()))
																.collect(Collectors.toList());
			UserTournament userTournament = userTournaments.get(0);
			int indexOfTournament = userOnDB.getUserTournaments().indexOf(userTournament);
			if(indexOfTournament != -1) {
				userTournaments.remove(indexOfTournament);
				userTournament.setUserTournamentStatus(TeamTournamentStatus.INACTIVE);
				userTournaments.add(userTournament);
				WriteBatch batch = firestore.batch();
				batch.update(userReference, "userTournaments", userTournaments);
				batch.commit().get();
				return "Tournament deleted from user list.";
			}
		}
		return "Not found.";
	}
	
	//TODO: Add Win to UserTournament
	public String addWinToUserTournaments(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
			DocumentReference userReference = getUserReference(user.getUserId());
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userTotalWs", FieldValue.increment(1));
			batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Added W to record.";
		}
		return "Not found.";
	}
	
	//TODO: fix method.
	public String addLossToUserTournament(User user, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId()) && isActiveTournament(tournament.getTournamentId())) {
//			DocumentReference tournamentSubDocumentReference = getTournamentsSubcollectionFromUser(user.getUserId()).document(tournament.getTournamentId());
			DocumentReference userReference = getUserReference(user.getUserId());
			WriteBatch batch = firestore.batch();
			//batch.update(tournamentSubDocumentReference, "userTournamentMatchesLosses", FieldValue.increment(-1));
			batch.update(userReference, "userTotalLs", FieldValue.increment(-1));
			batch.commit().get()
					.stream()
					.forEach(result -> System.out.println("Update Time: " + result.getUpdateTime()));
			return "Added L to record.";
		}
		return "Not found.";
	}
	
}

