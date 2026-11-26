package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistration;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationResult;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentUpdateRequest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agents")
class AgentController {

  private final AgentService agentService;
  private final AgentLinkBuilder linkBuilder;

  public AgentController(AgentService agentService, AgentLinkBuilder linkBuilder) {
    this.agentService = agentService;
    this.linkBuilder = linkBuilder;
  }

  @PostMapping
  @Schema(name = "Register an agent")
  @SecurityRequirements
  @ApiResponse(
      responseCode = "201",
      description = "Agent registered",
      content = @Content(schema = @Schema(implementation = AgentRegistration.class)))
  @ApiResponse(
      responseCode = "200",
      description =
          "Existing agent re-registered from its registered IP address; new credentials issued,"
              + " previous ones invalidated",
      content = @Content(schema = @Schema(implementation = AgentRegistration.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Agent ID already registered from a different IP address",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "429",
      description = "IP address already registered by a different agent",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ResponseEntity<EntityModel<AgentRegistration>> create(
      @Valid @RequestBody AgentRegistrationRequest createAgentDto, HttpServletRequest request) {
    String ipAddress = getClientIpAddress(request);
    AgentRegistrationResult result = agentService.create(createAgentDto, ipAddress);
    return ResponseEntity.status(result.reRegistered() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(EntityModel.of(result.registration()));
  }

  private String getClientIpAddress(HttpServletRequest request) {
    return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
  }

  @PatchMapping("/{id}")
  public ResponseEntity<EntityModel<AgentDTO>> update(
      @PathVariable String id, @Valid @RequestBody AgentUpdateRequest updateRequest) {
    AgentDTO agent = agentService.update(updateRequest, id);
    EntityModel<AgentDTO> agentModel = linkBuilder.toModel(agent);
    return ResponseEntity.ok(agentModel);
  }

  @GetMapping("/{id}")
  public ResponseEntity<EntityModel<AgentDTO>> findById(
      @PathVariable String id, @RequestParam(required = false) String expand) {
    boolean expandInteractions = "interactions".equals(expand);
    AgentDTO agent = agentService.findById(id, expandInteractions);
    EntityModel<AgentDTO> agentModel = linkBuilder.toModel(agent);
    return ResponseEntity.ok(agentModel);
  }

  @GetMapping
  public ResponseEntity<CollectionModel<EntityModel<AgentDTO>>> listAll() {
    List<AgentDTO> agents = agentService.listAll();
    CollectionModel<EntityModel<AgentDTO>> agentsModel = linkBuilder.toCollectionModel(agents);
    return ResponseEntity.ok(agentsModel);
  }

  @DeleteMapping("/{id}")
  @Schema(name = "Delete an agent")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    agentService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
