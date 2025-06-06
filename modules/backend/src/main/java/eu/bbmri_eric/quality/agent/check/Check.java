package eu.bbmri_eric.quality.agent.check;

interface Check {
    void execute();
    String getName();
    String getDescription();
}
