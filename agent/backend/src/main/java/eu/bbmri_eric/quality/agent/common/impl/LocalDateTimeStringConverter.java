package eu.bbmri_eric.quality.agent.common.impl;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
class LocalDateTimeStringConverter implements AttributeConverter<LocalDateTime, String> {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public String convertToDatabaseColumn(LocalDateTime attribute) {
    return (attribute == null) ? null : attribute.format(FORMATTER);
  }

  @Override
  public LocalDateTime convertToEntityAttribute(String dbData) {
    return (dbData == null) ? null : LocalDateTime.parse(dbData, FORMATTER);
  }
}
