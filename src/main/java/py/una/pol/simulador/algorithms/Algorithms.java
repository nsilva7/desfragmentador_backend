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
        System.out.println("CANTIDAD DE KSP UBICADOS: " + kspPlaced.size());
        if(kspPlaced.size() == 0)
            return null;
        //Ksp ubidados ahora se debe elegir el mejor
        bestKspSlot = Utils.countCuts(graph, kspPlaced, capacity, core, demand.getFs());
        System.out.println("INDEX: " + bestKspSlot);
        EstablisedRoute establisedRoute = new EstablisedRoute((kspPlaced.get(bestKspSlot.get("ksp")).getEdgeList()), bestKspSlot.get("slot"), demand.getFs(), demand.getTimeLife());
//        System.out.println("RUTA ESTABLECIDA: " + establisedRoute);
        return establisedRoute;
    }

    public static void faca(){
        System.out.println("FA-CA");
    }

    public static void mtlsc(){
        System.out.println("MTLSC");
    }
}
