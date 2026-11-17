package in.motorcircle.backend.lead.infrastructure;

import in.motorcircle.backend.lead.domain.LeadEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadEventRepository extends JpaRepository<LeadEvent, String> {

    List<LeadEvent> findByLeadIdOrderByCreatedAtDesc(String leadId);
}
