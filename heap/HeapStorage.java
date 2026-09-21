package heap;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import enums.SourceSurvivorEnum;
import enums.SpaceEnum;
import exceptions.VirtualHeapException;
import objects.SimObject;
import records.HeapStatusRecord;
import records.ObjectOperationRecord;
import records.SimObjectRecord;
import records.SpaceStatusRecord;
import space.Space;

final class HeapStorage {
    private final Space eden;
    private final Space survivor0;
    private final Space survivor1;
    private final Space old;

    HeapStorage(int edenCapacity, int survivorCapacity, int oldCapacity) {
        this.eden = new Space(SpaceEnum.EDEN, edenCapacity);
        this.survivor0 = new Space(SpaceEnum.SURVIVOR_0, survivorCapacity);
        this.survivor1 = new Space(SpaceEnum.SURVIVOR_1, survivorCapacity);
        this.old = new Space(SpaceEnum.OLD, oldCapacity);
    }

    SimObject findObject(long id) {
        Optional<SimObject> object = eden.findObjectById(id);
        if (object.isPresent()) {
            return object.get();
        }
        object = survivor0.findObjectById(id);
        if (object.isPresent()) {
            return object.get();
        }
        object = survivor1.findObjectById(id);
        if (object.isPresent()) {
            return object.get();
        }
        object = old.findObjectById(id);
        if (object.isPresent()) {
            return object.get();
        }
        throw new VirtualHeapException("No object exist in the heap with id: " + id);
    }

    Space getSpace(SpaceEnum space) {
        return switch (space) {
            case EDEN -> eden;
            case SURVIVOR_0 -> survivor0;
            case SURVIVOR_1 -> survivor1;
            case OLD -> old;
        };
    }

    double getOldOccupancyPercentage() {
        return (double) old.getActualSize() / old.getMaxSize() * 100;
    }

    int getEdenMaxSize() {
        return eden.getMaxSize();
    }

    int getEdenAvailableSize() {
        return eden.getAvailableSize();
    }

    void addToEden(SimObject object) {
        eden.addObject(object);
    }

    long getActualSize() {
        return (long) eden.getActualSize() + survivor0.getActualSize()
                + survivor1.getActualSize() + old.getActualSize();
    }

    long getActualObjects() {
        return (long) eden.getStorageSize() + survivor0.getStorageSize()
                + survivor1.getStorageSize() + old.getStorageSize();
    }

    HeapStatusRecord getStatus(Set<Long> roots, Set<Long> rememberedSet, SourceSurvivorEnum source) {
        return new HeapStatusRecord(spaceStatus(eden), spaceStatus(survivor0), spaceStatus(survivor1),
                spaceStatus(old), roots, rememberedSet, source, getDestination(source));
    }

    void deleteUnreachable(Set<Long> reachable) {
        deleteUnreachable(reachable, eden);
        deleteUnreachable(reachable, survivor0);
        deleteUnreachable(reachable, survivor1);
        deleteUnreachable(reachable, old);
    }

    void cleanYoungSpaces(Set<Long> reachable, SourceSurvivorEnum source) {
        deleteUnreachable(reachable, eden);
        Space sourceSpace = source == SourceSurvivorEnum.S0 ? survivor0 : survivor1;
        deleteUnreachable(reachable, sourceSpace);
    }

    void executePlan(List<ObjectOperationRecord> plan) {
        for (ObjectOperationRecord record : plan) {
            SimObject object = findObject(record.id());
            Space origin = getSpace(object.getSpace());
            Space destination = getSpace(record.destination());
            moveObject(object.getId(), origin, destination);
            object.incrementAge();
        }
    }

    private void deleteUnreachable(Set<Long> reachable, Space space) {
        for (SimObject object : space.getStorage()) {
            if (!reachable.contains(object.getId())) {
                space.removeObject(object.getId());
            }
        }
    }

    private void moveObject(long id, Space origin, Space destination) {
        Optional<SimObject> objectOpt = origin.findObjectById(id);
        if (objectOpt.isEmpty()) {
            throw new VirtualHeapException(
                    "Cannot move object between spaces becacuse it cannot be found in origin space");
        }
        if (destination.findObjectById(id).isPresent()) {
            throw new VirtualHeapException("Duplicate found when trying to move object between spaces");
        }
        SimObject object = objectOpt.get();
        if (!destination.fits(object)) {
            throw new VirtualHeapException("Object doesn't fit in destiny space");
        }
        destination.addObject(object);
        origin.removeObject(id);
        object.setSpace(destination.getId());
    }

    private SpaceStatusRecord spaceStatus(Space space) {
        List<SimObjectRecord> storageImage = space.getStorage().stream()
                .map(object -> new SimObjectRecord(object.getId(), object.getName(), object.getSize(),
                        object.getAge(), object.getSpace(), object.getReferences()))
                .toList();
        return new SpaceStatusRecord(space.getId(), space.getMaxSize(), space.getActualSize(),
                space.getAvailableSize(), storageImage);
    }

    private SourceSurvivorEnum getDestination(SourceSurvivorEnum source) {
        return source == SourceSurvivorEnum.S0 ? SourceSurvivorEnum.S1 : SourceSurvivorEnum.S0;
    }
}
