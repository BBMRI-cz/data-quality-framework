package eu.bbmri_eric.quality.agent.check;

record Result(Integer numberOfEntities, String entityType, String error) {
  Result(Integer numberOfEntities, String entityType) {
    this(numberOfEntities, entityType, null);
  }

   Result(String error) {
    this(-1, "", error);
  }
}
