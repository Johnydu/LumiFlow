package br.com.lumiflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LumiflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(LumiflowApplication.class, args);


	}
}
