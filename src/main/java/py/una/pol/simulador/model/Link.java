package py.una.pol.simulador.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Link implements Serializable {
    private int distance;
    private List <Core> cores;
    private int from;
    private int to;

    public Link(int distance, List<Core> cores, int from, int to) {
        this.distance = distance;
        this.cores = cores;
        this.from = from;
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
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
//                "distance=" + distance +
//                ", cores=" + cores.size() +
//                ", from=" + from +
//                ", to=" + to +
                + from + " - " + to +
                '}';
    }


}
