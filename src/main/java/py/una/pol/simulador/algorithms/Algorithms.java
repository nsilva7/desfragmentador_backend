package py.una.pol.simulador.algorithms;

import org.jgrapht.GraphPath;
import py.una.pol.simulador.model.Demand;
import org.jgrapht.Graph;
import py.una.pol.simulador.model.FrecuencySlot;
import py.una.pol.simulador.model.Link;
import sun.rmi.runtime.Log;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;

public class Algorithms {
    public static void fa(Graph graph, List<GraphPath> kspaths, Demand demand, int capacity, int core){
        System.out.println("FA");
        int begin = 0, end = 0, count;
        boolean demandPlaced = false;
        int  so[] = new int[capacity]; //Ocupacion de Espectro.
        List<GraphPath> kspPlaced = new ArrayList<>();
        ArrayList<Integer> begins = new ArrayList<Integer>();
        ArrayList<Integer> ends = new ArrayList<Integer>();
        ArrayList<Integer> kspIndexes = new ArrayList<Integer>();
        int k = 0;
        while (k < kspaths.size() && kspaths.get(k) != null){
            Arrays.fill(so, 0);//Se inicializa todo el espectro como libre
            GraphPath ksp = kspaths.get(k);

            //Se setean los slots libres
            for(int i = 0; i < capacity; i++){
                for (Object path: ksp.getEdgeList()){
                    Link link = (Link) path;
                    FrecuencySlot fs = link.getCores().get(core).getFs().get(i);
                    if(!fs.isFree()){
                        so[i] = 1;
                    }
                }
            }
            begin = end = count = 0;
            for(int i = 0; i < capacity; i++){
                if(so[i] == i){
                    begin = i;
                    for(int j = begin; j < capacity; j++){
                        if(so[i] == i){
                            count++;
                        }else{
                            count = 0;
                            break;
                        }
                        if(count == demand.getFs()){
                            end = k;
                            ends.add(end);
                            begins.add(begin);
                            demandPlaced = true;
                            kspPlaced.add(kspaths.get(k));
                            kspIndexes.add(k);
                            break;
                        }
                    }
                }
                if(demandPlaced){
                    demandPlaced = false;
                    break;
                }
            }
            k++;
        }

        //Ksp ubidados ahora se debe elegir el mejor

    }

    public static void faca(){
        System.out.println("FA-CA");
    }

    public static void mtlsc(){
        System.out.println("MTLSC");
    }
}
