package eu.bbmri_eric.quality.server.dataquality.controller;

import eu.bbmri_eric.quality.server.dataquality.GroupService;
import eu.bbmri_eric.quality.server.dataquality.dto.AssignAgentsDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupUpdateDTO;
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

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Groups", description = "API for managing agent groups")
class GroupController {

  private final GroupService groupService;
  private final GroupLinkBuilder linkBuilder;

  public GroupController(GroupService groupService, GroupLinkBuilder linkBuilder) {
    this.groupService = groupService;
    this.linkBuilder = linkBuilder;
  }

  @GetMapping("/groups/{id}")
  @Operation(
      summary = "Get group by ID",
      description = "Retrieves a specific group by its unique identifier")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<GroupDTO>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(linkBuilder.toModel(groupService.findById(id)));
  }

  @GetMapping("/groups")
  @Operation(summary = "Get all groups", description = "Retrieves all group definitions")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<CollectionModel<EntityModel<GroupDTO>>> findAll() {
    List<GroupDTO> groups = groupService.findAll();
    CollectionModel<EntityModel<GroupDTO>> groupsModel = linkBuilder.toCollectionModel(groups);
    return ResponseEntity.ok(groupsModel);
  }

  @PostMapping("/groups")
  @Operation(summary = "Create group", description = "Creates a new group")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<GroupDTO>> create(
      @Valid @RequestBody GroupCreateDTO createDTO) {
    GroupDTO createdGroup = groupService.create(createDTO);
    EntityModel<GroupDTO> groupModel = linkBuilder.toModel(createdGroup);
    return ResponseEntity.status(HttpStatus.CREATED).body(groupModel);
  }

  @PutMapping("/groups/{id}")
  @Operation(summary = "Update group", description = "Updates an existing group")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<GroupDTO>> update(
      @PathVariable Long id, @Valid @RequestBody GroupUpdateDTO updateDTO) {
    GroupDTO updatedGroup = groupService.update(id, updateDTO);
    EntityModel<GroupDTO> groupModel = linkBuilder.toModel(updatedGroup);
    return ResponseEntity.ok(groupModel);
  }

  @PutMapping("/groups/{id}/agents")
  @Operation(
      summary = "Assign agents to group",
      description = "Assigns a list of agents to the specified group")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EntityModel<GroupDTO>> assignAgents(
      @PathVariable Long id, @Valid @RequestBody AssignAgentsDTO assignAgentsDTO) {
    GroupDTO updatedGroup = groupService.assignAgents(id, assignAgentsDTO.getAgentIds());
    EntityModel<GroupDTO> groupModel = linkBuilder.toModel(updatedGroup);
    return ResponseEntity.ok(groupModel);
  }

  @DeleteMapping("/groups/{id}")
  @Operation(summary = "Delete group", description = "Deletes a group by its ID")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    groupService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
