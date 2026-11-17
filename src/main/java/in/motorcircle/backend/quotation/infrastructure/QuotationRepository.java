package in.motorcircle.backend.quotation.infrastructure;

import in.motorcircle.backend.quotation.domain.Quotation;
import in.motorcircle.backend.quotation.domain.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, String> {

    @Query("SELECT q FROM Quotation q " +
           "JOIN FETCH q.dealer d " +
           "WHERE q.group.id = :groupId " +
           "AND q.status != 'WITHDRAWN'")
    List<Quotation> findActiveQuotesForGroup(@Param("groupId") String groupId);

    Optional<Quotation> findByGroupIdAndDealerId(String groupId, String dealerId);
    boolean existsByGroupIdAndDealerId(String groupId, String dealerId);
    List<Quotation> findByDealerId(String dealerId);
}
