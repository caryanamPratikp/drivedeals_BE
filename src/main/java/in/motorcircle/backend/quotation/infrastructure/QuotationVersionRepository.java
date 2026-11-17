package in.motorcircle.backend.quotation.infrastructure;

import in.motorcircle.backend.quotation.domain.QuotationVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationVersionRepository extends JpaRepository<QuotationVersion, String> {
    List<QuotationVersion> findByQuotationIdOrderByVersionNumberDesc(String quotationId);
    Optional<QuotationVersion> findByQuotationIdAndVersionNumber(String quotationId, int versionNumber);
}
