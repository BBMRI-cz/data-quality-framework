package eu.bbmri_eric.quality.server.user;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for JWT authentication using MockMvc with @SpringBootTest. Tests the
 * /api/auth/login endpoint for JWT token generation and validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  private static final String LOGIN_ENDPOINT = "/api/auth/login";
  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "adminpass";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Value("${app.jwt.expiration:3600000}")
  private long jwtExpiration;

  @Test
  @DisplayName("Should return unauthorized for invalid credentials")
  void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
    // Test case-sensitive username
    mockMvc
        .perform(
            post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createLoginJson(ADMIN_USERNAME.toUpperCase(), ADMIN_PASSWORD)))
        .andExpect(status().isUnauthorized());

    // Test wrong password
    mockMvc
        .perform(
            post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createLoginJson(ADMIN_USERNAME, "wrongpassword")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return valid JWT token with correct structure and claims")
  void login_withValidCredentials_returnsValidJwtToken() throws Exception {
    long loginTime = System.currentTimeMillis();

    MvcResult result =
        mockMvc
            .perform(
                post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createLoginJson(ADMIN_USERNAME, ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.token").value(not(emptyString())))
            .andExpect(jsonPath("$.user.username").value(ADMIN_USERNAME))
            .andReturn();

    String token = extractTokenFromResponse(result);
    validateJwtToken(token, loginTime);
  }

  @Test
  @DisplayName("Should generate unique tokens for subsequent logins")
  void login_multipleRequests_generatesUniqueTokens() throws Exception {
    // Generate two tokens with delay to ensure different timestamps
    MvcResult result1 = performLogin();
    Thread.sleep(1100); // JWT timestamps have second precision
    MvcResult result2 = performLogin();

    String token1 = extractTokenFromResponse(result1);
    String token2 = extractTokenFromResponse(result2);

    assert !token1.equals(token2) : "Subsequent logins should generate different tokens";

    // Verify different timestamps
    JsonNode payload1 = extractPayload(token1);
    JsonNode payload2 = extractPayload(token2);
    assert payload1.get("iat").asLong() < payload2.get("iat").asLong()
        : "Second token should have later timestamp";
  }

  /** Comprehensive JWT token validation covering structure, claims, security, and format. */
  private void validateJwtToken(String token, long loginTime) throws Exception {
    // Basic structure validation
    String[] tokenParts = token.split("\\.");
    assert tokenParts.length == 3 : "JWT token should have 3 parts separated by dots";
    assert token.length() > 100 && token.length() < 4096
        : "Token should be substantial but under 4KB for HTTP headers";
    assert token.matches("[A-Za-z0-9._-]+") : "Token should only contain valid JWT characters";

    // Header validation
    JsonNode header =
        objectMapper.readTree(new String(Base64.getUrlDecoder().decode(tokenParts[0])));
    assert "JWT".equals(header.get("typ").asText()) : "Token type should be JWT";
    assert "HS256".equals(header.get("alg").asText()) : "Algorithm should be HS256";
    assert !header.has("crit") : "Token should not contain critical header parameter";

    // Payload validation
    JsonNode payload =
        objectMapper.readTree(new String(Base64.getUrlDecoder().decode(tokenParts[1])));

    // Required claims
    assert ADMIN_USERNAME.equals(payload.get("sub").asText()) : "Subject should match username";
    assert payload.has("authorities") : "Token should contain authorities claim";
    assert payload.has("iat") : "Token should contain issued at timestamp";
    assert payload.has("exp") : "Token should contain expiration timestamp";

    // Timestamp validation
    long iat = payload.get("iat").asLong() * 1000; // Convert to milliseconds
    long exp = payload.get("exp").asLong() * 1000;
    long actualDuration = exp - iat;

    assert iat > 0 && exp > 0 : "Timestamps should be positive";
    assert exp > iat : "Expiration should be after issued at";
    assert Math.abs(iat - loginTime) < 5000 : "Issued at should be close to login time";
    assert Math.abs(actualDuration - jwtExpiration) < 5000
        : "Token duration should match configured expiration";
    assert exp > System.currentTimeMillis() : "Token should not be expired immediately";

    // Signature validation (structure only - we can't verify signature without the secret)
    assert !tokenParts[2].isEmpty() : "Signature should not be empty";
    assert !tokenParts[2].contains("=") : "Base64 URL encoding should not contain padding";

    // Encoding validation
    try {
      Base64.getUrlDecoder().decode(tokenParts[0]); // header
      Base64.getUrlDecoder().decode(tokenParts[1]); // payload
    } catch (IllegalArgumentException e) {
      assert false : "Token parts should be valid Base64 URL encoded: " + e.getMessage();
    }
  }

  /** Helper method to perform login and return result */
  private MvcResult performLogin() throws Exception {
    return mockMvc
        .perform(
            post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createLoginJson(ADMIN_USERNAME, ADMIN_PASSWORD)))
        .andExpect(status().isOk())
        .andReturn();
  }

  /** Helper method to create login JSON payload */
  private String createLoginJson(String username, String password) {
    return """
        {
          "username": "%s",
          "password": "%s"
        }
        """
        .formatted(username, password);
  }

  /** Helper method to extract token from MvcResult response */
  private String extractTokenFromResponse(MvcResult result) throws Exception {
    String responseContent = result.getResponse().getContentAsString();
    JsonNode responseJson = objectMapper.readTree(responseContent);
    return responseJson.get("token").asText();
  }

  /** Helper method to extract and parse JWT payload */
  private JsonNode extractPayload(String token) throws Exception {
    String[] tokenParts = token.split("\\.");
    String payloadJson = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
    return objectMapper.readTree(payloadJson);
  }
}
