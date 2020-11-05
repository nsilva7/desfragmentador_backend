package py.una.pol.simulador.model;

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

    @Override
    public String toString() {
        return "FrecuencySlot{" +
                "lifetime=" + lifetime +
                ", free=" + free +
                ", fsWidh=" + fsWidh +
                '}';
    }
}
