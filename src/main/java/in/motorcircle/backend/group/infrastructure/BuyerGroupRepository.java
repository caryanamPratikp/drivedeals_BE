package in.motorcircle.backend.group.infrastructure;

import in.motorcircle.backend.group.domain.BuyerGroup;
import in.motorcircle.backend.group.domain.GroupStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuyerGroupRepository extends JpaRepository<BuyerGroup, String> {

    @Query("SELECT g FROM BuyerGroup g " +
           "JOIN FETCH g.car c " +
           "JOIN FETCH g.variant v " +
           "WHERE (:city IS NULL OR LOWER(g.city) = LOWER(:city)) " +
           "AND (:brand IS NULL OR LOWER(c.brand) = LOWER(:brand)) " +
           "AND (:stage IS NULL OR g.stage = :stage)")
    Page<BuyerGroup> searchGroups(
            @Param("city") String city,
            @Param("brand") String brand,
            @Param("stage") GroupStage stage,
            Pageable pageable
    );

    List<BuyerGroup> findByCarId(String carId);
    List<BuyerGroup> findByVariantId(String variantId);
    List<BuyerGroup> findByCityAndStage(String city, GroupStage stage);
}
