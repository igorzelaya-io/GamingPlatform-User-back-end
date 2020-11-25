package com.d1gaming.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.d1gaming.user.firebaseconfig.UserFirestoreUtils;

@SpringBootApplication
public class D1GamingUserBackEnd1Application {

	public static void main(String[] args) {
		UserFirestoreUtils.initialize(UserFirestoreUtils.getOptions());
		SpringApplication.run(D1GamingUserBackEnd1Application.class, args);
	}

}
