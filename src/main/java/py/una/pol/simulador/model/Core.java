package py.una.pol.simulador.model;

import java.util.ArrayList;
import java.util.List;

public class Core {
    private double bandwidth;
    private List<FrecuencySlot> fs;

    public Core(double bandwidth, int fs) {
        this.bandwidth = bandwidth;
        this.fs = new ArrayList<>();
        for (int i = 0; i < fs; i++){
            this.fs.add(new FrecuencySlot(bandwidth/fs));
        }
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
