package py.una.pol.simulador.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Core core = (Core) o;
        return Double.compare(core.bandwidth, bandwidth) == 0 &&
                Objects.equals(fs, core.fs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bandwidth, fs);
    }
}
