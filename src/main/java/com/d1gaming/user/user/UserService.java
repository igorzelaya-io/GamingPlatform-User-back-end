package com.d1gaming.user.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.service.D1Service;
import com.d1gaming.library.transaction.D1Transaction;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserDetailsImpl;
import com.d1gaming.library.user.UserStatus;
import com.d1gaming.user.image.UserImageService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.Query.Direction;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

@Service
public class UserService {
	
	public final String USERS_COLLECTION = "users";
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Firestore firestore;
	
	@Autowired
	private UserImageService userImageService;
	
	private CollectionReference getUsersCollection() {
		return firestore.collection(this.USERS_COLLECTION);
	}
	
	
	//Post a user onto the user collection. documentID is auto-generated by firestore.
	public String saveUser(User user) throws InterruptedException, ExecutionException {
		user.setHasImage(false);
		ApiFuture<DocumentReference> document = getUsersCollection().add(user);
		DocumentReference reference = document.get();
		String userId = document.get().getId();
		//Assign auto-generated Id to userId field for ease of querying.
		WriteBatch batch = firestore.batch();
		batch.update(reference, "userId",userId);
		//ENCODE USER PASSWORD!
		batch.update(reference, "userPassword", passwordEncoder.encode(reference.get().get().toObject(User.class).getUserPassword()));
		List<WriteResult> results = batch.commit().get();
		results.forEach(result -> {
			System.out.println("Update Time: " + result.getUpdateTime());
		});
		return "Created user with ID: " + "'" + userId + "'";
	}
	
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
	
