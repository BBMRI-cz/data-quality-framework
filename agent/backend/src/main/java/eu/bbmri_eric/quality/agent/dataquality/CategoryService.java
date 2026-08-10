package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.common.CRUDService;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryUpdateDTO;

/** Service interface for managing categories. */
public interface CategoryService
    extends CRUDService<CategoryDTO, CategoryCreateDTO, CategoryUpdateDTO, Long> {}
