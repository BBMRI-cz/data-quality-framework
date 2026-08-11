package eu.bbmri_eric.quality.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Main Application class */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AgentApplication {

  static void main(String[] args) {
    SpringApplication.run(AgentApplication.class, args);
  }
}
