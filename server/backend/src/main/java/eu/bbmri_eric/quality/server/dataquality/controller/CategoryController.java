package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.CategoryService;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for managing categories. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Categories", description = "API for managing quality check categories")
class CategoryController {

  private final CategoryService categoryService;
  private final CategoryLinkBuilder linkBuilder;

  public CategoryController(CategoryService categoryService, CategoryLinkBuilder linkBuilder) {
    this.categoryService = categoryService;
    this.linkBuilder = linkBuilder;
  }

  @GetMapping("/categories/{id}")
  @Operation(
      summary = "Get category by ID",
      description = "Retrieves a specific category by its unique identifier")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<CategoryDTO>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(linkBuilder.toModel(categoryService.findById(id)));
  }

  @GetMapping("/categories")
  @Operation(summary = "Get all categories", description = "Retrieves all category definitions")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<CategoryDTO>>> findAll() {
    List<CategoryDTO> categories = categoryService.findAll();
    CollectionModel<EntityModel<CategoryDTO>> categoriesModel =
        linkBuilder.toCollectionModel(categories);
    return ResponseEntity.ok(categoriesModel);
  }

  @PostMapping("/categories")
  @Operation(summary = "Create category", description = "Creates a new category")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<CategoryDTO>> create(
      @Valid @RequestBody CategoryCreateDTO createDTO) {
    CategoryDTO createdCategory = categoryService.create(createDTO);
    EntityModel<CategoryDTO> categoryModel = linkBuilder.toModel(createdCategory);
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryModel);
  }

  @PutMapping("/categories/{id}")
  @Operation(summary = "Update category", description = "Updates an existing category")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<CategoryDTO>> update(
      @PathVariable Long id, @Valid @RequestBody CategoryUpdateDTO updateDTO) {
    CategoryDTO updatedCategory = categoryService.update(id, updateDTO);
    EntityModel<CategoryDTO> categoryModel = linkBuilder.toModel(updatedCategory);
    return ResponseEntity.ok(categoryModel);
  }

  @DeleteMapping("/categories/{id}")
  @Operation(summary = "Delete category", description = "Deletes a category by its ID")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    categoryService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
