package com.d1gaming.user.userteam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.team.Team;
import com.d1gaming.library.team.TeamInviteRequest;
import com.d1gaming.library.team.TeamInviteRequestStatus;
import com.d1gaming.library.team.TeamStatus;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

@Service
public class UserTeamService {

	private final String USERS_COLLECTION = "users";
	private final String TEAMS_COLLECTION = "teams";

	@Autowired
	private Firestore firestore;
	
	private CollectionReference getUsersCollection() {
		return firestore.collection(USERS_COLLECTION);
	}
	
	private CollectionReference getTeamsCollection() {
		return firestore.collection(TEAMS_COLLECTION);
	}
	
	private DocumentReference getUserReference(String userId) {
		return getUsersCollection().document(userId);
	}
	
	private DocumentReference getTeamReference(String teamId) {
		return getTeamsCollection().document(teamId); 
	}
	
	private boolean isActiveUser(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = getUserReference(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists() && userSnapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return true;
		}
		return false; 
	}
	
	private boolean isActiveTeam(String teamId) throws InterruptedException, ExecutionException {
		DocumentReference teamReference = getTeamReference(teamId);
		DocumentSnapshot teamSnapshot = teamReference.get().get();
		if(teamSnapshot.exists() && teamSnapshot.toObject(Team.class).getTeamStatus().equals(TeamStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
	//Get a User's TEAM by its teamId.
	public Optional<Team> getUserTeamById(String userId, String teamId) throws InterruptedException, ExecutionException {
		if(isActiveTeam(teamId) && isActiveUser(userId)) {
			DocumentReference userReference = getUserReference(userId);
			DocumentSnapshot userSnapshot = userReference.get().get();
			DocumentReference teamReference = getTeamReference(teamId);
			Team userTeam = teamReference.get().get().toObject(Team.class);
			List<Team> userTeams = userSnapshot.toObject(User.class).getUserTeams();
			if(userTeams.contains(userTeam)) {
				int teamIndex = userTeams.indexOf(userTeam);
				return Optional.of(userTeams.get(teamIndex));
			}
			return null;
		}
		return null;
	}
	
	//Get a User's TEAM by its teamName.
	public Optional<Team> getUserTeamByName(String userId, String teamName) throws InterruptedException, ExecutionException{
		Query query = getTeamsCollection().whereEqualTo("teamName", teamName);
		QuerySnapshot querySnapshot = query.get().get();
		if(isActiveUser(userId) && !querySnapshot.isEmpty()) {
			Team team = query.get().get().toObjects(Team.class).get(0);			
			DocumentReference userReference = getUserReference(userId);
			DocumentSnapshot userSnapshot = userReference.get().get();
			List<Team> userTeams = userSnapshot.toObject(User.class).getUserTeams();
			if(userTeams.contains(team)) {
				int teamIndex = userTeams.indexOf(team);
				return Optional.of(userTeams.get(teamIndex));
			}
			return null;
		}
		return null;
	}
	
	//Remove player from team player List, same for userTeamList.
	public String exitTeam(String userId, String teamId) throws InterruptedException, ExecutionException {
		if(isActiveUser(userId) || isActiveTeam(teamId)) {
			DocumentReference userReference = getUsersCollection().document(userId);
			DocumentReference teamReference = getTeamsCollection().document(teamId);
			User user = userReference.get().get().toObject(User.class);
			Team userTeam = teamReference.get().get().toObject(Team.class);
			List<User> teamUsers = userTeam.getTeamUsers();
			List<Team> userTeams = user.getUserTeams();
			if(teamUsers.contains(user) && userTeams.contains(userTeam)) {
				int teamIndex = userTeams.indexOf(userTeam);
				int userIndex = teamUsers.indexOf(user);
				teamUsers.remove(userIndex);
				userTeams.remove(teamIndex);
			}
			WriteBatch batch = firestore.batch();
			batch.update(teamReference, "teamUsers",teamUsers);
			batch.update(userReference, "userTeams", userTeams);
			batch.commit().get()
				.forEach(result -> 	System.out.println("Update Time: " + result.getUpdateTime()));
			return "Team exited successfully.";
		}
		return "Not found.";
	}
	
	public List<TeamInviteRequest> getAllTeamRequests(String userId) throws InterruptedException, ExecutionException{
		if(isActiveUser(userId)) {
			DocumentReference reference = getUsersCollection().document(userId);
			return reference.get().get()
					.toObject(User.class).getUserTeamRequests()
					.stream()
//					.filter(invite -> invite.getRequestStatus().equals(TeamInviteRequestStatus.PENDING))
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public List<Team> getAllUserTeams(String userId) throws InterruptedException, ExecutionException{
		if(isActiveUser(userId)) {
			DocumentReference userReference = getUserReference(userId);
			return userReference.get().get().toObject(User.class).getUserTeams()
					.stream()
					.filter(team -> team.getTeamStatus().equals(TeamStatus.ACTIVE))
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public String acceptTeamInvite(TeamInviteRequest teamInviteRequest) throws InterruptedException, ExecutionException {
		if(isActiveUser(teamInviteRequest.getRequestedUser().getUserId()) && isActiveTeam(teamInviteRequest.getTeamRequest().getTeamId())) {
			DocumentReference userReference = getUserReference(teamInviteRequest.getRequestedUser().getUserId());
			DocumentReference teamReference = getTeamReference(teamInviteRequest.getTeamRequest().getTeamId());
			User user = userReference.get().get().toObject(User.class);
			Team team = teamReference.get().get().toObject(Team.class);
			List<User> teamUsers = team.getTeamUsers();
			List<Team> userTeams = user.getUserTeams();
			
			boolean containsUser = teamUsers
						.stream()
						.anyMatch(usr -> usr.getUserName().equals(user.getUserName()));
			
			boolean containsTeam = userTeams
						.stream()
						.anyMatch(userTeam -> userTeam.getTeamName().equals(team.getTeamName()));
			
			if(containsUser && containsTeam) {
				List<TeamInviteRequest> teamInviteRequestList = team.getTeamRequests()
						.stream()
						.filter(invite -> invite.getRequestedUser().getUserId().equals(user.getUserId()))
						.peek(invite -> invite.setRequestStatus(TeamInviteRequestStatus.INVALID))
						.collect(Collectors.toList());
				List<TeamInviteRequest> userInviteRequestList = user.getUserTeamRequests()
						.stream()
						.filter(invite -> invite.getTeamRequest().getTeamId().equals(team.getTeamId()))
						.peek(invite -> invite.setRequestStatus(TeamInviteRequestStatus.INVALID))
						.collect(Collectors.toList());
				WriteBatch batch = firestore.batch();
				batch.update(teamReference, "teamRequests", teamInviteRequestList);
				batch.update(userReference, "userTeamRequests", userInviteRequestList);
				batch.commit().get();
				return "User is already a member of this team.";
			}
			WriteBatch batch = firestore.batch();
			team.setTeamUsers(null);
			user.setUserTeams(null);
			userTeams.add(team);
			teamUsers.add(user);
			
			List<TeamInviteRequest> teamInviteRequestList = team.getTeamRequests()
									.stream()
									.filter(invite -> invite.getRequestedUser().getUserId().equals(user.getUserId()))
									.peek(invite -> invite.setRequestStatus(TeamInviteRequestStatus.ACCEPTED))
									.collect(Collectors.toList());
			List<TeamInviteRequest> userInviteRequestList = user.getUserTeamRequests()
					.stream()
					.filter(invite -> invite.getTeamRequest().getTeamId().equals(team.getTeamId()))
					.peek(invite -> invite.setRequestStatus(TeamInviteRequestStatus.ACCEPTED))
					.collect(Collectors.toList());
			
			batch.update(teamReference, "teamRequests", teamInviteRequestList);
			batch.update(userReference, "userTeamRequests", userInviteRequestList);
			batch.update(teamReference, "teamUsers", teamUsers);
			batch.update(userReference, "userTeams", userTeams);
			batch.commit().get();
			return "Invite accepted successfully.";
		}
		return "Not found.";
	}
	
	public String declineTeamInvite(TeamInviteRequest teamInviteRequest) throws InterruptedException, ExecutionException {
		DocumentReference teamReference = getTeamReference(teamInviteRequest.getTeamRequest().getTeamId());
		DocumentReference userReference = getUserReference(teamInviteRequest.getRequestedUser().getUserId());
		Team team = teamInviteRequest.getTeamRequest();
		User user = teamInviteRequest.getRequestedUser();
		List<TeamInviteRequest> teamInvites = team.getTeamRequests()
								.stream()
								.filter(invite -> invite.getRequestedUser().getUserId().equals(user.getUserId()))
								.peek(invite -> invite.setRequestStatus(TeamInviteRequestStatus.DECLINED))
								.collect(Collectors.toList());
		List<TeamInviteRequest> userInviteRequestList = user.getUserTeamRequests()
				.stream()
				.filter(invite -> invite.getTeamRequest().getTeamId().equals(team.getTeamId()))
				.peek(invite -> invite.setRequestStatus(TeamInviteRequestStatus.DECLINED))
				.collect(Collectors.toList());
		
		WriteBatch batch = firestore.batch();
		batch.update(teamReference, "teamRequests", teamInvites);
		batch.update(userReference, "userTeamRequests", userInviteRequestList);
		batch.commit().get();
		return "Invite declined.";
	}
	
	public String deleteTeamInvite(TeamInviteRequest teamInviteRequest) throws InterruptedException, ExecutionException {
		DocumentReference userReference = getUserReference(teamInviteRequest.getRequestedUser().getUserId());
		User user = teamInviteRequest.getRequestedUser();
		WriteBatch batch = firestore.batch();
		List<TeamInviteRequest> newUserInviteRequestList = user.getUserTeamRequests()
				.stream()
				.filter(invite -> !invite.getRequestedUser().getUserId().equals(user.getUserId()))
				.collect(Collectors.toList());
		batch.update(userReference, "userTeamRequests", newUserInviteRequestList);
		batch.commit().get();
		return "Invite deleted.";
	}
	
}