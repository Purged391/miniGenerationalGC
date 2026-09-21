package records;

import enums.GCTypeEnum;

public record GCReportRecord(
        GCTypeEnum type,
        int numCollection,
        long objectsBefore,
        long objectsAfter,
        long unitsBefore,
        long unitsAfter,
        long reclaimedObjects,
        long reclaimedUnits,
        long promotedObjects,
        long promotedUnits) {

}
