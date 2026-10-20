package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * {@link DataStore} backed by the omicsbox sandbox HTTP API. The sandbox executes Python quality
 * check scripts against a mounted omics data directory and also exposes health, entity-count and
 * entity-metadata endpoints, which this store adapts to the {@link DataStore} contract:
 *
 * <ul>
 *   <li>{@code POST /run} — executes a check's Python script (the check {@code query}). The script
 *       must print its result as a single JSON object on the last line of stdout: {@code {"count":
 *       int, "entityType": str, "ids": [str], "error": str|null}}.
 *   <li>{@code GET /health} — liveness probe (unauthenticated).
 *   <li>{@code GET /counts} — configured entity counts.
 *   <li>{@code GET /entities/<entityType>/<id>} — metadata of a file entity.
 * </ul>
 *
 * Authentication uses a Bearer token when one is configured in the settings; the sandbox only
 * requires it if it was started with {@code OMICSBOX_API_TOKEN} set.
 */
@Slf4j
public class OmicsBoxDataStore implements DataStore {

  /**
   * Checks can legitimately run for a long time: the sandbox queues a single script and executes it
   * with a configurable timeout ({@code OMICSBOX_TIMEOUT}), which deployments raise for heavy
   * checks over large corpora. The client must never give up before the sandbox does, so the read
   * timeout is deliberately far above any realistic sandbox configuration: the sandbox is the one
   * and only place bounding script execution time.
   */
  private static final Duration READ_TIMEOUT = Duration.ofHours(24);

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /** Bounds the stderr echoed into check results so huge outputs cannot bloat stored reports. */
  private static final int MAX_ERROR_DETAILS_LENGTH = 500;

  private static final String RESULT_STATUS_OK = "ok";

  private final RestTemplate restTemplate;
  private final String baseUrl;

