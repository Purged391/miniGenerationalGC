package heap;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import enums.SpaceEnum;
import objects.SimObject;

final class ReachabilityAnalyzer {
    private final HeapStorage storage;

    ReachabilityAnalyzer(HeapStorage storage) {
        this.storage = storage;
    }

    Set<Long> findReachable(Set<Long> roots) {
        Set<Long> visited = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            Long id = pending.pop();
            if (!visited.contains(id)) {
                visited.add(id);
                SimObject object = storage.findObject(id);
                pending.addAll(object.getReferences());
            }
        }
        return visited;
    }

    Set<Long> findYoungReachable(Set<Long> roots, Set<Long> rememberedSet) {
        Set<Long> visited = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.addAll(filterNotOld(roots));
        for (Long id : rememberedSet) {
            pending.addAll(filterNotOld(storage.findObject(id).getReferences()));
        }
        while (!pending.isEmpty()) {
            Long id = pending.pop();
            if (!visited.contains(id)) {
                visited.add(id);
                SimObject object = storage.findObject(id);
                pending.addAll(filterNotOld(object.getReferences()));
            }
        }
        return visited;
    }

    private List<Long> filterNotOld(Set<Long> ids) {
        return ids.stream()
                .filter(id -> !SpaceEnum.OLD.equals(storage.findObject(id).getSpace()))
                .toList();
    }
}
