package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin")
class CategoryIntegrationTests {

  private static final String API_CATEGORIES = "/api/categories";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CategoryRepository categoryRepository;

  @BeforeEach
  void setUp() {
    categoryRepository.deleteAll();
  }

  @Test
  void create_validCategory_returnsCreatedWithLocation() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("Data Completeness", "#FF5733");

    String location =
        mockMvc
            .perform(
                post(API_CATEGORIES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Data Completeness"))
            .andExpect(jsonPath("$.colorHex").value("#FF5733"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();
    assertThat(categoryRepository.count()).isEqualTo(1);
  }

  @Test
  void create_nullColorHex_returnsCreatedWithNullColor() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("No Color Category", null);

    mockMvc
        .perform(
            post(API_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("No Color Category"))
        .andExpect(jsonPath("$.colorHex").isEmpty());
  }

  @Test
  void create_invalidColorHex_returnsBadRequest() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("Valid Name", "not-a-color");

    mockMvc
        .perform(
            post(API_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void create_blankName_returnsBadRequest() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("", "#FF5733");

    mockMvc
        .perform(
            post(API_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findById_existingCategory_returnsOk() throws Exception {
    Category savedCategory = categoryRepository.save(new Category("Test Category", "#FF5733"));

    mockMvc
        .perform(get(API_CATEGORIES + "/{id}", savedCategory.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedCategory.getId()))
        .andExpect(jsonPath("$.name").value("Test Category"))
        .andExpect(jsonPath("$.colorHex").value("#FF5733"));
  }

  @Test
  void findById_nonExistingCategory_returnsNotFound() throws Exception {
    mockMvc.perform(get(API_CATEGORIES + "/{id}", 99999L)).andExpect(status().isNotFound());
  }

  @Test
  void findById_malformedId_returnsBadRequest() throws Exception {
    mockMvc.perform(get(API_CATEGORIES + "/abc")).andExpect(status().isBadRequest());
  }

  @Test
  void findAll_returnsEmbeddedList() throws Exception {
    categoryRepository.save(new Category("Category 1", "#FF5733"));
    categoryRepository.save(new Category("Category 2", "#33A1FF"));

    mockMvc
        .perform(get(API_CATEGORIES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.categories").exists())
        .andExpect(jsonPath("$._embedded.categories.length()").value(2));
  }

  @Test
  void update_existingCategory_returnsOkWithUpdatedData() throws Exception {
    Category savedCategory = categoryRepository.save(new Category("Original Name", "#FF5733"));

    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Updated Name", "#33A1FF");

    mockMvc
        .perform(
            put(API_CATEGORIES + "/{id}", savedCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedCategory.getId()))
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.colorHex").value("#33A1FF"));

    Category updatedCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();
    assertThat(updatedCategory.getName()).isEqualTo("Updated Name");
    assertThat(updatedCategory.getColorHex()).isEqualTo("#33A1FF");
  }

  @Test
  void update_nonExistingCategory_returnsNotFound() throws Exception {
    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Updated Name", "#33A1FF");

    mockMvc
        .perform(
            put(API_CATEGORIES + "/{id}", 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  void update_existingNameToAnotherExistingName_returnsConflict() throws Exception {
    Category firstCategory = categoryRepository.save(new Category("Category 1", "#FF5733"));
    categoryRepository.save(new Category("Category 2", "#33A1FF"));

    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Category 2", "#AABBCC");

    mockMvc
        .perform(
            put(API_CATEGORIES + "/{id}", firstCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isConflict());
  }

  @Test
  void update_blankName_returnsBadRequest() throws Exception {
    Category savedCategory = categoryRepository.save(new Category("Original Name", "#FF5733"));

    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("", "#33A1FF");

    mockMvc
        .perform(
            put(API_CATEGORIES + "/{id}", savedCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void delete_existingCategory_returnsNoContent() throws Exception {
    Category savedCategory = categoryRepository.save(new Category("Delete Test", "#FF5733"));

    mockMvc
        .perform(delete(API_CATEGORIES + "/{id}", savedCategory.getId()))
        .andExpect(status().isNoContent());

    assertThat(categoryRepository.existsById(savedCategory.getId())).isFalse();
  }

  @Test
  void delete_nonExistingCategory_returnsNotFound() throws Exception {
    mockMvc.perform(delete(API_CATEGORIES + "/{id}", 99999L)).andExpect(status().isNotFound());
  }

  @Test
  void create_duplicateName_returnsConflict() throws Exception {
    categoryRepository.save(new Category("Duplicate Category", "#FF5733"));

    CategoryCreateDTO createDTO = new CategoryCreateDTO("Duplicate Category", "#33A1FF");

    mockMvc
        .perform(
            post(API_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isConflict());
  }
}
