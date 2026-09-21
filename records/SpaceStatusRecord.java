package records;

import java.util.List;

import enums.SpaceEnum;

public record SpaceStatusRecord(
        SpaceEnum id,
        int totalSize,
        int actualSize,
        int availableSize,
        List<SimObjectRecord> storageImage) {
    public SpaceStatusRecord {
        storageImage = List.copyOf(storageImage);
    }

}
