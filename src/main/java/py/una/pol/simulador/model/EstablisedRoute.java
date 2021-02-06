package py.una.pol.simulador.model;

import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EstablisedRoute {
//    private GraphPath path;
    private int fsIndexBegin;
    private int fs;
    private int timeLife;
    private int from;
    private int to;
    private List<Link> path;
    private int core;

    public int getTimeLife() {
        return timeLife;
    }

    public void setTimeLife(int timeLife) {
        this.timeLife = timeLife;
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

    public EstablisedRoute() {
    }

    public EstablisedRoute(List path, int fsIndexBegin, int fs, int timeLife, int from, int to, int core) {
        this.path = path;
        this.fsIndexBegin = fsIndexBegin;
        this.fs = fs;
        this.timeLife = timeLife;
        this.from = from;
        this.to = to;
        this.core = core;
    }

    public int getCore() {
        return core;
    }

    public void setCore(int core) {
        this.core = core;
    }

    public List<Link> getPath() {
        return path;
    }

    public void setPath(List path) {
        this.path = path;
    }

    public int getFsIndexBegin() {
        return fsIndexBegin;
    }

    public void setFsIndexBegin(int fsIndexBegin) {
        this.fsIndexBegin = fsIndexBegin;
    }

    public int getFs() {
        return fs;
    }

    public void setFs(int fs) {
        this.fs = fs;
    }

    @Override
    public String toString() {
        return "EstablisedRoute{" +
                "path=" + path +
                ", fsIndexBegin=" + fsIndexBegin +
                ", fs=" + fs +
                ", tl=" + timeLife +
                ", from=" + from +
                ", to=" + to +
                '}';
    }



}
