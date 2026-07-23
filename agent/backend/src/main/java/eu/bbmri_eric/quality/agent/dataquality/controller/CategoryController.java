package eu.bbmri_eric.quality.agent.dataquality.controller;

import eu.bbmri_eric.quality.agent.dataquality.CategoryService;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** REST controller for managing categories. */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "API for managing quality check categories")
class CategoryController {

  private final CategoryService categoryService;
  private final CategoryLinkBuilder linkBuilder;

  CategoryController(CategoryService categoryService, CategoryLinkBuilder linkBuilder) {
    this.categoryService = categoryService;
    this.linkBuilder = linkBuilder;
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get category by ID",
      description = "Retrieves a specific category by its unique identifier")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found")
      })
  public ResponseEntity<EntityModel<CategoryDTO>> findById(
      @Parameter(description = "Category ID") @PathVariable Long id) {
    return ResponseEntity.ok(linkBuilder.toModel(categoryService.findById(id)));
  }

  @GetMapping
  @Operation(summary = "Get all categories", description = "Retrieves all category definitions")
  public ResponseEntity<CollectionModel<EntityModel<CategoryDTO>>> findAll() {
    List<CategoryDTO> categories = categoryService.findAll();
    CollectionModel<EntityModel<CategoryDTO>> categoriesModel =
        linkBuilder.toCollectionModel(categories);
    return ResponseEntity.ok(categoriesModel);
  }

  @PostMapping
  @Operation(summary = "Create category", description = "Creates a new category")
  public ResponseEntity<EntityModel<CategoryDTO>> create(
      @RequestBody @Valid CategoryCreateDTO createDTO) {
    CategoryDTO createdCategory = categoryService.create(createDTO);
    EntityModel<CategoryDTO> categoryModel = linkBuilder.toModel(createdCategory);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(createdCategory.getId())
            .toUri();
    return ResponseEntity.created(location).body(categoryModel);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update category", description = "Updates an existing category")
  public ResponseEntity<EntityModel<CategoryDTO>> update(
      @Parameter(description = "Category ID") @PathVariable Long id,
      @RequestBody @Valid CategoryUpdateDTO updateDTO) {
    CategoryDTO updatedCategory = categoryService.update(id, updateDTO);
    return ResponseEntity.ok(linkBuilder.toModel(updatedCategory));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete category", description = "Deletes a category by its ID")
  public ResponseEntity<Void> delete(
      @Parameter(description = "Category ID") @PathVariable Long id) {
    categoryService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
