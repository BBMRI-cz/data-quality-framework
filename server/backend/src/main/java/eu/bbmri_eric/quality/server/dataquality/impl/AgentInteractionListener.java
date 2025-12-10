package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.AgentInteractionType;
import eu.bbmri_eric.quality.server.dataquality.event.ReportSubmittedEvent;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AgentInteractionListener {
  private final AgentRepository agentRepository;

  AgentInteractionListener(AgentRepository agentRepository) {
    this.agentRepository = agentRepository;
  }

  @EventListener
  @Transactional
  protected void onReportSubmittedEvent(ReportSubmittedEvent event) {
    agentRepository
        .findById(event.getAgentId())
        .ifPresent(
            agent -> {
              agent.addInteraction(AgentInteractionType.REPORT);
              agentRepository.save(agent);
            });
  }
}
