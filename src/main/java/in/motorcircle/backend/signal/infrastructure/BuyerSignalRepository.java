package in.motorcircle.backend.signal.infrastructure;

import in.motorcircle.backend.signal.domain.BuyerSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerSignalRepository extends JpaRepository<BuyerSignal, String> {
    List<BuyerSignal> findByGroupId(String groupId);
    Optional<BuyerSignal> findByGroupIdAndUserId(String groupId, String userId);
}
