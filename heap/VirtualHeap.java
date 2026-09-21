package heap;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import config.VirtualHeapConfig;
import enums.SourceSurvivorEnum;
import exceptions.VirtualHeapException;
import objects.SimObject;
import records.GCReportRecord;
import records.HeapMetricsRecord;
import records.HeapStatusRecord;
import records.ObjectOperationRecord;

public class VirtualHeap {
    private static final double POLICY_THRESHOLD = 70;
    private final HeapStorage storage;
    private final Set<Long> roots;
    private final Set<Long> rememberedSet;
    private final ReachabilityAnalyzer reachability;
    private final YoungGcPlanner youngGcPlanner;
    private final RememberedSetManager rememberedSetManager;
    private final GcReportBuilder reportBuilder;
    private final HeapMetrics heapMetrics;
    private final FullGcPolicy fullGcPolicy;
    private SourceSurvivorEnum sourceSurvivor = SourceSurvivorEnum.S0;
    private long nextId;
    private GCReportRecord lastGcReportRecord;
    private long allocationVolume;
    private int youngCollectsCompleted;
    private HeapMetricsRecord heapMetricsRecord;

    public VirtualHeap(VirtualHeapConfig config) {
        if (config == null) {
            throw new VirtualHeapException("Null config");
        }
        this.roots = new HashSet<>();
        this.rememberedSet = new HashSet<>();
        this.storage = new HeapStorage(config.getEdenCapacity(), config.getSurvivorCapacity(), config.getOldCapacity());
        this.reachability = new ReachabilityAnalyzer(storage);
        this.youngGcPlanner = new YoungGcPlanner(storage, config);
        this.rememberedSetManager = new RememberedSetManager(storage, rememberedSet);
        this.reportBuilder = new GcReportBuilder(storage);
        this.heapMetrics = new HeapMetrics(storage);
        this.fullGcPolicy = new FullGcPolicy(POLICY_THRESHOLD);
    }

    public Optional<GCReportRecord> getLastGcReport() {
        return Optional.ofNullable(lastGcReportRecord);
    }

    public Optional<HeapMetricsRecord> getHeapMetrics() {
        return Optional.ofNullable(this.heapMetricsRecord);
    }

    public Optional<Boolean> getLastFullGcRecommendation() {
        return getHeapMetrics().map(this.fullGcPolicy::shouldRecommendFullGc);
    }

    public long allocate(String name, int size) {
        if (storage.getEdenMaxSize() < size) {
            throw new VirtualHeapException("Size exceeds Eden Heap size");
        }
        SimObject object = new SimObject(nextId, name, size);
        if (storage.getEdenAvailableSize() < size) {
            collectYoung();
        }
        storage.addToEden(object);
        this.allocationVolume += object.getSize();
        nextId++;
        return object.getId();
    }

    public void addRoot(long id) {
        roots.add(storage.findObject(id).getId());
    }

    public void removeRoot(long id) {
        roots.remove(storage.findObject(id).getId());
    }

    public void addReference(long fromId, long toId) {
        SimObject source = storage.findObject(fromId);
        SimObject target = storage.findObject(toId);
        source.addReference(toId);
        rememberedSetManager.record(source, target);
    }

    public void removeReference(long fromId, long toId) {
        SimObject source = storage.findObject(fromId);
        storage.findObject(toId);
        source.removeReference(toId);
    }

    public HeapStatusRecord getHeapStatus() {
        return storage.getStatus(roots, rememberedSet, sourceSurvivor);
    }

    public GCReportRecord collectFull() {
        long actualHeapSizeBefore = storage.getActualSize();
        long actualHeapObjectsBefore = storage.getActualObjects();
        Set<Long> reachable = reachability.findReachable(roots);
        storage.deleteUnreachable(reachable);
        rememberedSetManager.regenerate();
        long actualHeapSizeAfter = storage.getActualSize();
        long actualHeapObjectsAfter = storage.getActualObjects();
        lastGcReportRecord = reportBuilder.createFull(actualHeapObjectsBefore, actualHeapObjectsAfter,
                actualHeapSizeBefore, actualHeapSizeAfter);
        return lastGcReportRecord;
    }

    public GCReportRecord collectYoung() {
        long actualHeapSizeBefore = storage.getActualSize();
        long actualHeapObjectsBefore = storage.getActualObjects();
        Set<Long> reachable = reachability.findYoungReachable(roots, rememberedSet);
        List<ObjectOperationRecord> plan = youngGcPlanner.createPlan(reachable, sourceSurvivor);
        storage.executePlan(plan);
        rememberedSetManager.updateAfterPromotion(plan);
        storage.cleanYoungSpaces(reachable, sourceSurvivor);
        sourceSurvivor = sourceSurvivor == SourceSurvivorEnum.S0
            ? SourceSurvivorEnum.S1
            : SourceSurvivorEnum.S0;
        long actualHeapSizeAfter = storage.getActualSize();
        long actualHeapObjectsAfter = storage.getActualObjects();
        lastGcReportRecord = reportBuilder.createYoung(actualHeapObjectsBefore, actualHeapObjectsAfter,
                actualHeapSizeBefore, actualHeapSizeAfter, plan);
        this.youngCollectsCompleted += 1;
        this.heapMetricsRecord = this.heapMetrics.getHeapMetricsRecord(plan, this.lastGcReportRecord.promotedUnits(), this.allocationVolume, this.youngCollectsCompleted);
        return lastGcReportRecord;
    }
}
