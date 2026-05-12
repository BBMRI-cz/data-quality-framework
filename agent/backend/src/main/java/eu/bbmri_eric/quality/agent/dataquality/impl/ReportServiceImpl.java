package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.event.NewReportEvent;
import eu.bbmri_eric.quality.agent.dataquality.exception.ReportNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReportServiceImpl implements ReportService {

  private final ReportRepository reportRepository;
  private final QualityCheckService qualityCheckService;
  private final ModelMapper modelMapper;
  private final EventPublisher publisher;

  ReportServiceImpl(
      ReportRepository reportRepository,
      QualityCheckService qualityCheckService,
      ModelMapper modelMapper,
      EventPublisher publisher) {
    this.reportRepository = reportRepository;
    this.qualityCheckService = qualityCheckService;
    this.modelMapper = modelMapper;
    this.publisher = publisher;
  }

  @Override
  @Transactional
  public ReportDTO create(ReportCreateDTO createDTO) {
    Report report = new Report();
    report = reportRepository.save(report);
    publisher.publishEvent(new NewReportEvent(report.getId()));
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
    Sort.Direction direction =
        filter.getOrder() == null || filter.getOrder().name().equalsIgnoreCase("ASC")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    String sortProperty = filter.getSort();
    if (sortProperty == null) {
      sortProperty = "generatedAt";
      direction = Sort.Direction.DESC;
    }

    Sort sort = Sort.by(direction, sortProperty);
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
    List<QualityCheckDTO> qualityCheckDTOS = qualityCheckService.findAll();
    var results =
        (report.getResults() == null ? List.<Result>of() : report.getResults())
            .stream()
                .map(
                    result -> {
                      Double value = calculateObfuscatedValue(result, report.getNumberOfEntities());
                      Double boundedValue = null;
                      if (value != null) {
                        double roundedValue =
                            BigDecimal.valueOf(value)
                                .setScale(2, RoundingMode.HALF_UP)
                                .doubleValue();
                        boundedValue = Math.clamp(roundedValue, 0.0, 1.0);
                      }
                      String checkIdLabel = formatCheckIdWithStratum(result, qualityCheckDTOS);
                      return new QualityCheckResultDTO(
                          checkIdLabel, result.getCheckName(), boundedValue);
                    })
                .collect(Collectors.toList());
    return new ObfuscatedReportDTO(
        results, report.getNumberOfEntities(), report.getNumberOfSecondaryEntities());
  }

  private static Double calculateObfuscatedValue(Result result, Integer numberOfEntities) {
    if (Objects.isNull(numberOfEntities) || Objects.isNull(result.getObfuscatedValue())) {
      return null;
    }
    if (numberOfEntities == 0) {
      return 0.0;
    }
    return result.getObfuscatedValue() / numberOfEntities;
  }

  private static String getCheckId(Result result, List<QualityCheckDTO> cqlQueryDTOS) {
    String query =
        cqlQueryDTOS.stream()
            .filter(cqlQueryDTO -> cqlQueryDTO.getId().equals(result.getCheckId()))
            .findFirst()
            .map(QualityCheckDTO::getQuery)
            .orElse(result.getCheckId().toString());
    return hashQuery(query);
  }

  private static String formatCheckIdWithStratum(
      Result result, List<QualityCheckDTO> qualityCheckDTOS) {
    String checkId = getCheckId(result, qualityCheckDTOS);
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
