package py.una.pol.simulador.model;

public class Demand {
    private int origin;
    private int destination;
    private int fs;
    private int timeLife;

    public Demand(int origin, int destination, int fs, int timeLife) {
        this.origin = origin;
        this.destination = destination;
        this.fs = fs;
        this.timeLife = timeLife;
    }

    public int getOrigin() {
        return origin;
    }

    public void setOrigin(int origin) {
        this.origin = origin;
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
                "Origen=" + origin +
                ", Destino=" + destination +
                ", FS=" + fs +
                ", Tiempo de vida=" + timeLife +
                '}';
    }
}
