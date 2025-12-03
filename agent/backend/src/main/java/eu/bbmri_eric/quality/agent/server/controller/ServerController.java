package eu.bbmri_eric.quality.agent.server.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.quality.agent.server.ServerService;
import eu.bbmri_eric.quality.agent.server.dto.DetailedServerDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerCreateDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerUpdateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servers")
@Tag(name = "Server Management", description = "APIs for managing servers")
class ServerController {

  private final ServerService serverService;
  private final ServerLinkBuilder linkBuilder;

  public ServerController(ServerService serverService, ServerLinkBuilder linkBuilder) {
    this.serverService = serverService;
    this.linkBuilder = linkBuilder;
  }

  @GetMapping
  @Operation(summary = "List all servers", description = "Retrieves a list of all servers")
  public ResponseEntity<CollectionModel<EntityModel<ServerDto>>> findAll() {
    List<ServerDto> servers = serverService.findAll();
    CollectionModel<EntityModel<ServerDto>> serversModel = linkBuilder.toCollectionModel(servers);
    return ResponseEntity.ok(serversModel);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get server by ID",
      description = "Retrieves detailed information about a specific server")
  public ResponseEntity<EntityModel<DetailedServerDto>> findById(
      @Parameter(description = "Server ID", required = true) @PathVariable String id) {
    DetailedServerDto server = serverService.findById(id);
    EntityModel<DetailedServerDto> serverModel = linkBuilder.toModel(server);
    return ResponseEntity.ok(serverModel);
  }

  @PostMapping
  @Operation(
      summary = "Create a new server",
      description = "Creates a new server with the provided information")
  public ResponseEntity<EntityModel<ServerDto>> create(
      @Parameter(description = "Server creation data", required = true) @Valid @RequestBody
          ServerCreateDto createDto) {
    ServerDto createdServer = serverService.create(createDto);
    EntityModel<ServerDto> serverModel = linkBuilder.toModel(createdServer);

    // Create the Location header for the newly created server
    URI location = linkTo(methodOn(ServerController.class).findById(createdServer.getId())).toUri();

    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(serverModel);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update server",
      description = "Updates an existing server with the provided information")
  public ResponseEntity<EntityModel<ServerDto>> update(
      @Parameter(description = "Server ID", required = true) @PathVariable String id,
      @Parameter(description = "Server update data", required = true) @Valid @RequestBody
          ServerUpdateDto updateDto) {
    ServerDto updatedServer = serverService.update(id, updateDto);
    EntityModel<ServerDto> serverModel = linkBuilder.toModel(updatedServer);
    return ResponseEntity.ok(serverModel);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete server", description = "Deletes a server by its ID")
  public ResponseEntity<Void> deleteById(
      @Parameter(description = "Server ID", required = true) @PathVariable String id) {
    serverService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
