package heap;

import java.util.List;

import records.HeapMetricsRecord;
import records.ObjectOperationRecord;

final class HeapMetrics {
    private final HeapStorage storage;

    public HeapMetrics(HeapStorage heapStorage) {
        this.storage = heapStorage;
    }

    HeapMetricsRecord getHeapMetricsRecord(List<ObjectOperationRecord> plan, long promotedUnits, long allocationVolume,
            int youngCollectsCompleted) {
        double oldOccupancyPercentage = this.storage.getOldOccupancyPercentage();
        long survivorYoungUnits = plan.stream()
                .mapToLong(record -> storage.findObject(record.id()).getSize())
                .sum();
        double promortionPercentage = survivorYoungUnits != 0
                ? (double) promotedUnits / survivorYoungUnits * 100
                : 0;
        return new HeapMetricsRecord(oldOccupancyPercentage, allocationVolume, youngCollectsCompleted,
                promortionPercentage);

    }
}
