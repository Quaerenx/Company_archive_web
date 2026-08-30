package com.company.mypage;

import java.util.List;
import java.util.Objects;

public final class WorkInbox {
    private static final WorkInbox EMPTY = new WorkInbox(List.of(), 0, 0, 0, 0);

    private final List<WorkInboxItem> items;
    private final int totalCount;
    private final int dangerCount;
    private final int warningCount;
    private final int infoCount;

    private WorkInbox(
            List<WorkInboxItem> items,
            int totalCount,
            int dangerCount,
            int warningCount,
            int infoCount) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.totalCount = totalCount;
        this.dangerCount = dangerCount;
        this.warningCount = warningCount;
        this.infoCount = infoCount;
    }

    public static WorkInbox empty() {
        return EMPTY;
    }

    public static WorkInbox of(List<WorkInboxItem> allItems, int displayLimit) {
        Objects.requireNonNull(allItems, "allItems");
        if (displayLimit <= 0) {
            throw new IllegalArgumentException("displayLimit must be positive");
        }
        int dangerCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        for (WorkInboxItem item : allItems) {
            switch (Objects.requireNonNull(item, "item").getSeverity()) {
                case DANGER -> dangerCount++;
                case WARNING -> warningCount++;
                case INFO -> infoCount++;
            }
        }
        int end = Math.min(allItems.size(), displayLimit);
        return new WorkInbox(
                allItems.subList(0, end),
                allItems.size(),
                dangerCount,
                warningCount,
                infoCount);
    }

    public List<WorkInboxItem> getItems() {
        return items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getDangerCount() {
        return dangerCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public int getHiddenCount() {
        return Math.max(0, totalCount - items.size());
    }
}
