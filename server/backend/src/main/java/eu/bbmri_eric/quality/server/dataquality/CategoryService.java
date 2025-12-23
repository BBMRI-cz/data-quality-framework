package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.common.CRUDService;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryUpdateDTO;

public interface CategoryService
    extends CRUDService<CategoryDTO, CategoryCreateDTO, CategoryUpdateDTO, Long> {}
