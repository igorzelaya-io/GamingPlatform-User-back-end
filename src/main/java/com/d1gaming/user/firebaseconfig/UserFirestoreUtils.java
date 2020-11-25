package com.d1gaming.user.firebaseconfig;
import java.io.FileInputStream;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

@Component
public class UserFirestoreUtils {

//	@Value("classpath:static/gamingplatform-c922d-firebase-adminsdk-c25o8-06e92edfd5.json")
//	Resource resourceFile;
	
	public static FirebaseOptions getOptions() {
        FirebaseOptions options = null;
		try {
        	FileInputStream serviceAccount = new FileInputStream("./src/main/resources/static/gamingplatform-c922d-firebase-adminsdk-c25o8-06e92edfd5.json");
        	GoogleCredentials cred = GoogleCredentials.fromStream(serviceAccount)
        								.createScoped("https://www.googleapis.com/auth/datastore");
            options = FirebaseOptions.builder()
            		.setCredentials(cred)
            		.setDatabaseUrl("https://gamingplatform-c922d.firebaseio.com")
            		.build();
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
		return options;
	}
	
	public static void initialize(FirebaseOptions options) {
		FirebaseApp.initializeApp(options);
	}
	
	public static Firestore getFirestore() {
		return FirestoreClient.getFirestore();
	}



}
