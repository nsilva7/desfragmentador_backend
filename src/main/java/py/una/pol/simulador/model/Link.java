package py.una.pol.simulador.model;

import java.util.List;

public class Link {
    private int distance;
    private List <Core> cores;

    public Link(int distance, List<Core> cores) {
        this.distance = distance;
        this.cores = cores;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public List<Core> getCores() {
        return cores;
    }

    public void setCores(List<Core> cores) {
        this.cores = cores;
    }

    @Override
    public String toString() {
        return "Link{" +
                "distance=" + distance +
                ", cores=" + cores.size() +
                '}';
    }
}
