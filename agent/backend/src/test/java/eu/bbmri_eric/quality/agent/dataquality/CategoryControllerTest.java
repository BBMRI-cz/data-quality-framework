package eu.bbmri_eric.quality.agent.dataquality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryCreateDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {

  public static final String CategoryEndpoint = "/api/categories";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @WithUserDetails("admin")
  void post_validCategory_createdAndRetrievable() throws Exception {
    CategoryCreateDTO category = new CategoryCreateDTO("Test Category", "#FF5733");

    String location =
        mockMvc
            .perform(
                post(CategoryEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(category)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Category"))
            .andExpect(jsonPath("$.colorHex").value("#FF5733"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();

    mockMvc
        .perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Category"))
        .andExpect(jsonPath("$.colorHex").value("#FF5733"));
  }

  @Test
  @WithUserDetails("admin")
  void put_existingCategory_updatedSuccessfully() throws Exception {
    CategoryCreateDTO category = new CategoryCreateDTO("UpdateCategory", "#FF5733");

    String location =
        mockMvc
            .perform(
                post(CategoryEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(category)))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();

    category.setName("Updated Category");
    category.setColorHex("#33A1FF");

    mockMvc
        .perform(
            put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Category"))
        .andExpect(jsonPath("$.colorHex").value("#33A1FF"));

    mockMvc
        .perform(get(location))
        .andExpect(jsonPath("$.name").value("Updated Category"))
        .andExpect(jsonPath("$.colorHex").value("#33A1FF"));
  }

  @Test
  @WithUserDetails("admin")
  void delete_existingCategory_deletedSuccessfully() throws Exception {
    CategoryCreateDTO category = new CategoryCreateDTO("DeleteCategory", "#FF5733");

    String location =
        mockMvc
            .perform(
                post(CategoryEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(category)))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(location).isNotNull();

    mockMvc.perform(delete(location)).andExpect(status().isNoContent());

    mockMvc.perform(get(location)).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void post_invalidCategory_missingName_returnsBadRequest() throws Exception {
    String invalidJson = "{\"colorHex\": \"#FF5733\"}";
    mockMvc
        .perform(
            post(CategoryEndpoint).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void post_invalidCategory_invalidColor_returnsBadRequest() throws Exception {
    CategoryCreateDTO category = new CategoryCreateDTO("Valid Name", "invalid-color");
    mockMvc
        .perform(
            post(CategoryEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void put_nonExistingCategory_returnsNotFound() throws Exception {
    CategoryCreateDTO category = new CategoryCreateDTO("Nonexistent", "#FF5733");

    mockMvc
        .perform(
            put(CategoryEndpoint + "/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void delete_nonExistingCategory_returnsNotFound() throws Exception {
    mockMvc.perform(delete(CategoryEndpoint + "/9999")).andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  void get_malformedId_returnsBadRequest() throws Exception {
    mockMvc.perform(get(CategoryEndpoint + "/abc")).andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  void post_duplicateCategoryName_returnsConflict() throws Exception {
    CategoryCreateDTO category = new CategoryCreateDTO("Duplicate Category", "#FF5733");

    mockMvc
        .perform(
            post(CategoryEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(CategoryEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)))
        .andExpect(status().isConflict());
  }
}
