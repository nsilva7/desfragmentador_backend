package py.una.pol.simulador.algorithms;

import org.jgrapht.GraphPath;
import py.una.pol.simulador.model.Demand;
import org.jgrapht.Graph;
import py.una.pol.simulador.model.EstablisedRoute;
import py.una.pol.simulador.model.FrecuencySlot;
import py.una.pol.simulador.model.Link;
import py.una.pol.simulador.utils.Utils;
import sun.rmi.runtime.Log;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;

public class Algorithms {
    public static EstablisedRoute fa(Graph graph, List<GraphPath> kspaths, Demand demand, int capacity, int core){
        int begin = 0, end = 0, count;
        boolean  so[] = new boolean[capacity]; //Representa la ocupación del espectro de todos los enlaces.
        List<GraphPath> kspPlaced = new ArrayList<>();
        ArrayList<Integer> begins = new ArrayList<Integer>();
        ArrayList<Integer> ends = new ArrayList<Integer>();
        ArrayList<Integer> kspIndexes = new ArrayList<Integer>();
        int k = 0;
        while (k < kspaths.size() && kspaths.get(k) != null){//False libre true ocupado
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
            begin = end = count = 0;
            capacity:
            for(int i = 0; i < capacity; i++){
                if(!so[i]){
                    begin = i;
                    for(int j = begin; j < capacity; j++){
                        if(!so[i]){
                            count++;
                        }else{
                            count = 0;
                            break;
                        }
                        if(count == demand.getFs()){
                            end = k;
                            ends.add(end);
                            begins.add(begin);
                            kspPlaced.add(kspaths.get(k));
                            kspIndexes.add(k);
                            break capacity;
                        }
                    }
                }
            }
            k++;
        }
        System.out.println("CANTIDAD DE KSP UBICADOS: " + kspPlaced.size());
        if(kspPlaced.size() == 0)
            return null;
        //Ksp ubidados ahora se debe elegir el mejor
        Utils.countCuts(graph, kspPlaced, capacity, core, demand.getFs());
//        EstablisedRoute establisedRoute = new EstablisedRoute(kspPlaced.get(path), begins.get(path), demand.getFs(), demand.getTimeLife());
        EstablisedRoute establisedRoute = null;
        return establisedRoute;
    }

    public static void faca(){
        System.out.println("FA-CA");
    }

    public static void mtlsc(){
        System.out.println("MTLSC");
    }
}
