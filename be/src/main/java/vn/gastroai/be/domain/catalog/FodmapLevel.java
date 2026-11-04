package vn.gastroai.be.domain.catalog;

public enum FodmapLevel {

    UNKNOWN("Chưa có dữ liệu", 0),
    LOW("Thấp", 1),
    MEDIUM("Trung bình", 2),
    HIGH("Cao", 3);

    private final String label;
    private final int severity;

    FodmapLevel(String label, int severity) {
        this.label = label;
        this.severity = severity;
    }

    public String getLabel() {
        return label;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isHigherThan(FodmapLevel other) {
        return other == null || this.severity > other.severity;
    }

    public boolean isStorable() {
        return this != UNKNOWN;
    }
}