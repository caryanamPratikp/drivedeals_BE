package in.motorcircle.backend.lead.infrastructure;

import in.motorcircle.backend.lead.domain.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, String> {

    Optional<Consent> findByUserIdAndQuotationId(String userId, String quotationId);

    List<Consent> findByUserIdOrderByGrantedAtDesc(String userId);

    List<Consent> findByDealerIdOrderByGrantedAtDesc(String dealerId);
}
