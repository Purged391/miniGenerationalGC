package records;

public record HeapMetricsRecord(
    double oldOccupancyPercentage,
    long allocationVolume,
    int completedYoungGC,
    double promotionPercentage
) {
    
}
