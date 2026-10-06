package com.project.bookngo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookngoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookngoApplication.class, args);
	}

}
