package com.d1gaming.user.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.user.firebaseconfig.UserFirestoreUtils;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

@Service
public class UserService {
	
	public final String USERS_COLLECTION = "users";
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	private Firestore firestore = UserFirestoreUtils.getFirestore();
	
	private CollectionReference getUsersCollection() {
		return firestore.collection(this.USERS_COLLECTION);
	}
	
	
	//Post a user onto the user collection. documentID is auto-generated by firestore.
	public String saveUser(User user) throws InterruptedException, ExecutionException {
		Query query = getUsersCollection().whereEqualTo("userName", user.getUserName());
		QuerySnapshot querySnapshot = query.get().get();
		//Query to validate if userName is already in use.
		if(querySnapshot.isEmpty()) {
			ApiFuture<DocumentReference> document = getUsersCollection().add(user);
			DocumentReference reference = document.get();
			String userId = document.get().getId();
			//Assign auto-generated Id to userId field for ease of querying.
			WriteBatch batch = FirestoreClient.getFirestore().batch();
			batch.update(reference, "userId",userId);
			//ENCODE USER PASSWORD!
			batch.update(reference, "userPassword", passwordEncoder.encode(reference.get().get().toObject(User.class).getUserPassword()));
			List<WriteResult> results = batch.commit().get();
			results.forEach(result -> {
				System.out.println("Update Time: " + result.getUpdateTime());
			});
			return "Created user with ID: " + "'" + userId + "'";
		}
		return "Username is already in use";
	}
	//TODO: TEAM GET.
//	Get a User's TEAM.
//	public Team getUserTeam(String teamName) {
//		
//	}
	
	//Get User by given its email.
	public Optional<User> getUserByEmail(String userEmail) throws InterruptedException, ExecutionException{
		Query query = getUsersCollection().whereEqualTo("userEmail", userEmail);
		QuerySnapshot snapshot = query.get().get();
		//if user with provided Email exists in collection.
		if(!snapshot.isEmpty()) {
			List<User> userLs = snapshot.toObjects(User.class);
			//Since there is a unique email for each document,
			//There will only be on User object on list, we will retrieve the first one.
			for(User currUser: userLs) {
				return Optional.of(currUser);
			}
		}
		return null;
	}
	
	//Query for user by given userName. 
	public Optional<User> getUserByUserName(String userName) throws InterruptedException, ExecutionException{
		//Perform a query based on a user's Name.
		Query query = getUsersCollection().whereEqualTo("userName", userName);
		QuerySnapshot snapshot = query.get().get();
		if(!snapshot.isEmpty()) {
			List<User> userList = snapshot.toObjects(User.class);
			//Since there is a unique userName for each document,
			//there will only be one User object on the list, we will retrieve the first one.
			for(User currUser: userList) {
				return Optional.of(currUser);
			}
		}
		return null;
	}

	//Query for user by given userName. 
	public User getUserByName(String userName) throws InterruptedException, ExecutionException {
		//Perform a query based on a user's Name.
		Query query = getUsersCollection().whereEqualTo("userName", userName);
		QuerySnapshot snapshot = query.get().get();
		if(!snapshot.isEmpty()) {
			List<User> userList = snapshot.toObjects(User.class);
			//Since there is a unique userName for each document,
			//there will only be one User object on the list, we will retrieve the first one.
			for(User currUser : userList) {
				return currUser;
			}
		}
		return null;
	}
	
	
	//Get User by its auto-generated ID.
	public User getUserById(String userId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getUsersCollection().document(userId);
		if(reference.get().get().exists()) {
			DocumentSnapshot snapshot = reference.get().get();
			User user = snapshot.toObject(User.class);
			return user;
		}
		return null;
	}
	
	//Get DocumentReference on a User.
	public DocumentReference getUserReference(String userId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getUsersCollection().document(userId);
		//Evaluate if documentExists in users collection.
		if(reference.get().get().exists()) {
			return reference;
		}
		return null;
	}
	
