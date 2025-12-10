package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.ReportService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportCreateRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.server.dataquality.event.ReportSubmittedEvent;
import eu.bbmri_eric.quality.server.user.AuthenticationContextService;
import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserRole;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing reports. */
@Service
@Transactional
class ReportServiceImpl implements ReportService {
  private final AgentRepository agentRepository;
  private final ReportRepository reportRepository;
  private final QualityCheckRepository qualityCheckRepository;
  private final AuthenticationContextService authenticationContextService;
  private final ApplicationEventPublisher eventPublisher;
  private final ModelMapper modelMapper;

  public ReportServiceImpl(
      AgentRepository agentRepository,
      ReportRepository reportRepository,
      QualityCheckRepository qualityCheckRepository,
      AuthenticationContextService authenticationContextService,
      ApplicationEventPublisher eventPublisher,
      ModelMapper modelMapper) {
    this.agentRepository = agentRepository;
    this.reportRepository = reportRepository;
    this.qualityCheckRepository = qualityCheckRepository;
    this.authenticationContextService = authenticationContextService;
    this.eventPublisher = eventPublisher;
    this.modelMapper = modelMapper;
  }

  @Override
  public ReportDTO create(String agentId, ReportCreateRequest createRequest) {
    verifyAuthorization(agentId);
    Report report = parseReportDTO(createRequest);
    saveReport(agentId, report);
    return convertToDTO(report);
  }

  private void saveReport(String agentId, Report report) {
    Agent agent =
        agentRepository.findById(agentId).orElseThrow(() -> new EntityNotFoundException(agentId));
    agent.addReport(report);
    agentRepository.saveAndFlush(agent);
    eventPublisher.publishEvent(new ReportSubmittedEvent(this, agentId, report.getId()));
  }

  private @NonNull Report parseReportDTO(ReportCreateRequest createRequest) {
    Report report = new Report();
    for (QualityCheckResultDTO resultDTO : createRequest.getResults()) {
      QualityCheck qualityCheck =
          qualityCheckRepository
              .findById(resultDTO.getHash())
              .orElseGet(
                  () -> {
                    QualityCheck newCheck =
                        new QualityCheck(resultDTO.getHash(), resultDTO.getName(), "");
                    return qualityCheckRepository.save(newCheck);
                  });

      report.addQualityCheckResult(qualityCheck, resultDTO.getResult());
    }
    return report;
  }

  private void verifyAuthorization(String agentId) {
    UserDTO currentUser = authenticationContextService.getCurrentUser();
    if (!isAuthorizedToCreateReport(currentUser, agentId)) {
      throw new AccessDeniedException(
          "User is not authorized to create reports for agent: " + agentId);
    }
  }

  private boolean isAuthorizedToCreateReport(UserDTO user, String agentId) {
    boolean isAdmin = user.getRoles() != null && user.getRoles().contains(UserRole.ADMIN);
    boolean isLinkedToAgent = agentId.equals(user.getAgentId());
    return isAdmin || isLinkedToAgent;
  }

  @Override
  @Transactional(readOnly = true)
  public ReportDTO findById(String id) {
    Report report =
        reportRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Report with ID %s not found".formatted(id)));
    return convertToDTO(report);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReportDTO> findByAgentId(String agentId) {
    return agentRepository
        .findById(agentId)
        .orElseThrow(() -> new EntityNotFoundException(agentId))
        .getReports()
        .stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReportDTO> findAll() {
    return reportRepository.findAll().stream().map(this::convertToDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countAll() {
    return reportRepository.count();
  }

  private ReportDTO convertToDTO(Report report) {
    return modelMapper.map(report, ReportDTO.class);
  }
}
