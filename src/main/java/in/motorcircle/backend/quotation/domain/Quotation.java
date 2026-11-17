package in.motorcircle.backend.quotation.domain;

import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.group.domain.BuyerGroup;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "quotations")
public class Quotation {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private BuyerGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(name = "current_version_id", length = 36)
    private String currentVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private QuotationStatus status = QuotationStatus.SUBMITTED;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 0;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("versionNumber ASC")
    private List<QuotationVersion> versions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Quotation() {
    }

    public Quotation(String id, BuyerGroup group, Dealer dealer) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.group = group;
        this.dealer = dealer;
        this.status = QuotationStatus.SUBMITTED;
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
        if (this.status == null) {
            this.status = QuotationStatus.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addVersion(QuotationVersion qv) {
        versions.add(qv);
        qv.setQuotation(this);
        this.currentVersionId = qv.getId();
        if (qv.getVersionNumber() > 1) {
            this.status = QuotationStatus.REVISED;
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

    public Dealer getDealer() {
        return dealer;
    }

    public void setDealer(Dealer dealer) {
        this.dealer = dealer;
    }

    public String getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(String currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public List<QuotationVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<QuotationVersion> versions) {
        this.versions = versions;
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
        Quotation that = (Quotation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
