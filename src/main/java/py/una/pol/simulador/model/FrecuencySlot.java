package py.una.pol.simulador.model;

import java.io.Serializable;
import java.util.Objects;

public class FrecuencySlot implements Serializable {
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
        //if(this.lifetime < 0)
            //System.out.println("ERROR, LF MENOR A 0: " + this.lifetime);
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

}
