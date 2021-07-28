package py.una.pol.simulador.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Core implements Serializable {
    private double bandwidth;
    private List<FrecuencySlot> fs;

    public Core(double bandwidth, int fs) {
        this.bandwidth = bandwidth;
        this.fs = new ArrayList<>();
        for (int i = 0; i < fs; i++){
            this.fs.add(new FrecuencySlot(bandwidth/fs));
        }
    }

    public Core(double bandwidth, List<FrecuencySlot> fs) {
        this.bandwidth = bandwidth;
        this.fs = fs;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public List<FrecuencySlot> getFs() {
        return fs;
    }

    public void setFs(List<FrecuencySlot> fs) {
        this.fs = fs;
    }

    @Override
    public String toString() {
        return "Core{" +
                "bandwidth=" + bandwidth +
                ", fs=" + fs +
                '}';
    }


}
