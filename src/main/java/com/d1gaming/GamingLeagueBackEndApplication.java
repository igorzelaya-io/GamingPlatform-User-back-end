package com.d1gaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@SpringBootApplication
public class GamingLeagueBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamingLeagueBackEndApplication.class, args);
	}

}
