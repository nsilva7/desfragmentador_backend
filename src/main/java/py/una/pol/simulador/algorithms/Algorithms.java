package py.una.pol.simulador.algorithms;

import org.jgrapht.GraphPath;
import py.una.pol.simulador.model.Demand;
import org.jgrapht.Graph;
import py.una.pol.simulador.model.EstablisedRoute;
import py.una.pol.simulador.model.FrecuencySlot;
import py.una.pol.simulador.model.Link;
import py.una.pol.simulador.utils.Utils;
import sun.rmi.runtime.Log;

import java.util.*;

import static py.una.pol.simulador.utils.Utils.*;

public class Algorithms {
    public static EstablisedRoute fa(Graph graph, List<GraphPath> kspaths, Demand demand, int capacity, int core){
        int begin, count;
        boolean  so[] = new boolean[capacity]; //Representa la ocupación del espectro de todos los enlaces.
        List<GraphPath> kspPlaced = new ArrayList<>();
        Map<String, Integer> bestKspSlot = new HashMap<>();
        int k = 0;
        while (k < kspaths.size() && kspaths.get(k) != null){
            Arrays.fill(so, false);//Se inicializa todo el espectro como libre
            GraphPath ksp = kspaths.get(k);

            //Se setean los slots libres
            for(int i = 0; i < capacity; i++){
                for (Object path: ksp.getEdgeList()){
                    Link link = (Link) path;
                    FrecuencySlot fs = link.getCores().get(core).getFs().get(i);
                    if(!fs.isFree()){
                        so[i] = true;
                    }
                }
            }
            begin = count = 0;
            int j;
            capacity:
            for(int i = 0; i < capacity; i++){
                count = 0;
                if(!so[i]){
                    begin = i;
                    for(j = i; j < capacity; j++){
                        if(!so[j]){
                            count++;
                        }else{
                            i = j;
                            break;
                        }
                        if(count == demand.getFs()){
                            kspPlaced.add(kspaths.get(k));
                            break capacity;
                        }
                    }
                    if(j == capacity)
                        break;
                }
            }
            k++;
        }
        if(kspPlaced.size() == 0)
            return null;
        //Ksp ubidados ahora se debe elegir el mejor
        bestKspSlot = Utils.countCuts(graph, kspPlaced, capacity, core, demand.getFs());
        EstablisedRoute establisedRoute = new EstablisedRoute((kspPlaced.get(bestKspSlot.get("ksp")).getEdgeList()), bestKspSlot.get("slot"), demand.getFs(), demand.getTimeLife(), demand.getSource(), demand.getDestination(), core);
        return establisedRoute;
    }

    public static EstablisedRoute faca(Graph graph, List<GraphPath> kspaths, Demand demand, int capacity, int core){
        int count;
        boolean  so[] = new boolean[capacity]; //Representa la ocupación del espectro de todos los enlaces.
        List<GraphPath> kspPlaced = new ArrayList<>();
        int k = 0;
        while (k < kspaths.size() && kspaths.get(k) != null){
            Arrays.fill(so, false);//Se inicializa todo el espectro como libre
            GraphPath ksp = kspaths.get(k);

            //Se setean los slots libres
            for(int i = 0; i < capacity; i++){
                for (Object path: ksp.getEdgeList()){
                    Link link = (Link) path;
                    FrecuencySlot fs = link.getCores().get(core).getFs().get(i);
                    if(!fs.isFree()){
                        so[i] = true;
                    }
                }
            }

            int j;
            capacity:
            for(int i = 0; i < capacity; i++){
                count = 0;
                if(!so[i]){
                    for(j = i; j < capacity; j++){
                        if(!so[j]){
                            count++;
                        }else{
                            i = j;
                            break;
                        }
                        if(count == demand.getFs()){
                            kspPlaced.add(kspaths.get(k));
                            break capacity;
                        }
                    }
                    if(j == capacity)
                        break;
                }
            }
            k++;

        }

        System.out.println("CANTIDAD DE KSP UBICADOS: " + kspPlaced.size());
        if(kspPlaced.size() == 0)
            return null;
        Map<String, Integer> slotCuts = new HashMap<>();
        ArrayList<Integer> cIndexes;
        double Fcmt = 9999999;
        double FcmtAux;
        int selectedPath= -1;
        int slot = -1;
        for (k = 0; k <  kspPlaced.size(); k++) {
            slotCuts = numCuts(kspPlaced.get(k), graph, capacity, core, demand.getFs());
            System.out.println("Mejor slot: " + slotCuts.get("slot") + " con " + slotCuts.get("cuts") + " cuts");
            if(slotCuts != null) {

                double misalignement =  countMisalignment(kspPlaced.get(k), graph, core,slotCuts.get("slot"));
                double freeCapacity = countFreeCapacity(kspPlaced.get(k), graph, core,capacity);
                double jumps = kspPlaced.get(k).getLength();
                double neighbours = countNeighbour(kspPlaced.get(k),graph);

                FcmtAux = slotCuts.get("cuts") + (misalignement/(demand.getFs()*neighbours)) + (jumps *(demand.getFs()/freeCapacity));

                if (FcmtAux<Fcmt){
                    Fcmt = FcmtAux;
                    selectedPath = k;
                    slot = slotCuts.get("slot");
                    System.out.println("FCMT " + Fcmt);
                }
            }
        }

        if(selectedPath == -1) {
            return null;
        }

        EstablisedRoute establisedRoute = new EstablisedRoute(kspPlaced.get(selectedPath).getEdgeList(), slot, demand.getFs(), demand.getTimeLife(),demand.getSource(),demand.getDestination(), core);
        System.out.println("RUTA ESTABLECIDA: " + establisedRoute);
        return establisedRoute;


    }

