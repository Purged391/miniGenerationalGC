package config;

import exceptions.ConfigException;

public class VirtualHeapConfig {
    private final int edenCapacity;
    private final int survivorCapacity;
    private final int oldCapacity;
    private final int promotionAge;

    public VirtualHeapConfig(int edenCapacity, int survivorCapacity, int oldCapacity, int promotionAge) {
        if(!this.checkHigherThanZero(edenCapacity) || !this.checkHigherThanZero(survivorCapacity) || !this.checkHigherThanZero(oldCapacity)){
            throw new ConfigException("Memory units cannot be 0 nor negative");
        }
        if(!this.checkHigherThanZero(promotionAge)){
            throw new ConfigException("PromotionAge cannot be 0 nor negative");
        }
        this.edenCapacity = edenCapacity;
        this.survivorCapacity = survivorCapacity;
        this.oldCapacity = oldCapacity;
        this.promotionAge = promotionAge;
    }
    public VirtualHeapConfig() {
        this(10, 6, 20, 2);
    }

    public int getEdenCapacity(){
        return this.edenCapacity;
    }
    public int getSurvivorCapacity(){
        return this.survivorCapacity;
    }
    public int getOldCapacity(){
        return this.oldCapacity;
    }
    public int getPromotionAge(){
        return this.promotionAge;
    }

    private boolean checkHigherThanZero(int field){
        return field > 0;
    }
}
