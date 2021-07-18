package com.d1gaming.user.image;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.d1gaming.library.image.ImageModel;
import com.d1gaming.library.user.User;
import com.d1gaming.library.user.UserStatus;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

@Service
public class UserImageService {

	@Autowired
	private Firestore firestore;
	
	private boolean isActive(String userId) throws InterruptedException, ExecutionException {
		DocumentReference userReference = firestore.collection("users").document(userId);
		DocumentSnapshot userSnapshot = userReference.get().get();
		if(userSnapshot.exists() && userSnapshot.toObject(User.class).getUserStatusCode().equals(UserStatus.ACTIVE)) {
			return true; 
		}
		return false;
	}
	
	public Optional<ImageModel> getUserImage(String userId) throws InterruptedException, ExecutionException {
		if(isActive(userId)) {
			QuerySnapshot userImageQuery = firestore.collection("userImages").whereEqualTo("dtoID", userId).get().get();
			if(!userImageQuery.isEmpty()) {
				return Optional.of(userImageQuery.getDocuments().get(0).toObject(ImageModel.class));
			}
		}
		return null;
	}
	
	public String saveUserImage(ImageModel userImage) throws InterruptedException, ExecutionException {
		if(isActive(userImage.getDtoID())) {
			Optional<ImageModel> userImageModel = getUserImage(userImage.getDtoID());
			if(userImageModel != null) {
				firestore.collection("userImages").document(userImage.getImageModelDocumentId()).set(userImage).get(); 
				return "User image updated successfully.";
			}
			DocumentReference userImageReference = firestore.collection("userImages").add(userImage).get();
			DocumentReference userReference = firestore.collection("users").document(userImage.getDtoID());
			
			String documentId = userImageReference.getId();
			WriteBatch batch = firestore.batch();
			batch.update(userReference, "hasImage", true);
			batch.update(userImageReference, "imageModelDocumentId", documentId);
			batch.commit().get();
			return "User image uploaded successfully.";
		}
		return "Not found.";
	}
}