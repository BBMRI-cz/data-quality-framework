package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import eu.bbmri_eric.quality.server.dataquality.CategoryService;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryUpdateDTO;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing categories. */
@Service
@Transactional
class CategoryServiceImpl implements CategoryService {

  private final CategoryRepository categoryRepository;
  private final ModelMapper modelMapper;

  public CategoryServiceImpl(CategoryRepository categoryRepository, ModelMapper modelMapper) {
    this.categoryRepository = categoryRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public CategoryDTO create(CategoryCreateDTO categoryCreateDTO) {
    Objects.requireNonNull(categoryCreateDTO, "CategoryCreateDTO cannot be null");

    if (categoryRepository.existsByName(categoryCreateDTO.getName())) {
      throw new EntityAlreadyExistsException(
          "Category with name '" + categoryCreateDTO.getName() + "' already exists");
    }

    Category category = new Category(categoryCreateDTO.getName(), categoryCreateDTO.getColorHex());
    Category savedCategory = categoryRepository.save(category);
    return modelMapper.map(savedCategory, CategoryDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CategoryDTO> findById(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    return categoryRepository
        .findById(id)
        .map(category -> modelMapper.map(category, CategoryDTO.class));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryDTO> findAll() {
    return categoryRepository.findAll().stream()
        .map(category -> modelMapper.map(category, CategoryDTO.class))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<CategoryDTO> findAll(FilterDTO filter) {
    throw new UnsupportedOperationException("Filtered pagination not yet implemented");
  }

  @Override
  public CategoryDTO update(Long id, CategoryUpdateDTO categoryUpdateDTO) {
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(categoryUpdateDTO, "CategoryUpdateDTO cannot be null");

    Category category =
        categoryRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Category not found with ID: " + id));

    // Check if name is being changed and if new name already exists
    if (!category.getName().equals(categoryUpdateDTO.getName())
        && categoryRepository.existsByName(categoryUpdateDTO.getName())) {
      throw new EntityAlreadyExistsException(
          "Category with name '" + categoryUpdateDTO.getName() + "' already exists");
    }

    category.setName(categoryUpdateDTO.getName());
    category.setColorHex(categoryUpdateDTO.getColorHex());

    Category updatedCategory = categoryRepository.save(category);
    return modelMapper.map(updatedCategory, CategoryDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return categoryRepository.count();
  }

  @Override
  public void delete(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    if (!categoryRepository.existsById(id)) {
      throw new EntityNotFoundException("Category not found with ID: " + id);
    }
    categoryRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    return categoryRepository.existsById(id);
  }
}
