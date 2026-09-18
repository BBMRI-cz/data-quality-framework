package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.server.domain.Manifest;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ManifestRepositoryTest {

  @Autowired private ManifestRepository manifestRepository;

  @Autowired private ServerRepository serverRepository;

  @Autowired private EntityManager entityManager;

  private Server server;

  @BeforeEach
  void setUp() {
    manifestRepository.deleteAll();
    server =
        serverRepository.save(
            new Server(
                "https://central.example.com",
                "Central",
                "client-id",
                "client-secret",
                ServerConnectionStatus.ACTIVE));
  }

  @Test
  void save_persistsManifestLinkedToServer() {
    Manifest manifest = new Manifest(1L, "Core checks", server);
    manifest.setInstalledVersion(3);

    Manifest saved = manifestRepository.save(manifest);

    Optional<Manifest> loaded = manifestRepository.findById(saved.getId());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getRemoteId()).isEqualTo(1L);
    assertThat(loaded.get().getName()).isEqualTo("Core checks");
    assertThat(loaded.get().getInstalledVersion()).isEqualTo(3);
    assertThat(loaded.get().getServer().getId()).isEqualTo(server.getId());
  }

  @Test
  void findByServerIdAndRemoteId_resolvesManifest() {
    manifestRepository.save(new Manifest(42L, "Core checks", server));

    Optional<Manifest> loaded = manifestRepository.findByServerIdAndRemoteId(server.getId(), 42L);

    assertThat(loaded).isPresent();
    assertThat(loaded.get().getName()).isEqualTo("Core checks");
    assertThat(manifestRepository.findByServerIdAndRemoteId(server.getId(), 1L)).isEmpty();
  }

  @Test
  void save_persistsLinkedQualityCheckIds() {
    Manifest manifest = new Manifest(1L, "Core checks", server);
    manifest.addQualityCheckId(persistCheck("Missing values").getId());
    manifest.addQualityCheckId(persistCheck("Duplicates").getId());

    Manifest saved = manifestRepository.save(manifest);

    Optional<Manifest> loaded = manifestRepository.findById(saved.getId());
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getQualityCheckIds()).hasSize(2);
  }

  @Test
  void delete_manifestKeepsQualityChecks() {
    Long checkId = persistCheck("Missing values").getId();
    Manifest manifest = new Manifest(1L, "Core checks", server);
    manifest.addQualityCheckId(checkId);
    Manifest saved = manifestRepository.save(manifest);

    manifestRepository.deleteById(saved.getId());
    manifestRepository.flush();

    assertThat(entityManager.find(QualityCheck.class, checkId)).isNotNull();
  }

  private QualityCheck persistCheck(String name) {
    QualityCheck check = new QualityCheck(name, "Description of " + name, "define X: true");
    entityManager.persist(check);
    return check;
  }
}
