package objects;

import java.util.HashSet;
import java.util.Set;

import enums.SpaceEnum;
import exceptions.ObjectCreationException;

public class SimObject {
    private final long id;
    private final String name;
    private final int size;
    private int age;
    private SpaceEnum space;
    private final Set<Long> references;

    public SimObject(long id, String name, int size){
        if(size <= 0){
            throw new ObjectCreationException("Cannot simulate object with 0 or negative size");
        }
        if(name == null || name.isEmpty()){
            throw new ObjectCreationException("Cannot simulate object with null name");
        }

        this.id = id;
        this.name = name;
        this.size = size;
        this.age = 0;
        this.space = SpaceEnum.EDEN;
        this.references = new HashSet<>();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getAge() {
        return age;
    }

    public void incrementAge() {
        this.age ++;
    }

    public SpaceEnum getSpace() {
        return space;
    }

    public void setSpace(SpaceEnum space) {
        if(space == null){
            throw new ObjectCreationException("Cannot set null space");
        }
        this.space = space;
    }

    public Set<Long> getReferences() {
        return Set.copyOf(references);
    }

    public void addReference(long reference) {
        this.references.add(reference);
    }

    public void removeReference(long reference) {
        this.references.remove(reference);
    }
}
