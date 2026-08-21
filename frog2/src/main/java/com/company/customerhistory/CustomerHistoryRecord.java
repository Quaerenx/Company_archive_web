package com.company.customerhistory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public final class CustomerHistoryRecord {
    private final String id;
    private final String customerName;
    private final LocalDate workDate;
    private final CustomerHistoryCategory category;
    private final String title;
    private final String actionSummary;
    private final CustomerHistoryStatus status;
    private final String creatorUserId;
    private final String creatorName;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CustomerHistoryRecord(
            String id,
            String customerName,
            LocalDate workDate,
            CustomerHistoryCategory category,
            String title,
            String actionSummary,
            CustomerHistoryStatus status,
            String creatorUserId,
            String creatorName,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerName = Objects.requireNonNull(customerName, "customerName");
        this.workDate = Objects.requireNonNull(workDate, "workDate");
        this.category = Objects.requireNonNull(category, "category");
        this.title = Objects.requireNonNull(title, "title");
        this.actionSummary = Objects.requireNonNull(actionSummary, "actionSummary");
        this.status = Objects.requireNonNull(status, "status");
        this.creatorUserId = Objects.requireNonNull(creatorUserId, "creatorUserId");
        this.creatorName = Objects.requireNonNull(creatorName, "creatorName");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public CustomerHistoryCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getActionSummary() {
        return actionSummary;
    }

    public CustomerHistoryStatus getStatus() {
        return status;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOwnedBy(String userId) {
        return userId != null && creatorUserId.equals(userId);
    }
}
