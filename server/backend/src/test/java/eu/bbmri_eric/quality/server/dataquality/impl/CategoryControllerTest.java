package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.CategoryUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerTest {

  private static final String API_V1_CATEGORIES = "/api/v1/categories";
  private static final String API_V1_CATEGORIES_ID = "/api/v1/categories/{id}";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CategoryRepository categoryRepository;

  private Category testCategory;

  @BeforeEach
  void setUp() {
    categoryRepository.deleteAll();
    testCategory = new Category("Data Completeness", "#FF5733");
    testCategory = categoryRepository.save(testCategory);
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnCategoryWithHateoasLinksWhenExists() throws Exception {
    mockMvc
        .perform(get(API_V1_CATEGORIES_ID, testCategory.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId()))
        .andExpect(jsonPath("$.name").value("Data Completeness"))
        .andExpect(jsonPath("$.colorHex").value("#FF5733"))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/categories/" + testCategory.getId()))
        .andExpect(
            jsonPath("$._links.categories.href").value("http://localhost/api/v1/categories"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findById_shouldReturnCategoryForAdmin() throws Exception {
    mockMvc
        .perform(get(API_V1_CATEGORIES_ID, testCategory.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId()));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;

    mockMvc.perform(get(API_V1_CATEGORIES_ID, nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  void findById_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(get(API_V1_CATEGORIES_ID, testCategory.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnEmptyListWithHateoasLinksWhenNoCategories() throws Exception {
    categoryRepository.deleteAll();

    mockMvc
        .perform(get(API_V1_CATEGORIES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/categories"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnAllCategoriesWithHateoasLinks() throws Exception {
    Category secondCategory = new Category("Data Accuracy", "#33A1FF");
    categoryRepository.save(secondCategory);

    mockMvc
        .perform(get(API_V1_CATEGORIES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.categories").isArray())
        .andExpect(jsonPath("$._embedded.categories.length()").value(2))
        .andExpect(jsonPath("$._embedded.categories[?(@.name == 'Data Completeness')]").exists())
        .andExpect(jsonPath("$._embedded.categories[?(@.name == 'Data Accuracy')]").exists())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/categories"))
        .andExpect(jsonPath("$._embedded.categories[0]._links.self.href").exists())
        .andExpect(jsonPath("$._embedded.categories[1]._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findAll_shouldReturnAllCategoriesForAdmin() throws Exception {
    mockMvc
        .perform(get(API_V1_CATEGORIES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.categories").isArray())
        .andExpect(jsonPath("$._embedded.categories.length()").value(1));
  }

  @Test
  void findAll_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get(API_V1_CATEGORIES)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldCreateCategoryAndReturnCreatedStatusWithHateoasLinks() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("Data Timeliness", "#00FF00");

    mockMvc
        .perform(
            post(API_V1_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Data Timeliness"))
        .andExpect(jsonPath("$.colorHex").value("#00FF00"))
        .andExpect(jsonPath("$._links.self.href").exists())
        .andExpect(
            jsonPath("$._links.categories.href").value("http://localhost/api/v1/categories"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForDuplicateName() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("Data Completeness", "#AABBCC");

    mockMvc
        .perform(
            post(API_V1_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForInvalidColorHex() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("Valid Name", "invalid-color");

    mockMvc
        .perform(
            post(API_V1_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForEmptyName() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("", "#FF5733");

    mockMvc
        .perform(
            post(API_V1_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldAcceptNullColorHex() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("No Color Category", null);

    mockMvc
        .perform(
            post(API_V1_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("No Color Category"))
        .andExpect(jsonPath("$.colorHex").isEmpty());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void create_shouldReturnForbiddenForNonAdminUser() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("Data Timeliness", "#00FF00");

    mockMvc
        .perform(
            post(API_V1_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void create_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    CategoryCreateDTO createDTO = new CategoryCreateDTO("Data Timeliness", "#00FF00");

    mockMvc
        .perform(
            post(API_V1_CATEGORIES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldUpdateCategoryAndReturnHateoasResponse() throws Exception {
    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Updated Completeness", "#AABBCC");

    mockMvc
        .perform(
            put(API_V1_CATEGORIES_ID, testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testCategory.getId()))
        .andExpect(jsonPath("$.name").value("Updated Completeness"))
        .andExpect(jsonPath("$.colorHex").value("#AABBCC"))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/categories/" + testCategory.getId()))
        .andExpect(
            jsonPath("$._links.categories.href").value("http://localhost/api/v1/categories"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;
    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Updated Name", "#AABBCC");

    mockMvc
        .perform(
            put(API_V1_CATEGORIES_ID, nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnConflictWhenNameAlreadyExists() throws Exception {
    Category anotherCategory = new Category("Data Accuracy", "#123456");
    categoryRepository.save(anotherCategory);

    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Data Accuracy", "#AABBCC");

    mockMvc
        .perform(
            put(API_V1_CATEGORIES_ID, testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnBadRequestForInvalidData() throws Exception {
    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("", "#AABBCC");

    mockMvc
        .perform(
            put(API_V1_CATEGORIES_ID, testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldReturnBadRequestForInvalidColorHex() throws Exception {
    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Valid Name", "not-a-color");

    mockMvc
        .perform(
            put(API_V1_CATEGORIES_ID, testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void update_shouldReturnForbiddenForNonAdminUser() throws Exception {
    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Updated Name", "#AABBCC");

    mockMvc
        .perform(
            put(API_V1_CATEGORIES_ID, testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    CategoryUpdateDTO updateDTO = new CategoryUpdateDTO("Updated Name", "#AABBCC");

    mockMvc
        .perform(
            put(API_V1_CATEGORIES_ID, testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_shouldDeleteCategoryAndReturnNoContent() throws Exception {
    mockMvc
        .perform(delete(API_V1_CATEGORIES_ID, testCategory.getId()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get(API_V1_CATEGORIES_ID, testCategory.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;

    mockMvc.perform(delete(API_V1_CATEGORIES_ID, nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void delete_shouldReturnForbiddenForNonAdminUser() throws Exception {
    mockMvc
        .perform(delete(API_V1_CATEGORIES_ID, testCategory.getId()))
        .andExpect(status().isForbidden());
  }

  @Test
  void delete_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(delete(API_V1_CATEGORIES_ID, testCategory.getId()))
        .andExpect(status().isUnauthorized());
  }
}
