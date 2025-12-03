package eu.bbmri_eric.quality.agent.server.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link eu.bbmri_eric.quality.agent.server.domain.Server} entity. */
class ServerTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testConstructorCreatesServerWithAllFields() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    assertEquals("https://example.com", server.getUrl());
    assertEquals("Test Server", server.getName());
    assertEquals("client123", server.getClientId());
    assertEquals(base64Secret, server.getClientSecret());
    assertEquals(ServerConnectionStatus.ACTIVE, server.getStatus());
  }

  @Test
  void testConstructorLogsCreationInteraction() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.PENDING);
    assertEquals(1, server.getInteractions().size());
    ServerInteraction interaction = server.getInteractions().get(0);
    assertEquals(InteractionType.UPDATE, interaction.getType());
    assertTrue(interaction.getDescription().contains("Central server added to the database"));
  }

  @Test
  void testValidBase64ClientSecret() {
    String base64Secret = Base64.getEncoder().encodeToString("mysecret".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    Set<ConstraintViolation<Server>> violations = validator.validate(server);
    assertTrue(violations.isEmpty(), "Valid Base64 secret should not produce violations");
  }

  @Test
  void testSetUrlLogsInteractionWhenIdIsSet() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    // Simulate that the entity has been persisted by using reflection to set the ID
    setServerId(server, "test-uuid-1");

    server.setUrl("https://newurl.com");

    assertEquals("https://newurl.com", server.getUrl());
    assertEquals(2, server.getInteractions().size());
    ServerInteraction interaction = server.getInteractions().get(1);
    assertEquals(InteractionType.UPDATE, interaction.getType());
    assertEquals(
        "URL updated from 'https://example.com' to 'https://newurl.com'",
        interaction.getDescription());
  }

  @Test
  void testSetUrlDoesNotLogWhenValueIsUnchanged() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server, "test-uuid-1");

    server.setUrl("https://example.com");

    // Only creation interaction should exist
    assertEquals(1, server.getInteractions().size());
  }

  @Test
  void testSetNameLogsInteractionWhenIdIsSet() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server, "test-uuid-1");

    server.setName("Updated Server");

    assertEquals("Updated Server", server.getName());
    assertEquals(2, server.getInteractions().size());
    ServerInteraction interaction = server.getInteractions().get(1);
    assertEquals(InteractionType.UPDATE, interaction.getType());
    assertEquals(
        "Name updated from 'Test Server' to 'Updated Server'", interaction.getDescription());
  }

  @Test
  void testSetClientIdLogsInteractionWhenIdIsSet() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server, "test-uuid-1");

    server.setClientId("newclient789");

    assertEquals("newclient789", server.getClientId());
    assertEquals(2, server.getInteractions().size());
    ServerInteraction interaction = server.getInteractions().get(1);
    assertEquals(InteractionType.UPDATE, interaction.getType());
    assertEquals(
        "Client ID updated from 'client123' to 'newclient789'", interaction.getDescription());
  }

  @Test
  void testSetClientSecretLogsInteractionWhenIdIsSet() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server, "test-uuid-1");

    String newBase64Secret = Base64.getEncoder().encodeToString("newsecret789".getBytes());
    server.setClientSecret(newBase64Secret);

    assertEquals(newBase64Secret, server.getClientSecret());
    assertEquals(2, server.getInteractions().size());
    ServerInteraction interaction = server.getInteractions().get(1);
    assertEquals(InteractionType.UPDATE, interaction.getType());
    // Should not include actual secret values
    assertEquals("Client secret updated", interaction.getDescription());
  }

  @Test
  void testSetStatusLogsInteractionWhenIdIsSet() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.PENDING);

    setServerId(server, "test-uuid-1");

    server.setStatus(ServerConnectionStatus.ACTIVE);

    assertEquals(ServerConnectionStatus.ACTIVE, server.getStatus());
    assertEquals(2, server.getInteractions().size());
    ServerInteraction interaction = server.getInteractions().get(1);
    assertEquals(InteractionType.UPDATE, interaction.getType());
    assertEquals("Status changed from PENDING to ACTIVE", interaction.getDescription());
  }

  @Test
  void testSetStatusDoesNotLogWhenStatusIsUnchanged() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server, "test-uuid-1");

    server.setStatus(ServerConnectionStatus.ACTIVE);

    // Only creation interaction should exist
    assertEquals(1, server.getInteractions().size());
  }

  @Test
  void testAddInteraction() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    ServerInteraction interaction =
        new ServerInteraction(InteractionType.COMMUNICATION, "Test communication");
    server.addInteraction(interaction);

    assertEquals(2, server.getInteractions().size());
    assertEquals(interaction, server.getInteractions().get(1));
  }

  @Test
  void testGetInteractionsReturnsUnmodifiableList() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    try {
      server
          .getInteractions()
          .add(new ServerInteraction(InteractionType.COMMUNICATION, "Should fail"));
      // If we reach here, the test should fail
      assertTrue(false, "Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // Expected behavior
      assertTrue(true);
    }
  }

  @Test
  void testEqualsWithSameValues() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server1 =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);
    Server server2 =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    // Set same IDs
    setServerId(server1, "test-uuid-1");
    setServerId(server2, "test-uuid-1");

    assertEquals(server1, server2);
    assertEquals(server1.hashCode(), server2.hashCode());
  }

  @Test
  void testEqualsWithDifferentIds() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server1 =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);
    Server server2 =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server1, "test-uuid-1");
    setServerId(server2, "test-uuid-2");

    assertNotEquals(server1, server2);
  }

  @Test
  void testEqualsWithDifferentUrls() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server1 =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);
    Server server2 =
        new Server(
            "https://different.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server1, "test-uuid-1");
    setServerId(server2, "test-uuid-1");

    assertNotEquals(server1, server2);
  }

  @Test
  void testToString() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.ACTIVE);

    setServerId(server, "test-uuid-1");

    String toString = server.toString();
    assertTrue(toString.contains("id=test-uuid-1"));
    assertTrue(toString.contains("url='https://example.com'"));
    assertTrue(toString.contains("name='Test Server'"));
    assertTrue(toString.contains("clientId='client123'"));
    assertTrue(toString.contains("status=ACTIVE"));
    // Should not contain the secret
    assertFalse(toString.contains(base64Secret));
  }

  @Test
  void testMultipleUpdatesLogMultipleInteractions() {
    String base64Secret = Base64.getEncoder().encodeToString("secret456".getBytes());
    Server server =
        new Server(
            "https://example.com",
            "Test Server",
            "client123",
            base64Secret,
            ServerConnectionStatus.PENDING);

    setServerId(server, "test-uuid-1");

    server.setStatus(ServerConnectionStatus.ACTIVE);
    server.setUrl("https://newurl.com");
    server.setName("Updated Server");

    assertEquals(4, server.getInteractions().size());
    assertEquals(
        "Central server added to the database", server.getInteractions().get(0).getDescription());
    assertTrue(server.getInteractions().get(1).getDescription().contains("Status changed"));
    assertTrue(server.getInteractions().get(2).getDescription().contains("URL updated"));
    assertTrue(server.getInteractions().get(3).getDescription().contains("Name updated"));
  }

  /**
   * Helper method to set the server ID using reflection (simulating JPA persistence).
   *
   * @param server the server to set the ID on
   * @param id the ID to set
   */
  private void setServerId(Server server, String id) {
    try {
      var idField = Server.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(server, id);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set server ID", e);
    }
  }
}
