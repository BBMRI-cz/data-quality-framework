package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import eu.bbmri_eric.quality.server.dataquality.ReportService;
import eu.bbmri_eric.quality.server.dataquality.domain.Agent;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportCreateRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.server.dataquality.event.ReportSubmittedEvent;
import eu.bbmri_eric.quality.server.setting.SettingService;
import eu.bbmri_eric.quality.server.user.AuthenticationContextService;
import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserRole;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing reports. */
@Service
@Transactional
class ReportServiceImpl implements ReportService {
  private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);
  private static final int DEFAULT_RETENTION = 3;

  private final AgentRepository agentRepository;
  private final ReportRepository reportRepository;
  private final QualityCheckRepository qualityCheckRepository;
  private final AuthenticationContextService authenticationContextService;
  private final ApplicationEventPublisher eventPublisher;
  private final ModelMapper modelMapper;
  private final SettingService settingService;

  public ReportServiceImpl(
      AgentRepository agentRepository,
      ReportRepository reportRepository,
      QualityCheckRepository qualityCheckRepository,
      AuthenticationContextService authenticationContextService,
      ApplicationEventPublisher eventPublisher,
      ModelMapper modelMapper,
      SettingService settingService) {
    this.agentRepository = agentRepository;
    this.reportRepository = reportRepository;
    this.qualityCheckRepository = qualityCheckRepository;
    this.authenticationContextService = authenticationContextService;
    this.eventPublisher = eventPublisher;
    this.modelMapper = modelMapper;
    this.settingService = settingService;
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
    applyRetentionPolicy(agent);
  }

  private void applyRetentionPolicy(Agent agent) {
    int retention = getReportRetention();
    int maxReports = retention + 2; // oldest + latest + n additional latest reports

    List<Report> reports =
        agent.getReports().stream().sorted(Comparator.comparing(Report::getTimestamp)).toList();

    if (reports.size() <= maxReports) {
      return;
    }

    int deleteCount = reports.size() - maxReports;
    List<Report> toDelete = new ArrayList<>();
    for (int i = 1; i <= deleteCount; i++) {
      toDelete.add(reports.get(i));
    }

    toDelete.forEach(agent::removeReport);
    reportRepository.deleteAll(toDelete);

    logger.info(
        "Applied retention policy for agent {}: deleted {} reports, keeping {} reports",
        agent.getId(),
        deleteCount,
        maxReports);
  }

  private int getReportRetention() {
    var settings = settingService.getSettings();
    if (settings != null && settings.getReportRetention() != null) {
      return settings.getReportRetention();
    }
    return DEFAULT_RETENTION;
  }

  private @NonNull Report parseReportDTO(ReportCreateRequest createRequest) {
    Report report = new Report();
    report.setTotalPatients(createRequest.getTotalPatients());
    report.setTotalSamples(createRequest.getTotalSamples());

    for (QualityCheckResultDTO resultDTO : createRequest.getResults()) {
      QualityCheckVersion version = resolveVersion(resultDTO.getHash(), resultDTO.getName());
      report.addQualityCheckResult(version, resultDTO.getResult());
    }
    return report;
  }

  private QualityCheckVersion resolveVersion(String hash, String name) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findByVersions_Hash(hash)
            .orElseGet(
                () -> {
                  QualityCheck newCheck = new QualityCheck(hash, name, "");
                  QualityCheckVersion newVersion = new QualityCheckVersion(newCheck, 1, "", hash);
                  newCheck.addVersion(newVersion);
                  return qualityCheckRepository.save(newCheck);
                });
    return qualityCheck.getVersions().stream()
        .filter(version -> version.getHash().equals(hash))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No version with hash '%s' found for quality check".formatted(hash)));
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
  public PageResponse<ReportDTO> findByAgentId(String agentId, FilterDTO filter) {
    if (!agentRepository.existsById(agentId)) {
      throw new EntityNotFoundException(agentId);
    }

    FilterDTO normalizedFilter = normalizeFilter(filter);
    Sort.Direction direction =
        normalizedFilter.getOrder() == FilterDTO.SortOrder.DESC
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

    Sort sort = Sort.by(direction, normalizedFilter.getSort());
    PageRequest pageRequest =
        PageRequest.of(normalizedFilter.getPage(), normalizedFilter.getSize(), sort);
    Page<Report> page = reportRepository.findByAgentId(agentId, pageRequest);

    List<ReportDTO> content = page.getContent().stream().map(this::convertToDTO).toList();
    return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReportDTO> findAll() {
    return reportRepository.findAll().stream().map(this::convertToDTO).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ReportDTO> findAll(FilterDTO filter) {
    FilterDTO normalizedFilter = normalizeFilter(filter);
    Sort.Direction direction =
        normalizedFilter.getOrder() == FilterDTO.SortOrder.DESC
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

    Sort sort = Sort.by(direction, normalizedFilter.getSort());
    PageRequest pageRequest =
        PageRequest.of(normalizedFilter.getPage(), normalizedFilter.getSize(), sort);
    Page<Report> page = reportRepository.findAll(pageRequest);

    List<ReportDTO> content = page.getContent().stream().map(this::convertToDTO).toList();
    return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public long countAll() {
    return reportRepository.count();
  }

  private ReportDTO convertToDTO(Report report) {
    return modelMapper.map(report, ReportDTO.class);
  }

  private FilterDTO normalizeFilter(FilterDTO filter) {
    if (filter.getOrder() == null) {
      filter.setOrder(FilterDTO.SortOrder.ASC);
    }

    if (filter.getSort() == null || filter.getSort().isBlank()) {
      filter.setSort("timestamp");
      filter.setOrder(FilterDTO.SortOrder.DESC);
    }

    return filter;
  }
}
