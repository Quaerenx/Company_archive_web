package com.company.filerepo;

public final class FileRepositoryEntry {
    private final boolean directory;
    private final String id;
    private final String name;
    private final String path;
    private final String lastModifiedText;
    private final long size;
    private final String sizeText;
    private final String icon;
    private final String description;

    public FileRepositoryEntry(boolean directory, String id, String name, String path,
            String lastModifiedText, long size, String sizeText, String icon, String description) {
        this.directory = directory;
        this.id = id;
        this.name = name;
        this.path = path;
        this.lastModifiedText = lastModifiedText;
        this.size = size;
        this.sizeText = sizeText;
        this.icon = icon;
        this.description = description;
    }

    public boolean isDirectory() { return directory; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getLastModifiedText() { return lastModifiedText; }
    public long getSize() { return size; }
    public String getSizeText() { return sizeText; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
}
