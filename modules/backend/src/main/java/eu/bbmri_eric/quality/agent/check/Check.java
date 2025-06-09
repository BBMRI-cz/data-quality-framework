package eu.bbmri_eric.quality.agent.check;

interface Check {
  Result execute();

  String getName();

  String getDescription();
}
