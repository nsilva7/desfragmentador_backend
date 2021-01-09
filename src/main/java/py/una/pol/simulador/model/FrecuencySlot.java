package py.una.pol.simulador.model;

import java.util.Objects;

public class FrecuencySlot {
    private int lifetime;
    private boolean free;
    private double fsWidh;

    public FrecuencySlot(double fsWidh) {
        this.fsWidh = fsWidh;
        this.lifetime = 0;
        this.free = true;
    }

    public int getLifetime() {
        return lifetime;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public double getFsWidh() {
        return fsWidh;
    }

    public void setFsWidh(double fsWidh) {
        this.fsWidh = fsWidh;
    }

    public boolean subLifetime(){
        if (this.free)
            return false;
        this.lifetime--;
        if(this.lifetime == 0){
            this.free = true;
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "FrecuencySlot{" +
                "lifetime=" + lifetime +
                ", free=" + free +
                ", fsWidh=" + fsWidh +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrecuencySlot that = (FrecuencySlot) o;
        return lifetime == that.lifetime &&
                free == that.free &&
                Double.compare(that.fsWidh, fsWidh) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lifetime, free, fsWidh);
    }
}
