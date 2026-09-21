package space;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import enums.SpaceEnum;
import exceptions.SpaceException;
import objects.SimObject;

public class Space {
    private final SpaceEnum id;
    private final int maxSize;
    private final Map<Long, SimObject> storage;
    private int actualSize;

    public Space(SpaceEnum id, int maxSize) {
        if (id == null) {
            throw new SpaceException("ID cannot be null");
        }
        if (maxSize <= 0) {
            throw new SpaceException("Max size cannot be negative or 0");
        }
        this.id = id;
        this.maxSize = maxSize;
        this.storage = new HashMap<>();
        this.actualSize = 0;
    }

    public SpaceEnum getId() {
        return id;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public SimObject removeObject(Long id) {
        if(!this.storage.containsKey(id)){
            throw new SpaceException("ID not found");
        }
        SimObject object = this.storage.remove(id);
        this.actualSize = this.actualSize - object.getSize();
        return object;
    }

    public List<SimObject> getStorage() {
        return storage.values().stream().toList();
    }

    public int getStorageSize() {
        return storage.size();
    }

    public void addObject(SimObject object) {
        if (this.storage.containsKey(object.getId())) {
            throw new SpaceException("Duplicate ID");
        }
        if (!fits(object)) {
            throw new SpaceException("This object exceeds the avaliable space");
        }
        int virtualSize = object.getSize() + this.actualSize;
        this.storage.put(object.getId(), object);
        this.actualSize = virtualSize;
    }

    public int getActualSize() {
        return this.actualSize;
    }

    public int getAvailableSize() {
        return this.maxSize - this.actualSize;
    }

    public boolean fits(SimObject object) {
        return object.getSize() <= this.getAvailableSize();
    }

    public Optional<SimObject> findObjectById(long id){
        return Optional.ofNullable(this.storage.get(id));
    }

}
