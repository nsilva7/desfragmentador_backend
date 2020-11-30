package py.una.pol.simulador.model;

import java.util.ArrayList;
import java.util.Map;

public class ReleasedSlots {

    private ArrayList<Map<String, String>> releasedSlots;
    private int time;
    private boolean released;

    public ReleasedSlots(ArrayList<Map<String, String>> releasedSlots, int time, boolean released) {
        this.releasedSlots = releasedSlots;
        this.time = time;
        this.released = released;
    }

    public ReleasedSlots() {
    }

    public ArrayList<Map<String, String>> getReleasedSlots() {
        return releasedSlots;
    }

    public void setReleasedSlots(ArrayList<Map<String, String>> releasedSlots) {
        this.releasedSlots = releasedSlots;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    @Override
    public String toString() {
        return "ReleasedSlots{" +
                "releasedSlots=" + releasedSlots +
                ", time=" + time +
                ", released=" + released +
                '}';
    }


}
