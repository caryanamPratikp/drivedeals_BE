package in.motorcircle.backend.signal.domain;

import in.motorcircle.backend.group.domain.BuyerGroup;
import in.motorcircle.backend.identity.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "buyer_signals")
public class BuyerSignal {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private BuyerGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "timeline_months", length = 30, nullable = false)
    private TimelineMonths timelineMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_mode", length = 30, nullable = false)
    private PurchaseMode purchaseMode;

    @Column(name = "exchange_car", nullable = false)
    private boolean exchangeCar = false;

    @Column(name = "exchange_car_details", length = 255)
    private String exchangeCarDetails;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BuyerSignal() {
    }

    public BuyerSignal(String id, BuyerGroup group, User user, TimelineMonths timelineMonths, PurchaseMode purchaseMode, boolean exchangeCar, String exchangeCarDetails, String notes) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.group = group;
        this.user = user;
        this.timelineMonths = timelineMonths;
        this.purchaseMode = purchaseMode;
        this.exchangeCar = exchangeCar;
        this.exchangeCarDetails = exchangeCarDetails;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BuyerGroup getGroup() {
        return group;
    }

    public void setGroup(BuyerGroup group) {
        this.group = group;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TimelineMonths getTimelineMonths() {
        return timelineMonths;
    }

    public void setTimelineMonths(TimelineMonths timelineMonths) {
        this.timelineMonths = timelineMonths;
    }

    public PurchaseMode getPurchaseMode() {
        return purchaseMode;
    }

    public void setPurchaseMode(PurchaseMode purchaseMode) {
        this.purchaseMode = purchaseMode;
    }

    public boolean isExchangeCar() {
        return exchangeCar;
    }

    public void setExchangeCar(boolean exchangeCar) {
        this.exchangeCar = exchangeCar;
    }

    public String getExchangeCarDetails() {
        return exchangeCarDetails;
    }

    public void setExchangeCarDetails(String exchangeCarDetails) {
        this.exchangeCarDetails = exchangeCarDetails;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuyerSignal that = (BuyerSignal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
