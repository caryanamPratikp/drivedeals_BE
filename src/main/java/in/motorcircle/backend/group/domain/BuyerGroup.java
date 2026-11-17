package in.motorcircle.backend.group.domain;

import in.motorcircle.backend.catalog.domain.Car;
import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.identity.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "buyer_groups")
public class BuyerGroup {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private CarVariant variant;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "target_members", nullable = false)
    private int targetMembers;

    @Column(name = "current_members", nullable = false)
    private int currentMembers = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30, nullable = false)
    private GroupStage stage = GroupStage.FORMATION;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BuyerGroup() {
    }

    public BuyerGroup(String id, Car car, CarVariant variant, String city, int targetMembers, Instant expiresAt, User createdBy) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.car = car;
        this.variant = variant;
        this.city = city;
        this.targetMembers = targetMembers;
        this.currentMembers = 0;
        this.stage = GroupStage.FORMATION;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.version = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
        if (this.stage == null) {
            this.stage = GroupStage.FORMATION;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void incrementMembers() {
        this.currentMembers++;
        // State transition trigger: If threshold reached in FORMATION stage, open for dealer quotes
        if (this.stage == GroupStage.FORMATION && this.currentMembers >= this.targetMembers) {
            this.stage = GroupStage.OPEN_FOR_QUOTES;
        }
    }

    public void decrementMembers() {
        if (this.currentMembers > 0) {
            this.currentMembers--;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public CarVariant getVariant() {
        return variant;
    }

    public void setVariant(CarVariant variant) {
        this.variant = variant;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getTargetMembers() {
        return targetMembers;
    }

    public void setTargetMembers(int targetMembers) {
        this.targetMembers = targetMembers;
    }

    public int getCurrentMembers() {
        return currentMembers;
    }

    public void setCurrentMembers(int currentMembers) {
        this.currentMembers = currentMembers;
    }

    public GroupStage getStage() {
        return stage;
    }

    public void setStage(GroupStage stage) {
        this.stage = stage;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuyerGroup that = (BuyerGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
