package eu.bbmri_eric.quality.agent.check;

public record Result(Integer numberOfEntities, String entityType, String error) {
    public Result(Integer numberOfEntities, String entityType){
        this(numberOfEntities,entityType,null);
    }
    public Result(String error){
        this(-1,"",error);
    }

    @Override
    public Integer numberOfEntities() {
        return numberOfEntities;
    }

    @Override
    public String entityType() {
        return entityType;
    }

    @Override
    public String error() {
        return error;
    }
}
