package com.company.filerepo;

import java.util.List;

public final class FileRepositoryListing {
    private final String currentPath;
    private final String parentPath;
    private final List<Breadcrumb> breadcrumbs;
    private final List<FileRepositoryEntry> entries;
    private final int directoryCount;
    private final int fileCount;
    private final String totalSizeText;

    public FileRepositoryListing(String currentPath, String parentPath, List<Breadcrumb> breadcrumbs,
            List<FileRepositoryEntry> entries, int directoryCount, int fileCount, String totalSizeText) {
        this.currentPath = currentPath;
        this.parentPath = parentPath;
        this.breadcrumbs = List.copyOf(breadcrumbs);
        this.entries = List.copyOf(entries);
        this.directoryCount = directoryCount;
        this.fileCount = fileCount;
        this.totalSizeText = totalSizeText;
    }

    public String getCurrentPath() { return currentPath; }
    public String getParentPath() { return parentPath; }
    public List<Breadcrumb> getBreadcrumbs() { return breadcrumbs; }
    public List<FileRepositoryEntry> getEntries() { return entries; }
    public int getDirectoryCount() { return directoryCount; }
    public int getFileCount() { return fileCount; }
    public String getTotalSizeText() { return totalSizeText; }

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