  OmicsBoxDataStore(RestTemplateBuilder restTemplateBuilder, String baseUrl, String apiToken) {
    ClientHttpRequestFactorySettings requestFactorySettings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(CONNECT_TIMEOUT)
            .withReadTimeout(READ_TIMEOUT);
    RestTemplateBuilder builder =
        restTemplateBuilder.requestFactory(
            () -> ClientHttpRequestFactoryBuilder.detect().build(requestFactorySettings));
    if (apiToken != null && !apiToken.isBlank()) {
      builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);
    }
    this.restTemplate = builder.build();
    this.baseUrl = normalizeBaseUrl(baseUrl);
  }

  private static String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return null;
    }
    String normalized = baseUrl.trim();
    return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
  }

  private void requireConfigured() {
    if (baseUrl == null) {
      throw new IllegalStateException("OmicsBox data store is not configured (missing URL)");
    }
  }

  /**
   * Executes a Python quality check by posting its script to the sandbox's {@code /run} endpoint.
   * The sandbox response carries the captured stdout/stderr; the script's result is the JSON object
   * printed as the last line of stdout.
   *
   * @param script the Python script of the check
   * @return the parsed script result, or a {@link ResultDTO} carrying the error when the run or the
   *     result contract failed
   */
  @Override
  public ResultDTO executeQuery(String script) {
    try {
      requireConfigured();
      if (script == null || script.isBlank()) {
        return new ResultDTO("Python quality check has an empty script.");
      }
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      ResponseEntity<String> response =
          restTemplate.exchange(
              baseUrl + "/run", HttpMethod.POST, new HttpEntity<>(script, headers), String.class);
      return toResult(new JSONObject(response.getBody()));
    } catch (RestClientException | IllegalStateException e) {
      log.error("Failed to execute Python check on omicsbox: {}", e.getMessage(), e);
      return new ResultDTO(e.getMessage());
    }
  }

  private ResultDTO toResult(JSONObject runResult) {
    String status = runResult.optString("status");
    if (!RESULT_STATUS_OK.equals(status)) {
      String stderr = truncate(runResult.optString("stderr"));
      return new ResultDTO(
          "Python script execution failed (status=%s): %s".formatted(status, stderr));
    }
    try {
      JSONObject result = parseScriptResult(runResult.getString("stdout"));
      Set<String> idSet = parseIds(result.optJSONArray("ids"));
      Integer count = result.has("count") ? result.getInt("count") : idSet.size();
      String error = result.isNull("error") ? null : result.optString("error", null);
      return new ResultDTO(count, result.optString("entityType"), idSet, error);
    } catch (JSONException e) {
      log.warn("Python script did not produce a valid result JSON: {}", e.getMessage());
      return new ResultDTO(
          "Python script did not produce a valid result JSON as the last line of stdout.");
    }
  }

  /**
   * The result contract requires the JSON document to be printed exactly once and as the last line
   * of stdout; earlier lines are free-form log output of the script.
   */
  private static JSONObject parseScriptResult(String stdout) {
    String[] lines = stdout.split("\\R");
    for (int i = lines.length - 1; i >= 0; i--) {
      if (!lines[i].isBlank()) {
        return new JSONObject(lines[i]);
      }
    }
    throw new JSONException("stdout is empty");
  }

  private static Set<String> parseIds(JSONArray ids) {
    if (ids == null) {
      return Set.of();
    }
    Set<String> idSet = new HashSet<>();
    for (int i = 0; i < ids.length(); i++) {
      idSet.add(ids.getString(i));
    }
    return idSet;
  }

  private static String truncate(String value) {
    if (value == null || value.length() <= MAX_ERROR_DETAILS_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_ERROR_DETAILS_LENGTH) + "...";
  }

  @Override
  public JSONObject getEntity(String entityType, String id) {
    requireConfigured();
    try {
      String body =
          restTemplate.getForObject(
              baseUrl + "/entities/{entityType}/{id}", String.class, entityType, id);
      return body != null ? new JSONObject(body) : null;
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 404) {
        return null;
      }
      log.error(
          "Failed to retrieve entity {}/{} from omicsbox: {}", entityType, id, e.getMessage());
      throw new IllegalStateException("Failed to retrieve entity: " + e.getMessage(), e);
    }
  }

  @Override
  public JSONObject checkHealth() {
    JSONObject healthStatus = new JSONObject();
    try {
      requireConfigured();
      ResponseEntity<String> response =
          restTemplate.getForEntity(baseUrl + "/health", String.class);
      healthStatus.put("status", "UP");
      healthStatus.put("details", extractHealthDetails(response.getBody()));
    } catch (RestClientException | IllegalStateException e) {
      log.error("OmicsBox health check failed: {}", e.getMessage(), e);
      healthStatus.put("status", "DOWN");
      healthStatus.put("details", new JSONObject().put("error", e.getMessage()));
    }
    return healthStatus;
  }

  @Override
  public DatabaseHealthDTO checkHealthV2() {
    try {
      requireConfigured();
      ResponseEntity<String> response =
          restTemplate.getForEntity(baseUrl + "/health", String.class);
      JSONObject details = extractHealthDetails(response.getBody());
      return new DatabaseHealthDTO(DBStatus.UP, null, details != null ? details.toString() : null);
    } catch (RestClientException | IllegalStateException e) {
      log.error("OmicsBox health check V2 failed: {}", e.getMessage(), e);
      return new DatabaseHealthDTO(DBStatus.DOWN, e.getMessage(), null);
    }
  }

  /**
   * Extracts the sandbox's health {@code details} object; {@code null} when the body is not the
   * expected status/details JSON (e.g. a proxy error page).
   */
  private static JSONObject extractHealthDetails(String body) {
    if (body == null) {
      return null;
    }
    try {
      JSONObject json = new JSONObject(body);
      return json.isNull("details") ? null : json.optJSONObject("details");
    } catch (JSONException e) {
      return null;
    }
  }

  @Override
  public Integer countPatients() {
    return fetchCount("patients");
  }

  @Override
  public Integer countSecondaryEntities() {
    return fetchCount("secondary_entities");
  }

  private Integer fetchCount(String key) {
    try {
      requireConfigured();
      String body = restTemplate.getForObject(baseUrl + "/counts", String.class);
      JSONObject counts = new JSONObject(body);
      return counts.isNull(key) ? null : counts.getInt(key);
    } catch (RestClientException | IllegalStateException | JSONException e) {
      log.error("Failed to fetch {} count from omicsbox: {}", key, e.getMessage(), e);
      return null;
    }
  }
}
