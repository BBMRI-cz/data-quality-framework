package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportFilterDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportResultDetailDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.event.NewReportEvent;
import eu.bbmri_eric.quality.agent.dataquality.exception.ReportGenerationInProgressException;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
class ReportServiceImpl implements ReportService {

  private final ReportRepository reportRepository;
  private final SettingsService settingsService;
  private final ModelMapper modelMapper;
  private final EventPublisher publisher;
  private final StaleReportReaper staleReportReaper;
  private final ObfuscatedReportAssembler obfuscatedReportAssembler;

  ReportServiceImpl(
      ReportRepository reportRepository,
      SettingsService settingsService,
      ModelMapper modelMapper,
      EventPublisher publisher,
      StaleReportReaper staleReportReaper,
      ObfuscatedReportAssembler obfuscatedReportAssembler) {
    this.reportRepository = reportRepository;
    this.settingsService = settingsService;
    this.modelMapper = modelMapper;
    this.publisher = publisher;
    this.staleReportReaper = staleReportReaper;
    this.obfuscatedReportAssembler = obfuscatedReportAssembler;
  }

  @Override
  @Transactional
  @Audited(
      action = AuditAction.REPORT_CREATED,
      module = "dataquality",
      entityId = "#result.id",
      details = "Report created")
  public ReportDTO create(ReportCreateDTO createDTO) {
    staleReportReaper.failStale();
    List<Report> blockingReports = staleReportReaper.activeGeneratingReports();
    if (!blockingReports.isEmpty()) {
      throw new ReportGenerationInProgressException(blockingReports.getFirst().getId());
    }
    Report report = new Report();
    report.setEpsilonBudget(settingsService.getSettings().getEpsilon());
    report = reportRepository.save(report);
    log.info("Created report with id: {}", report.getId());
    publisher.publishEvent(new NewReportEvent(report.getId()));
    return modelMapper.map(report, ReportDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public ReportDTO findById(Long id) {
    return reportRepository
        .findById(id)
        .map(
            report -> {
              ReportDTO dto = modelMapper.map(report, ReportDTO.class);
              dto.setResults(
                  report.getResults() == null
                      ? null
                      : report.getResults().stream()
                          .map(result -> modelMapper.map(result, ReportResultDetailDTO.class))
                          .collect(Collectors.toList()));
              return dto;
            })
        .orElseThrow(() -> new EntityNotFoundException("Report not found with id: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return reportRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReportDTO> findAll() {
    return reportRepository.findAll().stream()
        .map(report -> modelMapper.map(report, ReportDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ReportDTO> findAll(ReportFilterDTO filter) {
    Sort sort =
        Sort.by(
            filter.getOrder() == FilterDTO.SortOrder.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
            filter.getSort());
    PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize(), sort);
    Page<Report> page = reportRepository.findAll(pageRequest);

    List<ReportDTO> content =
        page.getContent().stream().map(report -> modelMapper.map(report, ReportDTO.class)).toList();

    return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  @Override
  @Transactional
  public ReportDTO update(Long id, ReportUpdateDTO updateDTO) {
    Report report =
        reportRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Report not found with id: " + id));

    if (updateDTO.getStatus() != null) {
      // TODO: Refactor
      if (updateDTO.getStatus() == ReportStatus.GENERATING) {
        throw new IllegalStateException("Report status GENERATING is pipeline-managed");
      }
      if (report.getStatus() == ReportStatus.FAILED
          && updateDTO.getStatus() != ReportStatus.FAILED) {
        throw new IllegalStateException("A failed report cannot be revived");
      }
      if (updateDTO.getStatus() == ReportStatus.GENERATED) {
        report.markGenerated();
      } else if (updateDTO.getStatus() == ReportStatus.FAILED) {
        report.markFailed();
      }
    }

    report = reportRepository.save(report);
    log.info("Updated report {} - status: {}", id, report.getStatus());
    return modelMapper.map(report, ReportDTO.class);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Report report =
        reportRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Report not found with id: " + id));
    reportRepository.delete(report);
    log.info("Deleted report with id: {}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(Long id) {
    return reportRepository.existsById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public ObfuscatedReportDTO getObfuscatedById(Long id) {
    Report report =
        reportRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Report not found with id: " + id));
    return obfuscatedReportAssembler.assemble(report);
  }
}
