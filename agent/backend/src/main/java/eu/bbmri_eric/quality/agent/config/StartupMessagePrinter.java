package eu.bbmri_eric.quality.agent.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Start up message printer */
@Component
class StartupMessagePrinter implements ApplicationListener<ApplicationReadyEvent> {
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String STATUS_OK = "OK";
  public static final String STATUS_FAILED = "FAILED";
  private static final int LABEL_WIDTH = 21;

  private final Environment environment;
  private final BuildProperties buildProperties;
  private final DataSource dataSource;

  StartupMessagePrinter(
      Environment environment, BuildProperties buildProperties, DataSource dataSource) {
    this.environment = environment;
    this.buildProperties = buildProperties;
    this.dataSource = dataSource;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    List<String> messages =
        new ArrayList<>(
            List.of(
                "🛩️ Red Five standing by. All systems go.",
                "🚀 Hyperdrive engaged. Punch it, Chewie!",
                "🎉 App started. This is where the fun begins.",
                "✨ The Force is with us. App startup complete.",
                "🌠 App ready. Preparing to jump to hyperspace."));
    Collections.shuffle(messages);
    String dbStatus = databaseStatus();
    String apiStatus = apiStatus();
    String border = "#".repeat(60);
    IO.println(border);
    IO.println(statusLine("Database", dbStatus));
    IO.println(statusLine("API", apiStatus));
    IO.println(statusLine("UI", uiStatus()));
    IO.println(infoLine("Version", buildProperties.getVersion()));
    IO.println(infoLine("Profile", profile()));
    IO.println(infoLine("Started In", formatSeconds(event.getTimeTaken())));
    if (event.getArgs().length > 0) {
      IO.println(infoLine("Args", String.join(" ", event.getArgs())));
    }
    IO.println(ANSI_GREEN + " " + messages.getFirst() + ANSI_RESET);
    IO.println(border);
  }

  private String formatSeconds(Duration duration) {
    return String.format("%.2f s", duration.toMillis() / 1000.0);
  }

  private String infoLine(String label, String value) {
    return " " + label + ".".repeat(LABEL_WIDTH - label.length()) + " " + value;
  }

  private String statusLine(String label, String value) {
    String color = STATUS_OK.equals(value) ? ANSI_GREEN : ANSI_RED;
    return " "
        + label
        + ".".repeat(LABEL_WIDTH - label.length())
        + " "
        + color
        + value
        + ANSI_RESET;
  }

  private String databaseStatus() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(2) ? STATUS_OK : STATUS_FAILED;
    } catch (Exception e) {
      return STATUS_FAILED;
    }
  }

  private String apiStatus() {
    try {
      URL url = new URL("http://localhost:8081/api/health");
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");
      return con.getResponseCode() == 200 ? STATUS_OK : STATUS_FAILED;
    } catch (IOException e) {
      return STATUS_FAILED;
    }
  }

  private String uiStatus() {
    try {
      return new ClassPathResource("/static/index.html").exists() ? STATUS_OK : STATUS_FAILED;
    } catch (Exception e) {
      return STATUS_FAILED;
    }
  }

  private String profile() {
    String[] profiles = environment.getActiveProfiles();
    return profiles.length > 0 ? String.join(", ", profiles) : "default";
  }
}
