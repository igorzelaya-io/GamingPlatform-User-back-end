package com.d1gaming.user.firebaseconfig;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Service
public class UserFirestoreInitialize {
	
	@Value("classpath:static/gamingplatform-c922d-firebase-adminsdk-c25o8-06e92edfd5.json")
	Resource resourceFile;
	
	public void initialize() {
        try {
        	InputStream serviceAccount = resourceFile.getInputStream();
        	GoogleCredentials cred = GoogleCredentials.fromStream(serviceAccount)
        								.createScoped("https://www.googleapis.com/auth/datastore");
            FirebaseOptions options = FirebaseOptions.builder()
            		.setCredentials(cred)
            		.setDatabaseUrl("https://gamingplatform-c922d.firebaseio.com")
            		.build();
            FirebaseApp.initializeApp(options);
        } 
        catch (Exception e) {
        	e.printStackTrace();
        }
	}
}