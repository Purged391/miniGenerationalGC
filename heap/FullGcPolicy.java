package heap;

import exceptions.MetricsException;
import records.HeapMetricsRecord;

final class FullGcPolicy {
    private final double threshold;

    public FullGcPolicy(double threshold) {
        if(!Double.isFinite(threshold) || threshold > 100 || threshold < 0){
            throw new MetricsException("Threshols must be a percentage");
        }
        this.threshold = threshold;
    }

    public boolean shouldRecommendFullGc(HeapMetricsRecord metrics) {
        return metrics.oldOccupancyPercentage() >= this.threshold;
    }
}
