package heap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import config.VirtualHeapConfig;
import enums.SourceSurvivorEnum;
import enums.SpaceEnum;
import exceptions.VirtualHeapException;
import objects.SimObject;
import records.ObjectOperationRecord;
import space.Space;

final class YoungGcPlanner {
    private final HeapStorage storage;
    private final VirtualHeapConfig config;

    YoungGcPlanner(HeapStorage storage, VirtualHeapConfig config) {
        this.storage = storage;
        this.config = config;
    }

    List<ObjectOperationRecord> createPlan(Set<Long> reachable, SourceSurvivorEnum source) {
        Space destination = storage.getSpace(source == SourceSurvivorEnum.S0
                ? SpaceEnum.SURVIVOR_1
                : SpaceEnum.SURVIVOR_0);
        if (destination.getActualSize() != 0) {
            throw new VirtualHeapException("There was a problem in the space management");
        }
        int survivorCapacity = destination.getAvailableSize();
        int oldCapacity = storage.getSpace(SpaceEnum.OLD).getAvailableSize();
        List<ObjectOperationRecord> plan = new ArrayList<>();
        for (Long id : reachable.stream().sorted().toList()) {
            SimObject object = storage.findObject(id);
            int newAge = object.getAge() + 1;
            int size = object.getSize();
            if (newAge >= config.getPromotionAge()) {
                if (size <= oldCapacity) {
                    plan.add(new ObjectOperationRecord(id, newAge, SpaceEnum.OLD));
                    oldCapacity -= size;
                } else {
                    throw new VirtualHeapException("Old space overflow");
                }
            } else if (size <= survivorCapacity) {
                plan.add(new ObjectOperationRecord(id, newAge, destination.getId()));
                survivorCapacity -= size;
            } else if (size <= oldCapacity) {
                plan.add(new ObjectOperationRecord(id, newAge, SpaceEnum.OLD));
                oldCapacity -= size;
            } else {
                throw new VirtualHeapException("Old space overflow");
            }
        }
        return plan;
    }
}
