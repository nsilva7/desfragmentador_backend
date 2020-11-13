package py.una.pol.simulador.model;
//Se llama options y no option porque una instancia de la clase oontiene varias opciones :v
public class Options {
    private int time;
    private String topology;
    private float fsWidth;
    private int capacity;
    private int erlang;
    private int lambda;
    private int fsRangeMin;
    private int fsRangeMax;
    private String routingAlg;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getTopology() {
        return topology;
    }

    public void setTopology(String topology) {
        this.topology = topology;
    }

    public float getFsWidth() {
        return fsWidth;
    }

    public void setFsWidth(float fsWidth) {
        this.fsWidth = fsWidth;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getErlang() {
        return erlang;
    }

    public void setErlang(int erlang) {
        this.erlang = erlang;
    }

    public int getLambda() {
        return lambda;
    }

    public void setLambda(int lambda) {
        this.lambda = lambda;
    }

    public int getFsRangeMin() {
        return fsRangeMin;
    }

    public void setFsRangeMin(int fsRangeMin) {
        this.fsRangeMin = fsRangeMin;
    }

    public int getFsRangeMax() {
        return fsRangeMax;
    }

    public void setFsRangeMax(int fsRangeMax) {
        this.fsRangeMax = fsRangeMax;
    }

    public String getRoutingAlg() {
        return routingAlg;
    }

    public void setRoutingAlg(String routingAlg) {
        this.routingAlg = routingAlg;
    }

    @Override
    public String toString() {
        return "Options{" +
                "time=" + time +
                ", topology='" + topology + '\'' +
                ", fsWidth=" + fsWidth +
                ", capacity=" + capacity +
                ", erlang=" + erlang +
                ", lambda=" + lambda +
                ", fsRangeMin=" + fsRangeMin +
                ", fsRangeMax=" + fsRangeMax +
                ", routingAlg='" + routingAlg + '\'' +
                '}';
    }
}
