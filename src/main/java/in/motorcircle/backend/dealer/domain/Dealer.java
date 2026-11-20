package in.motorcircle.backend.dealer.domain;

import in.motorcircle.backend.identity.domain.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "dealers")
public class Dealer {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(name = "dealership_name", length = 191, nullable = false)
    private String dealershipName;

    @Column(name = "legal_entity_name", length = 191)
    private String legalEntityName;

    @Column(name = "brand", length = 100, nullable = false)
    private String brand;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "address", columnDefinition = "TEXT", nullable = false)
    private String address;

    @Column(name = "gstin", length = 15, unique = true)
    private String gstin;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "dealer_code", length = 50)
    private String dealerCode;

    @Column(name = "dealership_type", length = 50)
    private String dealershipType;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_email", length = 191)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DealerStatus status;

    @Column(name = "onboarding_status", length = 30, nullable = false)
    private String onboardingStatus = "NOT_STARTED";

    @Column(name = "rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal rating;

    @Column(name = "total_quotes", nullable = false)
    private int totalQuotes;

    @Column(name = "total_leads_delivered", nullable = false)
    private int totalLeadsDelivered;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Dealer() {
    }

    public Dealer(String id, User user, String dealershipName, String brand, String city, String address, String gstin) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.user = user;
        this.dealershipName = dealershipName;
        this.brand = brand;
        this.city = city;
        this.address = address;
        this.gstin = gstin;
        this.status = DealerStatus.PENDING_VERIFICATION;
        this.rating = new BigDecimal("5.00");
        this.totalQuotes = 0;
        this.totalLeadsDelivered = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.rating == null) {
            this.rating = new BigDecimal("5.00");
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDealershipName() {
        return dealershipName;
    }

    public void setDealershipName(String dealershipName) {
        this.dealershipName = dealershipName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public DealerStatus getStatus() {
        return status;
    }

    public void setStatus(DealerStatus status) {
        this.status = status;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public int getTotalQuotes() {
        return totalQuotes;
    }

    public void setTotalQuotes(int totalQuotes) {
        this.totalQuotes = totalQuotes;
    }

    public int getTotalLeadsDelivered() {
        return totalLeadsDelivered;
    }

    public void setTotalLeadsDelivered(int totalLeadsDelivered) {
        this.totalLeadsDelivered = totalLeadsDelivered;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getDealerCode() {
        return dealerCode;
    }

    public void setDealerCode(String dealerCode) {
        this.dealerCode = dealerCode;
    }

    public String getDealershipType() {
        return dealershipType;
    }

    public void setDealershipType(String dealershipType) {
        this.dealershipType = dealershipType;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getOnboardingStatus() {
        return onboardingStatus;
    }

    public void setOnboardingStatus(String onboardingStatus) {
        this.onboardingStatus = onboardingStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dealer dealer = (Dealer) o;
        return Objects.equals(id, dealer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
