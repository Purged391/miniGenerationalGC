package records;

import java.util.Set;

import enums.SourceSurvivorEnum;

public record HeapStatusRecord(
        SpaceStatusRecord eden,
        SpaceStatusRecord survivor0,
        SpaceStatusRecord survivor1,
        SpaceStatusRecord old,
        Set<Long> roots,
        Set<Long> rememberedSet,
        SourceSurvivorEnum source,
        SourceSurvivorEnum destination) {
    public HeapStatusRecord {
        roots = Set.copyOf(roots);
        rememberedSet = Set.copyOf(rememberedSet);
    }

}
