package py.una.pol.simulador.model;

import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.List;

public class EstablisedRoute {
//    private GraphPath path;
    private int fsIndexBegin;
    private int fs;
    private int timeLife;
    private List<Link> path;

    public int getTimeLife() {
        return timeLife;
    }

    public void setTimeLife(int timeLife) {
        this.timeLife = timeLife;
    }

    public EstablisedRoute(List path, int fsIndexBegin, int fs, int timeLife) {
        this.path = path;
        this.fsIndexBegin = fsIndexBegin;
        this.fs = fs;
        this.timeLife = timeLife;
    }

    public List getPath() {
        return path;
    }

    public void setPath(ArrayList path) {
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
                '}';
    }
}
