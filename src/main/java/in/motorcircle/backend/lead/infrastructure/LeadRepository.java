package in.motorcircle.backend.lead.infrastructure;

import in.motorcircle.backend.lead.domain.Lead;
import in.motorcircle.backend.lead.domain.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {

    Optional<Lead> findByConsentId(String consentId);

    Optional<Lead> findByIdAndDealerId(String id, String dealerId);

    Page<Lead> findByDealerId(String dealerId, Pageable pageable);

    Page<Lead> findByDealerIdAndStatus(String dealerId, LeadStatus status, Pageable pageable);

    boolean existsByBuyerIdAndDealerIdAndQuotationIdAndStatusNot(
            String buyerId,
            String dealerId,
            String quotationId,
            LeadStatus status
    );

    List<Lead> findByBuyerIdOrderByCreatedAtDesc(String buyerId);
}
