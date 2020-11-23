package py.una.pol.simulador.utils;
import org.jgrapht.GraphPath;
import py.una.pol.simulador.model.Demand;

import java.util.*;

import org.jgrapht.Graph;
import py.una.pol.simulador.model.EstablisedRoute;
import py.una.pol.simulador.model.FrecuencySlot;
import py.una.pol.simulador.model.Link;

public class Utils {

    public static List<Demand> generateDemands(int lambda, int totalTime, int fsMin, int fsMax, int cantNodos, int HT){
        int i, demandasQuantity, j, source, destination, fs, tLife;
        List<Demand> demands = new ArrayList<>();
        Random rand;
        demandasQuantity = poisson(lambda);
        for (j = 0; j < demandasQuantity; j++) {
            rand = new Random();
            source = rand.nextInt(cantNodos);
            destination = rand.nextInt(cantNodos);
            fs = (int) (Math.random() * (fsMax-fsMin+1)) + fsMin;
            while (source == destination) {
                destination = rand.nextInt(cantNodos);
            }
            tLife = getTimeLife(HT);
            demands.add(new Demand(source, destination, fs, tLife));
        }
        return demands;
    }

    public static int poisson(int lambda) {
        int b, bFact;
        double s, a;
        double e = Math.E;
        a = (Math.random() * 1) + 0;
        b = 0;
        bFact = factorial(b);
        s = (Math.pow(e, (-lambda))) * ((Math.pow(lambda, b)) / (bFact));
        while (a > s) {
            b++;
            bFact = factorial(b);
            s = s + ((Math.pow(e, (-lambda))) * ((Math.pow(lambda, b)) / (bFact)));
        }
        return b;
    }

    public static int getTimeLife(int ht) {
        int b;
        double s, a, aux, auxB, auxHT;
        double e = Math.E;
        a = (Math.random() * 1) + 0;
        b = 1;
        auxB = (double) b;
        auxHT = (double) ht;
        aux = (-1) * (auxB / auxHT);
        s = 1 - (Math.pow(e, (aux)));
        while (s < a) {
            b++;
            auxB = (double) b;
            aux = (-1) * (auxB / auxHT);
            s = 1 - (Math.pow(e, (aux)));
        }
        return b;
    }

    public static int factorial(int n) {
        int resultado = 1;
        for (int i = 1; i <= n; i++) {
            resultado *= i;
        }
        return resultado;
    }

    public static void ksp(Graph g, int source, int destination, int k){

    }

    public static Map countCuts(Graph graph, List<GraphPath> ksp, int capacity, int core, int fs){
        Map<String, Integer> slotCuts = new HashMap<>();
        ArrayList<Map<String, Integer>> bestKspSlot = new ArrayList<>();

        for (int k = 0; k < ksp.size(); k++) {
            System.out.println("----------------------------------");
            System.out.println(ksp.get(k));
            slotCuts = numCuts(ksp.get(k), graph, capacity, core, fs);
            System.out.println("Mejor slot: " + slotCuts.get("slot") + " con " + slotCuts.get("cuts") + " cuts");
            if(bestKspSlot.size() == 0 || slotCuts.get("cuts") < bestKspSlot.get(0).get("cuts")){ //Primera vez o si encuentra encuentra un resultado mejor (menos cuts)
                bestKspSlot.clear();//Limpiamos el array porque pueden haber más de un resultado guardado
                slotCuts.put("ksp", k);//Guardamos el indice del mejor ksp
                bestKspSlot.add(slotCuts);
            }else if(slotCuts.get("cuts") == bestKspSlot.get(0).get("cuts")){//Si tienen igual cantidad de cortes guardamos
                slotCuts.put("ksp", k);
                bestKspSlot.add(slotCuts);
            }
        }
        System.out.println("Best Ksp Slot");
        System.out.println(bestKspSlot);
//        if (slotCuts.size() == 1) //Solo un resultado
//            return bestKspSlot.get(0);

        int finalPath;
        finalPath = alignmentCalc(ksp, graph, bestKspSlot, core);
        return bestKspSlot.get(finalPath);
    }


    public static int alignmentCalc(List<GraphPath> ksp, Graph graph, ArrayList<Map<String, Integer>> kspSlot, int core) {
        int lessMisalign = -1;
        int lessMisalignAux = -1;
        int bestIndex = -1;
        for (Map<String, Integer> k : kspSlot){
            lessMisalignAux = countMisalignment(ksp.get(k.get("ksp")), graph, core);
            if(lessMisalign == -1 || lessMisalignAux < lessMisalign){
                lessMisalign = lessMisalignAux;
                bestIndex = k.get("ksp");
            }
        }
        return bestIndex;
    }

    public static int countMisalignment(GraphPath ksp, Graph graph, int core) {
        System.out.println("countMisalignment");
        System.out.println(ksp);
        System.out.println("LOS VECINOS DE " + ksp.getStartVertex() + " -> " + ksp.getEndVertex() + " SON:");
        for (Object vertex : ksp.getVertexList()){
            for (Object neighbour : graph.outgoingEdgesOf(vertex)){
                System.out.println(neighbour);
                System.out.println(ksp.getEdgeList().contains(neighbour));
                if(!ksp.getEdgeList().contains(neighbour)){//Verificamos que el vecino no este en el camino
                    System.out.println("ES VECINO");
                }
                System.out.println("");
            }
        }

        return 0;
    }