    public static EstablisedRoute mtlsc(Graph graph, List<GraphPath> kspaths, Demand demand, int capacity, int core){
        int k = 0;
        int begin;
        int end;
        int beginSlot = -1;
        int selectedPath = -1;
        float kspMaxSC = -1;
        boolean  so[] = new boolean[capacity];

        EstablisedRoute establisedRoute = new EstablisedRoute();
        while (k < kspaths.size() && kspaths.get(k) != null) {
            float maxSC = -1;
            Arrays.fill(so, false);//Se inicializa todo el espectro como libre
            GraphPath ksp = kspaths.get(k);
            for (int i = 0; i < capacity; i++) {
                for (Object path : ksp.getEdgeList()) {
                    Link link = (Link) path;
                    FrecuencySlot fs = link.getCores().get(core).getFs().get(i);
                    if (!fs.isFree()) {
                        so[i] = true;
                    }
                }
            }

            List<HashMap> espectralBlocks = new ArrayList<>();
            for (int i = 0; i < capacity; i++) {
                if (!so[i]) {
                    begin = i;
                    while (i < capacity && !so[i]) {
                        i++;
                    }
                    end = i - 1;
                    if (end - begin + 1 >= demand.getFs()) { //bloque que puede utilizarse
                        HashMap<String,Integer> block = new HashMap();
                        block.put("begin", begin);
                        block.put("end", end);
                        espectralBlocks.add(block);
                    }
                }

            }

            for (HashMap espectralBlock: espectralBlocks) {
                float spectrumConsecutiveness = 0;
                int blockBegin = (int)espectralBlock.get("begin");
                for (Object path: ksp.getEdgeList()){
                    Link link = (Link) path;

                    int linkBlocks = 0;
                    for (int i = 0; i < capacity; i++) {
                        if (link.getCores().get(core).getFs().get(i).isFree()) {
                            while (i < capacity && !link.getCores().get(core).getFs().get(i).isFree()) {  //calculamos la cantidad de bloques del Link
                                i++;
                            }
                            linkBlocks++;
                        }

                    }

                    float sum = 0;
                    float fsCount = 0;

                    for(int c = 0; c < capacity - 1; c++) {
                        if(c < blockBegin || c > blockBegin + demand.getFs() - 1 ) {
                            int slot = link.getCores().get(core).getFs().get(c).isFree()?1:0;
                            int nextSlot = link.getCores().get(core).getFs().get(c+1).isFree()?1:0;
                            sum += slot*nextSlot;
                            fsCount += slot;
                        }

                    }
                    fsCount += link.getCores().get(core).getFs().get(capacity-1).isFree()?1:0; //para el ultimo slot

                    spectrumConsecutiveness += (sum/linkBlocks)*(fsCount/capacity);  //acumulamos el cl de los links

                }

                if(spectrumConsecutiveness > maxSC) {
                    maxSC = spectrumConsecutiveness;
                    selectedPath = k;
                    beginSlot= blockBegin;

                }
            }
            if(maxSC > kspMaxSC) {
                kspMaxSC = maxSC;
                establisedRoute.setPath(kspaths.get(selectedPath).getEdgeList());
                establisedRoute.setFsIndexBegin(beginSlot);
            }
            k++;
        }
        if(establisedRoute.getPath() != null) {
            establisedRoute.setFs(demand.getFs());
            establisedRoute.setTimeLife(demand.getTimeLife());
            establisedRoute.setFrom(demand.getSource());
            establisedRoute.setTo(demand.getDestination());
            establisedRoute.setCore(core);
            System.out.println("RUTA ESTABLECIDA: " + establisedRoute);
            return establisedRoute;
        }

        return null;

    }
}
