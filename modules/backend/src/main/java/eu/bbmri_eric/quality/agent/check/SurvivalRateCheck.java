package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurvivalRateCheck implements CheckWithStratification {
    private final String name;
    private final String description;
    private final List<String> genders;
    private final int warningThreshold;
    private final int errorThreshold;

    public SurvivalRateCheck() {
        this.name = "Survival rate, stratified per gender value";
        this.description = "What is the survival rate for different gender values";
        this.genders = Arrays.asList("male", "female");
        this.warningThreshold = 10; // Example threshold, adjust as needed
        this.errorThreshold = 20;   // Example threshold, adjust as needed
    }

    @Override
    public Result execute(FHIRStore fhirStore) {
        try {
            List<Resource> patients = fhirStore.fetchAllResources("Patient", List.of("id", "gender", "deceased"));
            int totalAlive = 0;
            int totalCount = 0;

            for (Resource resource : patients) {
                Patient patient = (Patient) resource;
                boolean deceased = patient.hasDeceased()
                        || patient.hasDeceasedDateTimeType();
                if (!deceased) {
                    totalAlive++;
                }
                totalCount++;
            }

            return new Result(totalAlive, "Patient");
        } catch (Exception e) {
            System.err.println("Error processing " + name + ": " + e.getMessage());
            return new Result(e.getMessage());
        }
    }

    @Override
    public Map<String, Result> executeWithStratification(FHIRStore fhirStore) {
    Map<String, Result> results = new HashMap<>();
        try {
            List<Resource> patients = fhirStore.fetchAllResources("Patient", List.of("id", "gender", "deceased"));
            // Stratified by gender
            for (String gender : genders) {
                int genderAlive = getGenderAlive(gender, patients);

                results.put(gender ,new Result(genderAlive, "Patient"));
            }
            return results;
        } catch (Exception e) {
            System.err.println("Error processing stratified " + name + ": " + e.getMessage());
            results.put("error",new Result(e.getMessage()));
            return results;
        }
    }

    private static int getGenderAlive(String gender, List<Resource> patients) {
        int genderAlive = 0;
        int genderCount = 0;

        for (Resource resource : patients) {
            Patient patient = (Patient) resource;
            if (gender.equalsIgnoreCase(patient.getGender() != null ? patient.getGender().toCode() : "")) {
                boolean deceased = patient.hasDeceased()
                        || patient.hasDeceasedDateTimeType();
                if (!deceased) {
                    genderAlive++;
                }
                genderCount++;
            }
        }
        return genderAlive;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getWarningThreshold() {
        return warningThreshold;
    }

    @Override
    public int getErrorThreshold() {
        return errorThreshold;
    }

    @Override
    public float getEpsilonBudget() {
        return 0.3f; // No differential privacy used
    }

    @Override
    public Long getId() {
        return 3L; // Example ID, adjust as needed
    }
}