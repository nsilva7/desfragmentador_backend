package py.una.pol.simulador.utils;
import org.jgrapht.GraphPath;
import py.una.pol.simulador.model.Demand;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

    public static int countCuts(Graph graph, List<GraphPath> ksp, int capacity, int core){
        int[] cutsSlot;
        ArrayList<Integer> finalCuts = new ArrayList<>();
        ArrayList<Integer> finalPaths = new ArrayList<>();
        ArrayList<Integer> fSlots = new ArrayList<>();

        ArrayList<Integer> indicesL = new ArrayList<>();
        int finalPath = 0;
        for (int k = 0; k < ksp.size(); k++) {
            cutsSlot = numCuts(ksp.get(k), graph, capacity, core);
            if( cutsSlot != null){
                if (finalCuts.size() < 1) {//Primera vez entra aca
                    finalCuts.add(cutsSlot[0]);
                    finalPaths.add(k);
                    fSlots.add(cutsSlot[1]);
                } else if (finalCuts.get(finalCuts.size() - 1) > cutsSlot[0]) {
                    finalCuts.set(finalCuts.size() - 1, cutsSlot[1]);
                    finalPaths.set(finalPaths.size() - 1, k);
                    fSlots.set(fSlots.size() - 1, cutsSlot[1]);
                } else if (finalCuts.get(finalCuts.size() - 1) == cutsSlot[0]) {
                    finalCuts.add(cutsSlot[1]);
                    finalPaths.add(k);
                    fSlots.add(cutsSlot[1]);
                }
            }
        }

        if (finalCuts.size() > 1) {
            finalPath = alignmentCalc(ksp, graph, capacity, finalPaths, indicesL, fSlots, core);
        } else if (finalCuts.size() == 1) {
            finalPath = finalPaths.get(0);
        } else {
            System.out.println("Error");
        }

        return finalPath;
    }


    public static int alignmentCalc(List<GraphPath> ksp, Graph graph, int capacity, ArrayList<Integer> pahts, ArrayList<Integer> cIndexes, ArrayList<Integer> fSlots, int core) {
        Integer[] alineacion = new Integer[cIndexes.size()];
        int alineacionAux = 0;
        int alineacionFinal = 999;
        int indiceFinal = -1;
        ArrayList<Integer> desalineado = new ArrayList<Integer>();

        for (int k = 0; k < pahts.size(); k++) {

            alineacionAux = countMisalignment(ksp.get(pahts.get(k)), graph, capacity, cIndexes, fSlots.get(k), core);

            if (alineacionAux < alineacionFinal) {
                alineacionFinal = alineacionAux;
                indiceFinal = pahts.get(k);
            }
        }
        System.out.println("Indice Final: " + indiceFinal);
        return indiceFinal;
    }

    public static int countMisalignment(GraphPath ksp, Graph graph, int capacity, ArrayList<Integer> cIndexes, int fSlots, int core) {
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
                if (((Link)ksp.getEdgeList().get(actualLink)) != null && iLink != nextLink && iLink != actualLink && iLink != prevLink) {
                    if (((Link)ksp.getEdgeList().get(actualLink)).getCores().get(core).getFs().get(fSlots).isFree()) {
                        alignAux = alignAux + 1;
                    } else {
                        alignAux = alignAux - 1;
                    }

                }
                if (((Link)ksp.getEdgeList().get(nextLink) != null && iLink != actualLink && iLink != nextLink && iLink != prevLink && iLink != nextNextLink)) {
                    if (((Link)ksp.getEdgeList().get(nextLink)).getCores().get(core).getFs().get(fSlots).isFree()) {
                        alignAux = alignAux + 1;
                    } else {
                        alignAux = alignAux - 1;
                    }

                }
            }
            prevLink = actualLink;

        }
        return alignAux;
    }

    public static int[] numCuts(GraphPath  ksp, Graph graph, int capacity, int core) {
         int cuts = -1;
        int slots = -1;
        int[] cutsSlots = new int[2];

        ArrayList<Integer> cIndexes = new ArrayList<Integer>();
        int cutAux = 0;
        cIndexes = searchIndexes(ksp, graph, capacity, core);

        if (cIndexes.size() == 1 && cIndexes.get(0) == 0) {
            cuts = 0;
            slots = 0;
        } else {
            for (int i = 0; i < cIndexes.size(); i++) {
                System.out.println("KSP.GETLENTH(): " + ksp.getLength());
                for(int k = 0; k < ksp.getLength() - 2; k++){
                    System.out.println("k: " + k);
                    Object nextLink = ksp.getEdgeList().get(k);
                    if (cIndexes.get(i) != 0 && cIndexes.get(i) < capacity - 1) {
                        if(((Link) nextLink).getCores().get(core).getFs().get(cIndexes.get(i) - 1).isFree() &&
                           ((Link) nextLink).getCores().get(core).getFs().get(cIndexes.get(i) + 1).isFree()) {
                            cutAux = cutAux + 1;

                        }
                    }

                }
                if (cutAux < cuts) {
                    cuts = cutAux;
                    slots = i;
                }
                cutAux = 0;
            }
        }

        if (cuts != -1 && slots != -1) {
            cutsSlots[0] = cuts;
            cutsSlots[1] = slots;

            return cutsSlots;
        }

        return null;

    }

    public static ArrayList<Integer> searchIndexes(GraphPath ksp, Graph graph, int capacity, int core){
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
