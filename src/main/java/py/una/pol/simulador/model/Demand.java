package py.una.pol.simulador.model;

public class Demand {
    private int source;
    private int destination;
    private int fs;
    private int timeLife;
    private boolean blocked;

    public Demand(int source, int destination, int fs, int timeLife) {
        this.source = source;
        this.destination = destination;
        this.fs = fs;
        this.timeLife = timeLife;
        this.blocked = false;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean getBlocked() {
        return this.blocked;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public int getFs() {
        return fs;
    }

    public void setFs(int fs) {
        this.fs = fs;
    }

    public int getTimeLife() {
        return timeLife;
    }

    public void setTimeLife(int timeLife) {
        this.timeLife = timeLife;
    }

    @Override
    public String toString() {
        return "Demand{" +
                "Origen=" + source +
                ", Destino=" + destination +
                ", FS=" + fs +
                ", Tiempo de vida=" + timeLife +
                '}';
    }
}