    public static int countMisalignment2(GraphPath ksp, Graph graph, int core) {
        int alignAux = 0;
        int actualLink = -1;
        int nextNextLink = -1;
        int nextLink, iLink, prevLink = -1;
        int nSgteSgte = -1;
        for(int k = 0; k < ksp.getLength() - 2; k++){
            for (int i = 0; i < graph.vertexSet().size(); i++) {
                actualLink = k;
                nextLink = k+1;
                iLink = i;
                try {
                    if (ksp.getEdgeList().get(k+2) != null) {
                        nextNextLink = k+2;
                    }
                } catch (Exception e) {
                    System.out.println("Error No le salio sgte de sgte");
                    e.printStackTrace();
                }
//                if (((Link)ksp.getEdgeList().get(actualLink)) != null && iLink != nextLink && iLink != actualLink && iLink != prevLink) {
//                    if (((Link)ksp.getEdgeList().get(actualLink)).getCores().get(core).getFs().get(fSlots).isFree()) {
//                        alignAux = alignAux + 1;
//                    } else {
//                        alignAux = alignAux - 1;
//                    }
//
//                }
//                if (((Link)ksp.getEdgeList().get(nextLink) != null && iLink != actualLink && iLink != nextLink && iLink != prevLink && iLink != nextNextLink)) {
//                    if (((Link)ksp.getEdgeList().get(nextLink)).getCores().get(core).getFs().get(fSlots).isFree()) {
//                        alignAux = alignAux + 1;
//                    } else {
//                        alignAux = alignAux - 1;
//                    }
//
//                }
            }
            prevLink = actualLink;

        }
        return alignAux;
    }

    public static Map numCuts(GraphPath  ksp, Graph graph, int capacity, int core, int fs) {
        int cuts = -1;
        int slot = -1;
        Map<String, Integer> slotCuts = new HashMap<>();

        ArrayList<Integer> cIndexes;
        int cutAux = 0;
        cIndexes = searchIndexes(ksp, graph, capacity, core, fs);

        for (int slotIndex : cIndexes) {
            if (slotIndex != 0){
                for (Object link : ksp.getEdgeList()) {
                    if (((Link) link).getCores().get(core).getFs().get(slotIndex - 1).isFree()) {
                        cutAux++;//Se encontro un lugar vacio en el slot i - 1 del ksp actual
                    }
                }
            }
            System.out.println("Para el cIndex: " + slotIndex + ", cuts = " + cutAux);
            if (cuts == -1 || cutAux < cuts) {
                cuts = cutAux;
                slot = slotIndex;
            }
            cutAux = 0;
        }


        slotCuts.put("cuts", cuts);
        slotCuts.put("slot", slot);
        return slotCuts;


    }


    public static ArrayList<Integer> searchIndexes(GraphPath ksp, Graph graph, int capacity, int core, int fsQ){
        ArrayList<Integer> indexes = new ArrayList<Integer>();

        ArrayList<Integer> cIndexes = new ArrayList<Integer>();
        boolean free = false;

        for (int i = 0; i < capacity; i++) {
            free = true;
            for (Object path: ksp.getEdgeList()){
                Link link = (Link) path;
                FrecuencySlot fs = link.getCores().get(core).getFs().get(i);
                if(!fs.isFree()){//Se verifica que todo el camino este libre en el slot i
                    free = false;
                    break;
                }
            }
            if (free)
                indexes.add(i);
        }

        //Se quitan indices de slots contiguos
        for (int i = indexes.size() - 1; i > 0; i--) {
            if ((indexes.get(i) - indexes.get(i - 1)) != 1) {
                cIndexes.add(indexes.get(i));
            }
        }
        cIndexes.add(indexes.get(0));
        System.out.println("cIndexes: " + cIndexes);
        return cIndexes;
    }

    public static void assignFs(EstablisedRoute establisedRoute, int core){
        System.out.println("assignFs()");
        for (Object link: establisedRoute.getPath().getEdgeList()){
            for (int i = establisedRoute.getFsIndexBegin(); i < establisedRoute.getFs(); i++){
                ((Link) link).getCores().get(core).getFs().get(i).setFree(false);
                ((Link) link).getCores().get(core).getFs().get(i).setLifetime(establisedRoute.getTimeLife());
            }
        }
        for (Object link: establisedRoute.getPath().getEdgeList()){
            System.out.println("ESPECTRO DEL ENLACE: " + ((Link)link).getFrom() + " -> " + ((Link)link).getTo());
            System.out.print("|");
            for(int i = 0; i < 16; i++){
                if(!((Link) link).getCores().get(core).getFs().get(i).isFree())
                    System.out.print(" x |");
                else
                    System.out.print("  |");
            }
            System.out.println("");
        }
        System.out.println("");
    }
}
