package py.una.pol.simulador.model;
//Se llama options y no option porque una instancia de la clase oontiene varias opciones :v
public class Options {
    private int time;
    private String topology;
    private float fswidth;
    private int capacity;
    private int erlang;
    private int lambda;
    private int fsrangemin;
    private int fsrangmax;

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

    public float getFswidth() {
        return fswidth;
    }

    public void setFswidth(float fswidth) {
        this.fswidth = fswidth;
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

    public int getFsrangemin() {
        return fsrangemin;
    }

    public void setFsrangemin(int fsrangemin) {
        this.fsrangemin = fsrangemin;
    }

    public int getFsrangmax() {
        return fsrangmax;
    }

    public void setFsrangmax(int fsrangmax) {
        this.fsrangmax = fsrangmax;
    }

    @Override
    public String toString() {
        return "Options{" +
                "time=" + time +
                ", topology='" + topology + '\'' +
                ", fswidth=" + fswidth +
                ", capacity=" + capacity +
                ", erlang=" + erlang +
                ", lambda=" + lambda +
                ", fsrangemin=" + fsrangemin +
                ", fsrang_max=" + fsrangmax +
                '}';
    }
}
