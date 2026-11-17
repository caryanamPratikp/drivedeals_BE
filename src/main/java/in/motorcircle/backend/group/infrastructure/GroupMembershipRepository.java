package in.motorcircle.backend.group.infrastructure;

import in.motorcircle.backend.group.domain.GroupMembership;
import in.motorcircle.backend.group.domain.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, String> {
    List<GroupMembership> findByGroupIdAndStatus(String groupId, MembershipStatus status);
    List<GroupMembership> findByUserIdAndStatus(String userId, MembershipStatus status);
    Optional<GroupMembership> findByGroupIdAndUserIdAndStatus(String groupId, String userId, MembershipStatus status);
    boolean existsByGroupIdAndUserIdAndStatus(String groupId, String userId, MembershipStatus status);
    long countByGroupIdAndStatus(String groupId, MembershipStatus status);
}
