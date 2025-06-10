package eu.bbmri_eric.quality.agent.report;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String checkName;
    private Long checkId;
    private int rawValue;
    private int obfuscatedValue;
    private int warningThreshold;
    private int errorThreshold;

    protected Result() {
    }

    public Result(Long id, String checkName, Long checkId, int rawValue, int obfuscatedValue, int warningThreshold, int errorThreshold) {
        this.id = id;
        this.checkName = checkName;
        this.checkId = checkId;
        this.rawValue = rawValue;
        this.obfuscatedValue = obfuscatedValue;
        this.warningThreshold = warningThreshold;
        this.errorThreshold = errorThreshold;
    }

    public Result(String checkName, Long checkId, int rawValue, int obfuscatedValue, int warningThreshold, int errorThreshold) {
        this.checkName = checkName;
        this.checkId = checkId;
        this.rawValue = rawValue;
        this.obfuscatedValue = obfuscatedValue;
        this.warningThreshold = warningThreshold;
        this.errorThreshold = errorThreshold;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getCheckName() {
        return checkName;
    }

    public Long getCheckId() {
        return checkId;
    }

    public int getRawValue() {
        return rawValue;
    }

    public int getObfuscatedValue() {
        return obfuscatedValue;
    }

    public int getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(int warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public int getErrorThreshold() {
        return errorThreshold;
    }

    public void setErrorThreshold(int errorThreshold) {
        this.errorThreshold = errorThreshold;
    }
}
