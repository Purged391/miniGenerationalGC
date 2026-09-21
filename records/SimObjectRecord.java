package records;

import java.util.Set;
import enums.SpaceEnum;

public record SimObjectRecord(
                long id,
                String name,
                int size,
                int age,
                SpaceEnum space,
                Set<Long> references) {
                        
        public SimObjectRecord {
                references = Set.copyOf(references);
        }

}