	//return a list of objects located in the users collection.
	public List<User> getAllUsers() throws InterruptedException, ExecutionException {
		//asynchronously retrieve all documents
		ApiFuture<QuerySnapshot> future = getUsersCollection().get();
		List<QueryDocumentSnapshot> objects = future.get().getDocuments();
		//If there is no documents, return null.
		if(!objects.isEmpty()) {
			List<User> ls = new ArrayList<>();		
			objects.forEach((obj) -> {
				User currUser = obj.toObject(User.class);
				ls.add(currUser);
			});
			return ls;
		}	
		return null;
	}
	
	
	//delete a User from users collection by a given id.
	//In reality delete method just changes UserStatus from active to inactive or banned.
	public String deleteUserById(String userId) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreClient.getFirestore();
		DocumentReference reference = db.collection(USERS_COLLECTION).document(userId);
		User user = reference.get().get().toObject(User.class);
		if(user == null) {
			return "User not found.";
		}
		WriteBatch batch = db.batch();
		batch.update(reference, "userStatusCode",UserStatus.INACTIVE);
		ApiFuture<List<WriteResult>> result = batch.commit();
		List<WriteResult> results = result.get();
		results.forEach(response -> {
			System.out.println("Update Time:" + response.getUpdateTime());
		});
		//Check if user did actually change status.
		if(reference.get().get().toObject(User.class).getStatusCode().equals(UserStatus.ACTIVE)) {
			return "User with ID: " + "'" +  userId + "'" + " was deleted.";
		}
		return "User could not be deleted";
	}
	
	//Delete a User's certain field value.
	public String deleteUserField(String userId, String userField) throws InterruptedException, ExecutionException {
		Firestore firestore = FirestoreClient.getFirestore();
		DocumentReference reference = getUsersCollection().document(userId);
		if(!reference.get().get().exists()) {
			return "User not found.";
		}
		Map<String,Object> map = new HashMap<>();
		map.put(userField, FieldValue.delete());
		WriteBatch batch = firestore.batch();
		batch.update(reference, map);
		List<WriteResult> results = batch.commit().get();
		results.forEach(response -> System.out.println("Update Time: " + response.getUpdateTime()));
		return "Field deleted Successfully";	
	}
	
	//Change UserStatus to BANNED
	public String banUserById(String userId) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreClient.getFirestore();
		final DocumentReference reference = db.collection(this.USERS_COLLECTION).document(userId);
		WriteBatch batch = db.batch().update(reference,"userStatusCode",UserStatus.BANNED);
		List<WriteResult> results = batch.commit().get();
		results.forEach(response -> System.out.println("Update Time: " + response.getUpdateTime()));
		if(reference.get().get().toObject(User.class).getStatusCode().equals(UserStatus.BANNED)) {
			return "User with ID: " + "'" + userId + "'"  + " was BANNED.";
		}
		return "User could not be BANNED.";
	}
	
	//Set a user with all new fields. 
	public String updateUser(User user) throws InterruptedException, ExecutionException {
		Firestore firestore = FirestoreClient.getFirestore();
		final DocumentReference reference = getUsersCollection().document(user.getUserId());
		DocumentSnapshot snapshot = reference.get().get();
		if(snapshot.exists()) {	
			WriteBatch batch = firestore.batch();	
			batch.set(reference, user);
			List<WriteResult> results = batch.commit().get();
			results.forEach(response -> System.out.println("Update time: " + response.getUpdateTime()));
			return "User updated successfully";
		}
		return "User not found.";
	}
	

	// Update a specific field on a given document by another given value. In case userId is field to be changed, one integer will be subtracted from userTokens field.
	public String updateUserField(String userId,String objectField, String replaceValue) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreClient.getFirestore();
		final DocumentReference reference = getUsersCollection().document(userId);
		if(!reference.get().get().exists()) {
			return "User not found.";
		}
		WriteBatch batch = db.batch();
		List<WriteResult> results = new ArrayList<>();
		//These fields cannot be updated.
		if(!objectField.equals("userName") && !objectField.equals("userCash") && !objectField.equals("userTokens") && !objectField.equals("userId")) {
			batch.update(reference, objectField, replaceValue);
			results = batch.commit().get();
			results.forEach(response ->{
				System.out.println("Update time: " + response.getUpdateTime());	
			});
		}
		else if(objectField.equals("userName")) {
			String response = updateUserName(userId, replaceValue);
			return response;
		}
		else {
			return "This field canntot be updated.";
		}
		return "User field could not be updated.";
	}
	
		
	//Update a user's userName depending of availability and token adquisition capacity. i.e. if user has enough tokens to pay fee.
	public String updateUserName(String userId, String newUserName) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreClient.getFirestore();
		final DocumentReference reference = getUsersCollection().document(userId);
		DocumentSnapshot snapshot = reference.get().get();
		if(!snapshot.exists()) {
			return "User not found.";
		}
		Query query = getUsersCollection().whereEqualTo("userName", newUserName);
		QuerySnapshot querySnapshot = query.get().get();
		//Evaluate if userName is already in use.
		String response = "Username is already taken";
		if(querySnapshot.isEmpty()) {
				//Transaction to get() tokens and update() tokens.
				ApiFuture<String> futureTransact = db.runTransaction(transaction -> {
					DocumentSnapshot doc = transaction.get(reference).get();
					double tokens = doc.getDouble("userTokens");
					//evaluate if user holds more than one token
					if(tokens >= 1) {
						transaction.update(reference, "userTokens", tokens - 1);
						transaction.update(reference, "userName", newUserName);
						return "Username updated to: '"+ newUserName +"'";
					}
					else {
						throw new Exception("Not enough Tokens");
					}
				});
				response = futureTransact.get();
		}
		return response;
	}
	
	//update user Currency field.
	public String updateUserCash(String userId, double cashQuantity) throws InterruptedException, ExecutionException {
		Firestore firestore = FirestoreClient.getFirestore();
		final DocumentReference reference = getUsersCollection().document(userId);
		DocumentSnapshot snapshot = reference.get().get();
		String response = "User not found.";
		//evaluate if document exists 
		if(snapshot.exists()) {
			ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
				Map<String,Object> map = new HashMap<>();
				map.put("userCash", FieldValue.increment(cashQuantity));
				transaction.update(reference, map);
				return "Updated userCash";
			});
			response = futureTransaction.get();
			return response;
		}
		return response;
	}
	
	//Update user Token field.
	public String updateUserTokens(String userId, double tokenQuantity) throws InterruptedException, ExecutionException {
		Firestore firestore = FirestoreClient.getFirestore();
		final DocumentReference reference = getUsersCollection().document(userId);
		String response = "User not found.";
		//evaluate if user exists on collection.
		if(reference.get().get().exists()) {
			ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
				Map<String,Object> map = new HashMap<>();
				map.put("userTokens",tokenQuantity);
				transaction.update(reference, map);
				return "Updated userTokens";
			});
			response = futureTransaction.get();
			return response;
		}
		return response;
	}
	
	
	//evaluate if given documentId exists on given collection.
	public static boolean isPresent(String userId,String collectionName) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreClient.getFirestore();
		DocumentReference reference = db.collection(collectionName).document(userId);
		ApiFuture<DocumentSnapshot> snapshot = reference.get();
		DocumentSnapshot document = snapshot.get();
		if(!document.exists()) {
			return false;
		}
		return true;
	}

	//Evaluate if given document's status corresponds to active.
	public static boolean isActive(String userId, String collectionName) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreClient.getFirestore();
		DocumentReference reference = db.collection(collectionName).document(userId);
		ApiFuture<DocumentSnapshot> snapshot = reference.get();
		DocumentSnapshot result = snapshot.get();
		User user = result.toObject(User.class);	
		if(user.getStatusCode().equals(UserStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
}
