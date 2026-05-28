package eu.bbmri_eric.quality.agent.dataquality.dto;

import java.util.Set;
import lombok.Data;

@Data
public class ReportResultDTO {
  private Long id;
  private String checkName;
  private Long checkId;
  private Integer rawValue;
  private Double obfuscatedValue;
  private int warningThreshold;
  private int errorThreshold;
  private double epsilon;
  private String error;
  private String stratum;
  private Set<String> patients;
}
