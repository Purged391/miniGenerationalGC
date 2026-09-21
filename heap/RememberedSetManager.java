package heap;

import java.util.List;
import java.util.Set;

import enums.SpaceEnum;
import objects.SimObject;
import records.ObjectOperationRecord;

final class RememberedSetManager {
    private final HeapStorage storage;
    private final Set<Long> rememberedSet;

    RememberedSetManager(HeapStorage storage, Set<Long> rememberedSet) {
        this.storage = storage;
        this.rememberedSet = rememberedSet;
    }

    void record(SimObject source, SimObject target) {
        if (SpaceEnum.OLD.equals(source.getSpace()) && isYoung(target.getSpace())) {
            rememberedSet.add(source.getId());
        }
    }

    void regenerate() {
        rememberedSet.clear();
        for (SimObject object : storage.getSpace(SpaceEnum.OLD).getStorage()) {
            for (long referenceId : object.getReferences()) {
                record(object, storage.findObject(referenceId));
            }
        }
    }

    void updateAfterPromotion(List<ObjectOperationRecord> plan) {
        plan.stream()
                .filter(record -> SpaceEnum.OLD.equals(record.destination()))
                .map(record -> storage.findObject(record.id()))
                .forEach(object -> {
                    for (long referenceId : object.getReferences()) {
                        record(object, storage.findObject(referenceId));
                    }
                });
    }

    private boolean isYoung(SpaceEnum space) {
        return SpaceEnum.EDEN.equals(space)
                || SpaceEnum.SURVIVOR_0.equals(space)
                || SpaceEnum.SURVIVOR_1.equals(space);
    }
}
