package records;

import enums.SpaceEnum;

public record ObjectOperationRecord(
    long id,
    int newAge,
    SpaceEnum destination
) {
    
}
