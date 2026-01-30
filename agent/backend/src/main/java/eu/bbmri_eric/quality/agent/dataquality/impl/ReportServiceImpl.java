package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException;
import eu.bbmri_eric.quality.agent.dataquality.CQLQueryService;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.CQLQueryDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.ReportNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReportServiceImpl implements ReportService {

  private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);
  private final ReportRepository reportRepository;
  private final ReportEventHandler reportRestEventHandler;
  private final CQLQueryService cqlQueryService;
  private final ModelMapper modelMapper;

  ReportServiceImpl(
      ReportRepository reportRepository,
      ReportEventHandler reportRestEventHandler,
      CQLQueryService cqlQueryService,
      ModelMapper modelMapper) {
    this.reportRepository = reportRepository;
    this.reportRestEventHandler = reportRestEventHandler;
    this.cqlQueryService = cqlQueryService;
    this.modelMapper = modelMapper;
  }

  @Transactional
  public void generateReport() {
    Report report = reportRepository.save(new Report());
    reportRestEventHandler.onAfterCreate(report);
    log.info("📊 Scheduled report created with ID: {}", report.getId());
  }

  @Override
  @Transactional
  public ReportDTO create(ReportCreateDTO createDTO) {
    Report report = new Report();
    // Potentially map fields from createDTO if any
    report = reportRepository.save(report);
    reportRestEventHandler.onAfterCreate(report);
    log.info("📊 Report created via API with ID: {}", report.getId());
    return modelMapper.map(report, ReportDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public ReportDTO findById(Long id) {
    return reportRepository
        .findById(id)
        .map(report -> modelMapper.map(report, ReportDTO.class))
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
  public PageResponse<ReportDTO> findAll(FilterDTO filter) {
    Sort sort =
        Sort.by(
            filter.getOrder() == null || filter.getOrder().name().equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC,
            filter.getSort() != null ? filter.getSort() : "id");
    PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize(), sort);
    Page<Report> page = reportRepository.findAll(pageRequest);
    List<ReportDTO> content =
        page.getContent().stream()
            .map(report -> modelMapper.map(report, ReportDTO.class))
            .collect(Collectors.toList());
    return new PageResponse<>(
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast(),
        page.hasNext(),
        page.hasPrevious());
  }

  @Override
  @Transactional
  public ReportDTO update(Long id, ReportUpdateDTO updateDTO) {
    Report report =
        reportRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Report not found with id: " + id));

    if (updateDTO.getStatus() != null) {
      report.setStatus(updateDTO.getStatus());
    }

    report = reportRepository.save(report);
    return modelMapper.map(report, ReportDTO.class);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    if (!reportRepository.existsById(id)) {
      throw new EntityNotFoundException("Report not found with id: " + id);
    }
    reportRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(Long id) {
    return reportRepository.existsById(id);
  }

  @Transactional(readOnly = true)
  @Override
  public ObfuscatedReportDTO getObfuscatedById(Long id) {
    Report report =
        reportRepository.findById(id).orElseThrow(() -> new ReportNotFoundException(id));
    List<CQLQueryDTO> cqlQueryDTOS = cqlQueryService.findAll();
    var results =
        report.getResults().stream()
            .map(
                result -> {
                  double value;
                  if (report.getNumberOfEntities() == 0) {
                    // Handle edge case: 0/0 = NaN, return 0.0 instead
                    value = 0.0;
                  } else {
                    value = result.getObfuscatedValue() / report.getNumberOfEntities();
                  }
                  double roundedValue =
                      BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
                  double boundedValue = Math.min(1.0, Math.max(0.0, roundedValue));
                  String checkIdLabel = formatCheckIdWithStratum(result, cqlQueryDTOS);
                  return new QualityCheckResultDTO(
                      checkIdLabel, result.getCheckName(), boundedValue);
                })
            .collect(Collectors.toList());
    return new ObfuscatedReportDTO(
        results, report.getNumberOfEntities(), report.getNumberOfSecondaryEntities());
  }

  private static String getCheckId(Result result, List<CQLQueryDTO> cqlQueryDTOS) {
    String query =
        cqlQueryDTOS.stream()
            .filter(cqlQueryDTO -> cqlQueryDTO.getId().equals(result.getCheckId()))
            .findFirst()
            .map(CQLQueryDTO::getQuery)
            .orElse(result.getCheckId().toString());
    return hashQuery(query);
  }

  private static String formatCheckIdWithStratum(Result result, List<CQLQueryDTO> cqlQueryDTOS) {
    String checkId = getCheckId(result, cqlQueryDTOS);
    if (result.getStratum() != null) {
      return "%s (%s)".formatted(checkId, result.getStratum());
    }
    return checkId;
  }

  private static String hashQuery(String query) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }
}
