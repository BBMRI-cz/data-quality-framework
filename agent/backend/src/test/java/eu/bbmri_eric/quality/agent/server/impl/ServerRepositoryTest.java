package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ServerRepositoryTest {

  @Autowired private ServerRepository serverRepository;

  @Autowired private EntityManager entityManager;

  @Test
  void findAllByNextRegistrationRetryAtLessThanEqual_returnsOnlyServersWithDueRetry() {
    Server due = new Server("https://due.example.com", "Due");
    due.setNextRegistrationRetryAt(LocalDateTime.now().minusMinutes(1));
    Server future = new Server("https://future.example.com", "Future");
    future.setNextRegistrationRetryAt(LocalDateTime.now().plusHours(1));
    Server unscheduled = new Server("https://unscheduled.example.com", "Unscheduled");
    serverRepository.saveAll(List.of(due, future, unscheduled));
    entityManager.flush();
    entityManager.clear();

    List<Server> result =
        serverRepository.findAllByNextRegistrationRetryAtLessThanEqual(LocalDateTime.now());

    assertThat(result)
        .extracting(Server::getUrl)
        .contains("https://due.example.com")
        .doesNotContain("https://future.example.com", "https://unscheduled.example.com");
  }

  @Test
  void findWithInteractionsByUrl_existingServer_returnsServerWithItsInteractions() {
    serverRepository.save(new Server("https://central.example.com", "Central"));
    entityManager.flush();
    entityManager.clear();

    Server found =
        serverRepository
            .findWithInteractionsByUrl("https://central.example.com")
            .orElseThrow(() -> new AssertionError("server not found"));

    assertThat(found.getName()).isEqualTo("Central");
    assertThat(found.getInteractions())
        .extracting(ServerInteraction::getDescription)
        .containsExactly("Central server added to the database");
  }
}
