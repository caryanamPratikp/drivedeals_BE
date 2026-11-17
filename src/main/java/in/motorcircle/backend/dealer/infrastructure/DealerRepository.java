package in.motorcircle.backend.dealer.infrastructure;

import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.dealer.domain.DealerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, String> {
    Optional<Dealer> findByUserId(String userId);
    Optional<Dealer> findByGstin(String gstin);
    List<Dealer> findByStatus(DealerStatus status);
    List<Dealer> findByBrandAndCityAndStatus(String brand, String city, DealerStatus status);
    boolean existsByGstin(String gstin);
}
