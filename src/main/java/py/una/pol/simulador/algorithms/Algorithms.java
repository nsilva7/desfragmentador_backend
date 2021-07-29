package py.una.pol.simulador.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.SimpleWeightedGraph;
import py.una.pol.simulador.model.*;
import org.jgrapht.Graph;
import py.una.pol.simulador.utils.Utils;
import sun.rmi.runtime.Log;

import java.awt.image.AreaAveragingScaleFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        List<Link> bestKsp = new ArrayList<Link>(kspPlaced.get(selectedPath).getEdgeList());
        EstablisedRoute establisedRoute = new EstablisedRoute(bestKsp, slot, demand.getFs(), demand.getTimeLife(),demand.getSource(),demand.getDestination(), core);
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

    public static Graph aco_def(Graph graph, List<EstablisedRoute> establishedRoutes, int antsq, String metric, int FSminPC, double improvement, String routingAlg, KShortestSimplePaths ksp, int capacity, List<List<GraphPath>> kspList) throws Exception {
        double e0 = System.currentTimeMillis();
        System.out.println("INICIA DESFRAGMENTACIÓN ACO CON: " + establishedRoutes.size() + " RUTAS");
        double[] probabilities = new double[establishedRoutes.size()];
        double[] pheromones = new double[establishedRoutes.size()];
        double[] visibility = new double[establishedRoutes.size()];
        double ro = 0.1;
        double currentImprovement = 0;
        double graphEntropy = 0;
        double graphBFR = 0;
        double graphPC = 0;
        double graphMSI = 0;
        Graph graphAux = null;
        Graph bestGraph = graph;
        boolean success = false;
        ArrayList<Integer> usedIndexes =  new ArrayList<>();
        List<EstablisedRoute> selectedRoutes = new ArrayList<>();
        List<EstablisedRoute> optimalSelectedRoutes = new ArrayList<>();
        List<EstablisedRoute> actualOptimalSelectedRoutes = new ArrayList<>();
        int betterRoutesQ = establishedRoutes.size();
        int count;
        int sameReRouting = 0;

        boolean blocked = false;

        switch (metric) {
            case "ENTROPY":
                graphEntropy = Utils.graphEntropyCalculation(graph);
                break;
            case "PATH_CONSECUTIVENESS":
                graphPC = PathConsecutiveness(Utils.twoLinksRoutes(graph), capacity, FSminPC);
                break;
            case "BFR":
                graphBFR = BFR(graph, capacity);
                break;
            case "MSI":
                graphMSI =MSI(graph);
                break;
        }

        for (int i = 0; i < establishedRoutes.size(); i++) {
            pheromones[i] = 1;
            visibility[i] = visibilityCalc(establishedRoutes.get(i),metric,FSminPC,capacity,graph);
        }

        double summ;
        for (int i = 0; i < antsq; i++) {
            selectedRoutes.clear();
            usedIndexes.clear();
            currentImprovement = 0;
            summ = 0;
            for (int j = 0; j < pheromones.length ; j++) {
                summ += pheromones[j]*visibility[j];
            }

            for (int j = 0; j < probabilities.length; j++) {
                probabilities[j] = pheromones[j]*visibility[j]/summ;
            }
            count = 0;
            sortProbabilities(probabilities);
            while(currentImprovement < improvement && count < establishedRoutes.size()) {
                System.out.println("ANT: " + i + " count: " + count + " de: " + establishedRoutes.size());
                sameReRouting = 0;
                try {
                    graphAux = Utils.deepCopy(graph);

                    int routeIndex = selectRoute(probabilities,usedIndexes);
                    usedIndexes.add(routeIndex);
                    selectedRoutes.add(establishedRoutes.get(routeIndex));

                    for (int j = 0; j < usedIndexes.size(); j++) {
                        Utils.deallocateFs(graphAux,establishedRoutes.get(usedIndexes.get(j)));
                    }
                    if(selectedRoutes.size() > 1) {
                        sortRoutes(selectedRoutes, usedIndexes);
                    }

                    blocked = false;
                    actualOptimalSelectedRoutes.clear();
                    for (int j = 0; j < selectedRoutes.size() ; j++) {
                        Demand demand = new Demand(selectedRoutes.get(j).getFrom(), selectedRoutes.get(j).getTo(), selectedRoutes.get(j).getFs(), selectedRoutes.get(j).getTimeLife());
                        List<GraphPath> kspaths = kspList.get(usedIndexes.get(j));
                        boolean [] tested = new boolean[selectedRoutes.get(j).getPath().get(0).getCores().size()];
                        Arrays.fill(tested, false);
                        int core;
                        while (true){
                            core = Utils.getCore(selectedRoutes.get(j).getPath().get(0).getCores().size(), tested);
                            Class<?>[] paramTypes = {Graph.class, List.class, Demand.class, int.class, int.class};
                            Method method = Algorithms.class.getMethod(routingAlg, paramTypes);
                            List<GraphPath> kspathAux = new ArrayList<>();
                            for (GraphPath<Integer, Link> kspathss : kspaths){
                                   List<Link> edgeList = new ArrayList<>();
                                   double weight = 0;
                                   for ( Link path : kspathss.getEdgeList()){
                                       Link aux = (Link) graphAux.getEdge(path.getFrom(), path.getTo());
                                       edgeList.add(aux);
                                       weight += aux.getDistance();
                                   }
                                   kspathAux.add(new GraphWalk(graphAux, kspathss.getStartVertex(), kspathss.getEndVertex(), edgeList, weight));
                            }
                            Object establisedRoute = method.invoke(Algorithms.class, graphAux, kspathAux, demand, capacity, core);

                            if(establisedRoute == null){
                                tested[core] = true;//Se marca el core probado
                                if(!Arrays.asList(tested).contains(false)){//Se ve si ya se probaron todos los cores
                                    blocked = true;
                                    break;
                                }
                            }else{
                                //Ruta establecida
                                Utils.assignFs((EstablisedRoute)establisedRoute, core);
                                actualOptimalSelectedRoutes.add((EstablisedRoute)establisedRoute);


                                if(Utils.compareRoutes((EstablisedRoute)establisedRoute, selectedRoutes.get(j)))
                                    sameReRouting++;
                                break;
                            }
                        }
                    }
                    if(currentImprovement >= improvement)
                        System.out.println("currentImprovement: " + currentImprovement + " Sale por mejora");
                    if(blocked)
                        currentImprovement = 0;
                    else {
                        currentImprovement = improvementCalculation(graphAux, metric, capacity, graphEntropy, graphBFR, graphMSI, graphPC, FSminPC);
                    }

                    count++;
                    if((selectedRoutes.size() - sameReRouting) > betterRoutesQ) {
                        System.out.println("Sale por break");
                        break;
                    }
                } catch ( NoSuchMethodException  | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if((currentImprovement >= improvement) && betterRoutesQ > actualOptimalSelectedRoutes.size() - sameReRouting){
                System.out.println("Llego con currentImprovement: " + currentImprovement + " en count: " + count + " y ant: " + i);
                optimalSelectedRoutes.clear();
                optimalSelectedRoutes.addAll(selectedRoutes);
                bestGraph =  Utils.deepCopy(graphAux);
                betterRoutesQ = optimalSelectedRoutes.size();
                success = true;
                for(int index : usedIndexes){
                    pheromones[index] += currentImprovement/100;
                }
                break;
            }

            //Evaporar feromonas
            for (int index = 0; index < pheromones.length; index++){
                pheromones[index] *= (1-ro);
            }
        }
        System.out.println("Tiempo de ejecución ACO: " + (System.currentTimeMillis() - e0) + " ms");
        if(success){
            return bestGraph;
        }else{
            return graph;
        }
    }

    private static double improvementCalculation(Graph graph, String metric, int capacity, double graphEntropy, double graphBFR, double graphMSI,double graphPC, int fsMinPC){

        switch (metric) {
            case "ENTROPY":
                double currentGraphEntropy = Utils.graphEntropyCalculation(graph);
                return 100 - currentGraphEntropy*100/graphEntropy;
            case "BFR":
               double currentBFR = BFR(graph, capacity);
               //System.out.println(graphBFR + " - " + currentBFR);
               return 100 - ((roundDecimals(currentBFR, 6) * 100)/roundDecimals(graphBFR, 6));
            case "MSI":
                double currentMSI = MSI(graph);
                return 100 - ((roundDecimals(currentMSI, 6) * 100)/roundDecimals(graphMSI, 6));
            case "PATH_CONSECUTIVENESS":
                double currentPC = PathConsecutiveness(Utils.twoLinksRoutes(graph), capacity, fsMinPC);
                return ((roundDecimals(currentPC, 6) * 100)/roundDecimals(graphPC, 6))-100;
            default:
                return 0;
        }
    }

    public static double visibilityCalc(EstablisedRoute establishedRoute, String metric, int FSminPC, int capacity, Graph g) {
        switch (metric) {
            case "ENTROPY":
                return routeEntropy(establishedRoute);
            case "BFR":
                return routeBFR(establishedRoute,capacity);
            case "PATH_CONSECUTIVENESS":
                List<GraphPath> routes = new ArrayList<>();
                GraphWalk gw = new GraphWalk(g,establishedRoute.getFrom(),establishedRoute.getTo(),establishedRoute.getPath(),1);
                routes.add(gw);
                double pathAux = PathConsecutiveness(routes,capacity,FSminPC);
                return capacity - 1 - pathAux;
            case "MSI":
                return establishedRoute.getFsIndexBegin() + establishedRoute.getFs() - 1;
        }

        return -1;
    }

    public static double roundDecimals(double value, int decimals) {
        double result = value * Math.pow(10, decimals);
        result = Math.round(result);
        result = Math.floor(result);
        result = result / (Math.pow(10, decimals));

        return result;
    }

    public static double PathConsecutiveness (List<GraphPath> twoLinksRoutes, int capacity, int FSMinPC){
        double sum=0;
        boolean so[] = new boolean[capacity];
        boolean goNextBlock;//bandera para avisar que tiene que ir al siguiente bloque
        int cgb = 0;//contador global de bloques
        double CE;
        double joins, cfs;
        int contFSMinPC = 0; //contador para saber si tiene el mínimo de espacio para ser considerado libre
        int cores= 0;
        for(GraphPath route : twoLinksRoutes){
            cores = ((Link)route.getEdgeList().get(0)).getCores().size();
            //Se setean los slots libres
            for(int i = 0; i < cores; i++){
                Arrays.fill(so, false); //Se inicializa todo el espectro como libre
                for (Link link: (List<Link>) route.getEdgeList()){
                    for(int j=0;j<capacity;j++){
                        FrecuencySlot fs = link.getCores().get(i).getFs().get(j);
                        if(!fs.isFree()){
                            so[j] = true;
                        }
                    }
                }

                //pone ocupado los bloques menores al minimo
                for(int j=0;j<capacity;j++){
                    contFSMinPC = 0; //reset
                    if(!so[j]){
                        for(int k=j;k<capacity;k++){
                            if(!so[j]){
                                contFSMinPC++;
                            }else{
                                break;
                            }
                        }
                        if(contFSMinPC < FSMinPC){
                            //poner como ocupados
                            for(int l=0;l < contFSMinPC;l++){
                                so[j + l] = true;
                            }
                        }
                        j = j + contFSMinPC; //para que ya no controle los siguientes que ya controló
                    }
                }

                //calcular cantidad de bloques libres
                goNextBlock = false;
                cgb = 0;
                for(int j=0;j<capacity;j++){
                    if(!so[j] && !goNextBlock){
                        cgb++;
                        goNextBlock = true;
                    }else if (so[j]){
                        goNextBlock = false;
                    }
                }

                cfs = 0;
                joins = 0;
                for(int j=0;j<capacity - 1;j++){ //recorre hasta el penúltimo fs
                    int slot = so[j]?0:1;
                    int nextSlot = so[j+1]?0:1;
                    joins += slot*nextSlot;
                    if(!so[j]){
                        cfs++;
                    }
                }
                //para el ultimo fs
                if(!so[capacity - 1]){
                    cfs++;
                }

                if(cgb==0){
                    CE=0;
                }else{
                    CE = (joins / cgb) * (cfs / capacity);
                }
                sum += CE;
            }

        }

        return sum/twoLinksRoutes.size()*cores;
    }
    public static double BFRLinks(List<Link> links, int capacity) {
        double ocuppiedSlotCount = 0;
        double freeBlockSize = 0;
        double maxBlock = 0;
        double BFRLinks = 0;

        for (Link link: links) {
            ocuppiedSlotCount = 0;
            freeBlockSize = 0;
            maxBlock = 0;
            for (Core core: link.getCores()) {
                for (int i=0; i< capacity; i++){
                    if (core.getFs().get(i).isFree()){
                        freeBlockSize++;
                    }else{
                        if (freeBlockSize > maxBlock ){
                            maxBlock  = freeBlockSize;
                        }
                        freeBlockSize = 0;
                        ocuppiedSlotCount++;
                    }
                }

                if (freeBlockSize > maxBlock ){
                    maxBlock  = freeBlockSize;
                }
                if(capacity != ocuppiedSlotCount)
                     BFRLinks +=  (1 - maxBlock/(capacity-ocuppiedSlotCount));
            }

        }

        return BFRLinks;
    }
    public static double BFR(Graph g, int capacity){
        double BFRLinks = 0;
        int cores;

        List<Link> links = new ArrayList<>();
        links.addAll(g.edgeSet());
        cores = links.get(0).getCores().size();
        BFRLinks = BFRLinks( links,capacity);
        return BFRLinks/g.edgeSet().size()*cores;
    }

    public static double routeBFR(EstablisedRoute route, int capacity){

        double BFRLinks = BFRLinks(route.getPath(),capacity);

        return BFRLinks/route.getPath().size();
    }

    public static double MSILinks(List<Link> links) {
        int greaterFreeIndex = 0;
        double MSILink = 0;
        int cores = 0;
        for (Link link: links) {
            cores = link.getCores().size();
            for (Core core: link.getCores()) {
                greaterFreeIndex = 0;
                for (int i=core.getFs().size() - 1; i >= 0; i--){
                    if (!core.getFs().get(i).isFree()){
                        greaterFreeIndex = i;
                        break;
                    }
                }
                MSILink += greaterFreeIndex;

            }
        }

        return MSILink;
    }

    public static double MSI(Graph g){
        List<Link> links = new ArrayList<>();
        links.addAll(g.edgeSet());
        int cores =links.get(0).getCores().size();
        double MSILink = MSILinks(links);
       // System.out.println("----MSILink: " + MSILink + "----");
        //System.out.println("----g.edgeSet().size(): " + g.edgeSet().size() + "----");
        return MSILink/(g.edgeSet().size()*cores);
    }

    public static double MSIPath(EstablisedRoute route){
        int cores = route.getPath().get(0).getCores().size();
        double MSILink = MSILinks(route.getPath());

        return MSILink/route.getPath().size()*cores;
    }

    public static double routeEntropy(EstablisedRoute establishedRoute) {
        List<FrecuencySlot> fs;
        double uelink=0;

        for (Link link: establishedRoute.getPath()) {
            fs = link.getCores().get(establishedRoute.getCore()).getFs();
            int ueCount = 0;
            for (int i = 0; i < fs.size() - 1 ; i++) {
                if(fs.get(i).isFree() != fs.get(i+1).isFree()){
                    ueCount++;
                }
            }

            uelink += ueCount;
        }

        return uelink/establishedRoute.getPath().size();
    }

    public static int selectRoute(double[] probabilities, ArrayList usedIndexes) {
        int[] indexOrder = new int[probabilities.length];
        int sortAux;

        for (int i = 0; i < probabilities.length; i++) {
            indexOrder[i] = i;
        }

        for (int i = 0; i < probabilities.length -1 ; i++) {
            for (int j = i +1 ; j < probabilities.length; j++) {
                if(probabilities[i] > probabilities[j]) {
                    sortAux = indexOrder[i];
                    indexOrder[i] = indexOrder[j];
                    indexOrder[j] = sortAux;
                }
            }
        }

        double summProb = 0;
        int lastZeroPos = -1;
        for (int i = 0; i < probabilities.length; i++) {
            if(!usedIndexes.contains(i)) {
                summProb += probabilities[i];

                if(probabilities[i] == 0) {
                    lastZeroPos = i;
                }
            }
        }
        if(summProb == 0) return lastZeroPos;

        Random random = new Random();
        double randomValue = summProb * random.nextDouble();
        summProb = 0;
        int index = -1;
        while (summProb <= randomValue) {
            index++;
            if(!usedIndexes.contains(indexOrder[index])) {
                summProb += probabilities[indexOrder[index]];
            }
//            System.out.println("summProb: " + summProb);
//            System.out.println("index: " + index);
        }
        return indexOrder[index];
    }

    public static void sortProbabilities(double[] probabilites){
        double aux;
        int n = probabilites.length;
        for (int i = 0; i <= n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                if(probabilites[i] > probabilites[j]){
                   aux = probabilites[i];
                   probabilites[i] = probabilites[j];
                   probabilites[j] = aux;
                }
            }
        }
    }

    public static void sortRoutes(List<EstablisedRoute> routes, ArrayList<Integer> usedIndexes) {
        for (int i = 0; i < routes.size() - 1 ; i++) {
            for (int j = i + 1; j < routes.size() ; j++) {
                if(routes.get(j).getFs() > routes.get(i).getFs()) {
                    EstablisedRoute routeAux = routes.get(i);
                    routes.set(i,routes.get(j));
                    routes.set(j,routeAux);

                    int indexAux = usedIndexes.get(i);
                    usedIndexes.set(i,usedIndexes.get(j));
                    usedIndexes.set(j,indexAux);
                }
            }
        }
    }

    public static double graphUsePercentage(Graph graph){
        double total = 0;
        double occup = 0;
        List<Link> links = new ArrayList<>();
        links.addAll(graph.edgeSet());
        for (Link link: links) {
            for (Core core: link.getCores()) {
                for(int i = 0; i < core.getFs().size(); i++){
                    if(!core.getFs().get(i).isFree())
                        occup++;
                    total++;
                }
            }
        }

        return occup / total ;
    }

    public static double entropyPerUse(Graph graph, List<EstablisedRoute> establishedRoutes, int capacity){
        List<FrecuencySlot> fs;
        double uelink=0;
        int ueCount;
        int countLinks = 0;
        int occup = 0;
        double used = 0;
        double total = 0;
        double entropy;
        for (EstablisedRoute route : establishedRoutes){
            for(Link link : route.getPath()){
                ueCount = 0;
                for(Core core : link.getCores()){
                    fs = core.getFs();
                    for (int i = 0; i < fs.size() - 1 ; i++) {
                        if(fs.get(i).isFree() != fs.get(i+1).isFree()){
                            ueCount++;
                        }
                        if(!fs.get(i).isFree())
                            occup++;
                    }
                }
                uelink += ueCount;
                countLinks++;
            }

            entropy = uelink / countLinks;
            used = occup / capacity;
            total += (entropy*used);

        }

        return total / establishedRoutes.size();

    }

    public static double externalFragmentation(Graph graph, int capacity){
        double ef = 0;
        int blocksFreeC;
        int maxBlockFree;
        int currentBlockFree;
        List<Link> links = new ArrayList<>();
        links.addAll(graph.edgeSet());
        for(Link link : links){
            for(Core core : link.getCores()){
                maxBlockFree = 0;
                blocksFreeC = 0;
                currentBlockFree = 0;
                for(FrecuencySlot fs : core.getFs()){
                    if(fs.isFree()){
                        //if(currentBlockFree == 0)
                        blocksFreeC++;//Contador global de slots libre
                        currentBlockFree++;//Contador slots libre del bloque actual
                    }else{
                        if(currentBlockFree > maxBlockFree)
                            maxBlockFree = currentBlockFree;
                        currentBlockFree = 0;
                    }
                }
                if(maxBlockFree == 0 && currentBlockFree == capacity)//Para el caso en el que todo el espectro esta libre
                    maxBlockFree = capacity;

                if(maxBlockFree == 0 && blocksFreeC == 0)//Para el caso en el que todo el espectro esta ocupado
                    blocksFreeC = 1;

                if(maxBlockFree == 0 && currentBlockFree != capacity && currentBlockFree != 0)//Para el caso en el que solo se encuentra 1 bloque libre
                    maxBlockFree = currentBlockFree;

                ef += 1 - ((double)maxBlockFree / (double)blocksFreeC);
            }
        }

        return ef / (links.size() * links.get(0).getCores().size());
    }

    public static double shf(Graph graph, int capacity){
        double shf = 0;
        double sf = 0;
        List<Link> links = new ArrayList<>();
        links.addAll(graph.edgeSet());
        for(Link link : links) {
            for (Core core : link.getCores()) {
                sf = 0;
                for (FrecuencySlot fs : core.getFs()){
                    if(fs.isFree())
                        sf++;
                    else {
                        if(sf != 0)  //hasta 1   *  [0, 5.86]
                            shf += ((sf / capacity) * Math.log(capacity / sf));
                        sf = 0;
                    }
                }
                sf = sf;
                if(sf != 0)
                  shf += ((sf / capacity) * Math.log(capacity / sf));
            }
        }
        return shf / links.size() * links.get(0).getCores().size();
    }

}