	public List<D1Transaction> getAllUserTransactions(String userId) throws InterruptedException, ExecutionException{
		if(isActive(userId)) {
			return getUserReference(userId).collection("userTransactions").get().get()
																.getDocuments()
																.stream()
																.map(document -> document.toObject(D1Transaction.class))
																.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
	public Optional<D1Transaction> getUserTransactionById(String userId, String transactionId) throws InterruptedException, ExecutionException{
		if(isActive(userId)) {
			QuerySnapshot query = getUserReference(userId).collection("userTransactions").whereGreaterThanOrEqualTo("transactionId", transactionId).get().get();
			if(!query.isEmpty()) {				
				return Optional.of(query.getDocuments()
						.stream()
						.map(queryDocument -> queryDocument.toObject(D1Transaction.class))
						.collect(Collectors.toList()).get(0));
			}
		}
		return null;
	}
	
	public List<User> getFirstFifteenUsersByFifaWins() throws InterruptedException, ExecutionException{
		CollectionReference usersCollectionReference = getUsersCollection();
		Query firstPage = usersCollectionReference.orderBy("userFifaTotalWs", Direction.DESCENDING).limit(15);
		ApiFuture<QuerySnapshot> future = firstPage.get();
		return future.get()
							.getDocuments()
							.stream()
							.map(document -> document.toObject(User.class))
							.collect(Collectors.toList());
	}
	
	public List<User> getFirstFifteenUsersByCodWins() throws InterruptedException, ExecutionException{
		CollectionReference usersCollectionReference = getUsersCollection();
		Query firstPage = usersCollectionReference.orderBy("userCodTotalWs", Direction.DESCENDING).limit(15);
		ApiFuture<QuerySnapshot> future = firstPage.get();
		return future.get()
							.getDocuments()
							.stream()
							.map(document -> document.toObject(User.class))
							.collect(Collectors.toList());
	}
	
	public List<User> getNextFifaPageBy(String lastUserId) throws InterruptedException, ExecutionException{
		CollectionReference usersCollectionReference = getUsersCollection();
		DocumentSnapshot lasUserIdSnapshot = getUserReference(lastUserId).get().get();
		Query nextPage = usersCollectionReference.orderBy("userFifaTotalWs").startAfter(lasUserIdSnapshot).limit(15);
		ApiFuture<QuerySnapshot> future = nextPage.get();
		return future.get()
									.getDocuments()
									.stream()
									.map(document -> document.toObject(User.class))
									.collect(Collectors.toList());
	}
	
	public List<User> getNextCodPageBy(String lastUserId) throws InterruptedException, ExecutionException{
		CollectionReference usersCollectionReference = getUsersCollection();
		DocumentSnapshot lastUserIdSnapshot = getUserReference(lastUserId).get().get();
		Query nextPage = usersCollectionReference.orderBy("userCodTotalWs").startAfter(lastUserIdSnapshot).limit(15);
		ApiFuture<QuerySnapshot> future = nextPage.get();
		return future.get()
							.getDocuments()
							.stream()
							.map(document -> document.toObject(User.class))
							.collect(Collectors.toList());
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
	
	public Optional<UserDetails> getUserDetailsByUserName(String userName) throws InterruptedException, ExecutionException{
		Query query = getUsersCollection().whereEqualTo("userName", userName);
		QuerySnapshot userSnapshot = query.get().get();
		if(!userSnapshot.isEmpty()) {
			List<User> userList = userSnapshot.toObjects(User.class);
			for(User currUser : userList) {
				return Optional.of(UserDetailsImpl.build(currUser));
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
	
	public String changeUserPassword(String userId, String newPassword) throws InterruptedException, ExecutionException {
		if(isActive(userId)) {
			DocumentReference userReference = getUserReference(userId);
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "userPassword", passwordEncoder.encode(newPassword));
			batch.commit().get();
			return "Password Updated";
		}
		return "Not found.";
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
		return future.get().getDocuments()
							.stream()
							.map(document -> document.toObject(User.class))
							.collect(Collectors.toList());
	}
	
	
	//delete a User from users collection by a given id.
	//In reality delete method just changes UserStatus from active to inactive or banned.
	public String deleteUserById(String userId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getUsersCollection().document(userId);
		User user = reference.get().get().toObject(User.class);
		if(user == null) {
			return "User not found.";
		}
		if(user.isHasImage()) {
			deleteUserImage(user.getUserId());
		}
		reference.delete();
		return "User with ID: " + "'" +  userId + "'" + " was deleted.";
	}
	
	public void deleteUserImage(String userId) throws InterruptedException, ExecutionException {
		Optional<ImageModel> userImageModel = userImageService.getUserImage(userId);
		if(userImageModel.isPresent()) {
			firestore.collection("teamImages").document(userImageModel.get().getImageModelDocumentId()).delete(); 
		}
	}
	
	//Delete a User's certain field value.
	public String deleteUserField(String userId, String userField) throws InterruptedException, ExecutionException {
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
	
	//TODO
	public String deleteUserTransaction(String userId, String transactionId) throws InterruptedException, ExecutionException {
		if(isActive(userId)) {
			ApiFuture<WriteResult> transactionReference = getUsersCollection().document(userId).collection("userTransactions").document(transactionId).delete();
			return "Document deleted successfully."; 
		}
		return "Not found.";
	}
	
	//Change UserStatus to BANNED
	public String banUserById(String userId) throws InterruptedException, ExecutionException {
		final DocumentReference reference = getUsersCollection().document(userId);
		WriteBatch batch = firestore.batch().update(reference,"userStatusCode",UserStatus.BANNED);
		List<WriteResult> results = batch.commit().get();
		results.forEach(response -> System.out.println("Update Time: " + response.getUpdateTime()));
		if(reference.get().get().toObject(User.class).getUserStatusCode().equals(UserStatus.BANNED)) {
			return "User with ID: " + "'" + userId + "'"  + " was BANNED.";
		}
		return "User could not be BANNED.";
	}
	
	//Set a user with all new fields. 
	public String updateUser(User user) throws InterruptedException, ExecutionException {
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
		final DocumentReference reference = getUsersCollection().document(userId);
		if(!reference.get().get().exists()) {
			return "User not found.";
		}
		WriteBatch batch = firestore.batch();
		List<WriteResult> results = new ArrayList<>();
		//These fields cannot be updated.
		if(!objectField.equals("userName") && !objectField.equals("userCash") && !objectField.equals("userTokens") && !objectField.equals("userId")) {
			batch.update(reference, objectField, replaceValue);
			results = batch.commit().get();
			results.forEach(response -> {
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
		return "User field updated Successfully";
	}
	
		
	//Update a user's userName depending of availability and token adquisition capacity. i.e. if user has enough tokens to pay fee.
	public String updateUserName(String userId, String newUserName) throws InterruptedException, ExecutionException {
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
			WriteBatch batch = firestore.batch();
			User userOnDB = snapshot.toObject(User.class);
			if(userOnDB.getUserTokens() > 100) {
				batch.update(reference, "userName", newUserName);
				batch.update(reference, "userTokens", FieldValue.increment(-100));
				batch.commit().get();
				return "User name modified successfully";
			}
			return "Not enough tokens to change username";
		}
		return response;
	}
	
	public String updateUserTokens(User user, D1Service serviceToConsume) throws InterruptedException, ExecutionException {
		if(isActive(user.getUserId())) {
			DocumentReference userReference = getUserReference(user.getUserId());
			ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
				DocumentSnapshot userSnapshot = transaction.get(userReference).get();
				double userTokens = userSnapshot.getDouble("userTokens");
				transaction.update(userReference, "userTokens", userTokens + serviceToConsume.getServiceGivingAmount());
				return "Updated Tokens";
			});
			return futureTransaction.get();
		}
		return "Not found.";
	}
	
	
	//update user Currency field.
	public String updateUserCash(String userId, double cashQuantity) throws InterruptedException, ExecutionException {
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
		final DocumentReference userReference = getUsersCollection().document(userId);
		String response = "User not found.";
		//evaluate if user exists on collection.
		if(userReference.get().get().exists()) {
			ApiFuture<String> futureTransaction = firestore.runTransaction(transaction -> {
				DocumentSnapshot userSnapshot = transaction.get(userReference).get();
				double userTokens = userSnapshot.getDouble("userTokens");
				transaction.update(userReference, "userTokens", userTokens + tokenQuantity);
				return "Updated Tokens";
			});
			response = futureTransaction.get();
			return response;
		}
		return response;
	}
	
	//evaluate if given documentId exists on given collection.
	public boolean isPresent(String userId,String collectionName) throws InterruptedException, ExecutionException {
		DocumentReference reference = firestore.collection(collectionName).document(userId);
		ApiFuture<DocumentSnapshot> snapshot = reference.get();
		DocumentSnapshot document = snapshot.get();
		if(!document.exists()) {
			return false;
		}
		return true;
	}

	//Evaluate if given document's status corresponds to active.
	public boolean isActive(String userId) throws InterruptedException, ExecutionException {
		DocumentReference reference = getUserReference(userId);
		DocumentSnapshot snapshot = reference.get().get();
		if(snapshot.exists() && snapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return true;
		}
		return false;
	}
	
}
