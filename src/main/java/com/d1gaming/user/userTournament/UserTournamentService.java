package com.d1gaming.user.userTournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
		Tournament tournamentOnDB = tournamentSnapshot.toObject(Tournament.class);
		if(tournamentSnapshot.exists() && tournamentOnDB.getTournamentStatus().equals(TournamentStatus.ACTIVE) 
											|| tournamentOnDB.getTournamentStatus().equals(TournamentStatus.TERMINATED)
											|| tournamentOnDB.getTournamentStatus().equals(TournamentStatus.IN_PROGRESS)) {
			return true;
		}
		return false;
	}
	
	public Optional<Tournament> getTournamentById(String tournamentId) throws InterruptedException, ExecutionException{
		if(isActiveTournament(tournamentId)) {
			DocumentReference reference = firestore.collection("tournaments").document(tournamentId);
			DocumentSnapshot snapshot = reference.get().get();
			return Optional.of(snapshot.toObject(Tournament.class));
		}
		return null;
	}
	
	public List<Tournament> getAllTournamentsById(List<String> tournamentIds) throws InterruptedException, ExecutionException{
		List<Tournament> tournamentList = new ArrayList<>();
		for(String tournamentId: tournamentIds) {
			Tournament tournament = getTournamentById(tournamentId).get();
			if(tournament != null) {
				tournamentList.add(tournament);
			}
		}
		return tournamentList;
	}
	
	public List<Tournament> getAllTournamentsFromUser(String userId) throws InterruptedException, ExecutionException{
		if(isActive(userId)) {
			List<UserTournament> userTournaments = getUserReference(userId).get().get().toObject(User.class).getUserTournaments()
															.stream()
															.filter(userTournament -> userTournament.getUserTournamentStatus().equals(TeamTournamentStatus.ACTIVE))
															.collect(Collectors.toList());
			
			return getAllTournamentsById(userTournaments
											.stream()
											.map(userTournament -> userTournament.getUserTournamentId())
											.collect(Collectors.toList())
										);
					
		}
		return new ArrayList<>();
	}
		
	public String addTournamentToUserTournamentList(String userId, Team team, Tournament tournament) throws InterruptedException, ExecutionException {
		if(isActive(userId)) {
			DocumentReference userReference = getUserReference(userId);
			List<Match> userTournamentMatches = new ArrayList<>();
			UserTournament userTournament = new UserTournament(tournament.getTournamentId(), team, 0, 0, userTournamentMatches, TeamTournamentStatus.ACTIVE );
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
}

