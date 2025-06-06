package eu.bbmri_eric.quality.agent.check;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
class CQLCheck implements Check {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String description;
    private String query;

    protected CQLCheck() {
    }

    CQLCheck(Long id, String name, String description, String query) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.query = query;
    }

    @Override
    public void execute() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
