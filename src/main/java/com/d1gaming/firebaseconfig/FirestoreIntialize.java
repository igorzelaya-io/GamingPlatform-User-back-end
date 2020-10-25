package com.d1gaming.firebaseconfig;

import java.io.FileInputStream;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Service
public class FirestoreIntialize {
	
	@PostConstruct
	public void initializeFirestore() {
		try {
			FileInputStream serviceAccount = 
			new FileInputStream("./firebase-admin-sdk.json");
			FirebaseOptions options  = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccount))
					.setDatabaseUrl("https://gamingplatform-c922d.firebaseio.com")
					.build();
			FirebaseApp.initializeApp(options);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
