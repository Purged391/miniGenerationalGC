package heap;

import java.util.List;

import enums.GCTypeEnum;
import enums.SpaceEnum;
import records.GCReportRecord;
import records.ObjectOperationRecord;

final class GcReportBuilder {
    private final HeapStorage storage;
    private int completedCollections;

    GcReportBuilder(HeapStorage storage) {
        this.storage = storage;
    }

    GCReportRecord createFull(long objectsBefore, long objectsAfter, long unitsBefore, long unitsAfter) {
        return new GCReportRecord(GCTypeEnum.FULL, nextCollection(), objectsBefore, objectsAfter,
                unitsBefore, unitsAfter, objectsBefore - objectsAfter, unitsBefore - unitsAfter, 0, 0);
    }

    GCReportRecord createYoung(long objectsBefore, long objectsAfter, long unitsBefore, long unitsAfter,
            List<ObjectOperationRecord> plan) {
        List<ObjectOperationRecord> promotedObjects = plan.stream()
                .filter(record -> SpaceEnum.OLD.equals(record.destination()))
                .toList();
        long promotedUnits = promotedObjects.stream()
                .mapToLong(record -> storage.findObject(record.id()).getSize())
                .sum();
        return new GCReportRecord(GCTypeEnum.YOUNG, nextCollection(), objectsBefore, objectsAfter,
                unitsBefore, unitsAfter, objectsBefore - objectsAfter, unitsBefore - unitsAfter,
                promotedObjects.size(), promotedUnits);
    }

    private int nextCollection() {
        completedCollections += 1;
        return completedCollections;
    }
}
