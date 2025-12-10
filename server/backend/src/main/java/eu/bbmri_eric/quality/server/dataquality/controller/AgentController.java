package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistration;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
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
  public ResponseEntity<EntityModel<AgentRegistration>> create(
      @Valid @RequestBody AgentRegistrationRequest createAgentDto) {
    AgentRegistration createdAgentDto = agentService.create(createAgentDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(createdAgentDto));
  }

  @PatchMapping("/{id}")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<AgentDTO>> update(
      @PathVariable String id, @Valid @RequestBody AgentUpdateRequest updateRequest) {
    AgentDTO agent = agentService.update(updateRequest, id);
    EntityModel<AgentDTO> agentModel = linkBuilder.toModel(agent);
    return ResponseEntity.ok(agentModel);
  }

  @GetMapping("/{id}")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<AgentDTO>> findById(
      @PathVariable String id, @RequestParam(required = false) String expand) {
    boolean expandInteractions = "interactions".equals(expand);
    AgentDTO agent = agentService.findById(id, expandInteractions);
    EntityModel<AgentDTO> agentModel = linkBuilder.toModel(agent);
    return ResponseEntity.ok(agentModel);
  }

  @GetMapping
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<AgentDTO>>> listAll() {
    List<AgentDTO> agents = agentService.listAll();
    CollectionModel<EntityModel<AgentDTO>> agentsModel = linkBuilder.toCollectionModel(agents);
    return ResponseEntity.ok(agentsModel);
  }

  @DeleteMapping("/{id}")
  @SecurityRequirement(name = "bearerAuth")
  @Schema(name = "Delete an agent")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    agentService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
