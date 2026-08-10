package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing Category entities. */
@Repository
interface CategoryRepository extends JpaRepository<Category, Long> {

  /**
   * Checks if a category with the given name exists.
   *
   * @param name the name to check
   * @return true if a category with the name exists, false otherwise
   */
  boolean existsByName(String name);

  /**
   * Finds a category by name.
   *
   * @param name the name to search for
   * @return an Optional containing the category if found
   */
  Optional<Category> findByName(String name);
}
