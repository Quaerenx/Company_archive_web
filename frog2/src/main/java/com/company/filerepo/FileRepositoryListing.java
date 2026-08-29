package com.company.filerepo;

import java.util.List;

public final class FileRepositoryListing {
    private final String currentPath;
    private final String parentPath;
    private final List<Breadcrumb> breadcrumbs;
    private final List<FileRepositoryEntry> entries;
    private final int directoryCount;
    private final int fileCount;
    private final int invalidEntryCount;
    private final String totalSizeText;
    private final String previousCursor;
    private final String nextCursor;
    private final boolean hasPrevious;
    private final boolean hasNext;
    private final int currentPage;
    private final int totalPages;
    private final int totalCount;
    private final int pageSize;

    public FileRepositoryListing(String currentPath, String parentPath, List<Breadcrumb> breadcrumbs,
            List<FileRepositoryEntry> entries, int directoryCount, int fileCount,
            int invalidEntryCount, String totalSizeText,
            String previousCursor, String nextCursor,
            boolean hasPrevious, boolean hasNext, int currentPage, int totalPages,
            int totalCount, int pageSize) {
        this.currentPath = currentPath;
        this.parentPath = parentPath;
        this.breadcrumbs = List.copyOf(breadcrumbs);
        this.entries = List.copyOf(entries);
        this.directoryCount = directoryCount;
        this.fileCount = fileCount;
        this.invalidEntryCount = invalidEntryCount;
        this.totalSizeText = totalSizeText;
        this.previousCursor = previousCursor;
        this.nextCursor = nextCursor;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
    }

    public String getCurrentPath() { return currentPath; }
    public String getParentPath() { return parentPath; }
    public List<Breadcrumb> getBreadcrumbs() { return breadcrumbs; }
    public List<FileRepositoryEntry> getEntries() { return entries; }
    public int getDirectoryCount() { return directoryCount; }
    public int getFileCount() { return fileCount; }
    public int getInvalidEntryCount() { return invalidEntryCount; }
    public String getTotalSizeText() { return totalSizeText; }
    public String getPreviousCursor() { return previousCursor; }
    public String getNextCursor() { return nextCursor; }
    public boolean isHasPrevious() { return hasPrevious; }
    public boolean isHasNext() { return hasNext; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public int getTotalCount() { return totalCount; }
    public int getPageSize() { return pageSize; }

    public static final class Breadcrumb {
        private final String name;
        private final String path;

        public Breadcrumb(String name, String path) {
            this.name = name;
            this.path = path;
        }

        public String getName() { return name; }
        public String getPath() { return path; }
    }
}
