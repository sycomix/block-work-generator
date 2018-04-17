package io.block16.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GeneratorApplication {
	public static void main(String[] args) {
		SpringApplication.run(GeneratorApplication.class, args);
	}
}
