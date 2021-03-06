package com.d1gaming.user.firebaseconfig;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

@Service
public class UserFirestoreInitialize {
	
	@Value("classpath:static/D1GamingPlatform-3576ad78e8f2.json")
	Resource resourceFile;
	
	@PostConstruct
	public FirestoreOptions initialize() throws IOException {
        return FirestoreOptions.getDefaultInstance()
        										.toBuilder().setProjectId("d1gamingplatform")
        										.setCredentials(GoogleCredentials.fromStream(resourceFile.getInputStream()))
        										.build();
	}
	
	@Bean
	public Firestore getFirestore() throws IOException {
		return initialize().getService();
	}
}