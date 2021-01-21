package py.una.pol.simulador.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.GraphWalk;
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

    public static boolean aco_def(Graph graph, List<EstablisedRoute> establishedRoutes, int antsq, String metric, int FSminPC, double improvement, String routingAlg, KShortestSimplePaths ksp, int capacity) throws JsonProcessingException {
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
        int count;
        int sameReRouting = 0;
        ObjectMapper objectMapper = new ObjectMapper();

        boolean blocked = false;

        switch (metric) {
            case "Entropía":
                graphEntropy = Utils.graphEntropyCalculation(graph);
                break;
            case "Path Consecutiveness":
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
            sameReRouting = 0;
            for (int j = 0; j < pheromones.length ; j++) {
                summ += pheromones[j]*visibility[j];
            }

            for (int j = 0; j < probabilities.length; j++) {
                probabilities[j] = pheromones[j]*visibility[j]/summ;
            }
            count = 0;
            while(currentImprovement < improvement && count < establishedRoutes.size()) {
                try {
                    graphAux = objectMapper.readValue(objectMapper.writeValueAsString(graph), Graph.class);
                    int routeIndex = selectRoute(probabilities,usedIndexes);
                    usedIndexes.add(routeIndex);
                    selectedRoutes.add(establishedRoutes.get(routeIndex));
                    Utils.deallocateFs(graphAux,establishedRoutes.get(routeIndex));

                    if(selectedRoutes.size() > 1) {
                        sortRoutes(selectedRoutes, usedIndexes);
                    }
                    blocked = false;
                    for (int j = 0; j < selectedRoutes.size() ; j++) {
                        Demand demand = new Demand(selectedRoutes.get(j).getFrom(), selectedRoutes.get(j).getTo(), selectedRoutes.get(j).getFs(), selectedRoutes.get(j).getTimeLife());
                        List<GraphPath> kspaths = ksp.getPaths(demand.getSource(), demand.getDestination(), 5);
                        boolean [] tested = new boolean[selectedRoutes.get(j).getPath().get(0).getCores().size()];
                        Arrays.fill(tested, false);
                        int core;
                        while (true){
                            core = Utils.getCore(selectedRoutes.get(j).getPath().get(0).getCores().size(), tested);
                            Class<?>[] paramTypes = {Graph.class, List.class, Demand.class, int.class, int.class};
                            Method method = Algorithms.class.getMethod(routingAlg, paramTypes);
                            Object establisedRoute = method.invoke(Algorithms.class, graphAux, kspaths, demand, capacity, core);
                            if(establisedRoute == null){
                                tested[core] = true;//Se marca el core probado
                                if(!Arrays.asList(tested).contains(false)){//Se ve si ya se probaron todos los cores
                                    blocked = true;
                                    break;
                                }
                            }else{
                                //Ruta establecida
                                Utils.assignFs((EstablisedRoute)establisedRoute, core);
                                if(establisedRoute.equals(selectedRoutes.get(j)))
                                    sameReRouting++;
                                break;
                            }
                        }
                    }
                    if(blocked)
                        currentImprovement = 0;
                    else
                        currentImprovement = improvementCalculation(graphAux, metric, capacity,graphEntropy,graphBFR,graphMSI,graphPC,FSminPC);
                    count++;
                } catch (JsonProcessingException | NoSuchMethodException  | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            if((currentImprovement >= improvement) && (selectedRoutes.size() - sameReRouting < optimalSelectedRoutes.size() || optimalSelectedRoutes.size() == 0)){
                optimalSelectedRoutes.clear();
                optimalSelectedRoutes.addAll(selectedRoutes);
                bestGraph = objectMapper.readValue(objectMapper.writeValueAsString(graphAux), Graph.class);
                success = true;
                for(int index : usedIndexes){
                    pheromones[index] += currentImprovement/100;
                }
            }

            //Evaporar feromonas
            for (int index = 0; index < pheromones.length; index++){
                pheromones[index] *= (1-ro);
            }
        }
        if(success){
            graph = bestGraph;
        }
        return success;
    }

    private static double improvementCalculation(Graph graph, String metric, int capacity, double graphEntropy, double graphBFR, double graphMSI,double graphPC, int fsMinPC){

        switch (metric) {
            case "ENTROPIA":
                double currentGraphEntropy = Utils.graphEntropyCalculation(graph);
                return 100 - currentGraphEntropy*100/graphEntropy;
            case "BFR":
               double currentBFR = BFR(graph, capacity);
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
            case "ENTROPIA":
                return routeEntropy(establishedRoute);
            case "BFR":
                return routeBFR(establishedRoute,capacity);
            case "PATH_CONSECUTIVENESS":
                List<GraphPath> routes = new ArrayList<>();
                GraphWalk gw = new GraphWalk(g,establishedRoute.getFrom(),establishedRoute.getTo(),establishedRoute.getPath(),1);
                routes.add(gw);
                double pathAux = PathConsecutiveness(routes,capacity,FSminPC);
                return capacity - 1 - pathAux;
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
                for(int j=0;j<capacity;j++){
                    for (Link link: (List<Link>) route.getEdgeList()){
                        FrecuencySlot fs = link.getCores().get(i).getFs().get(j);
                        if(!fs.isFree()){
                            so[i] = true;
                        }
                    }

                }

                //pone ocupado los bloques menores al minimo
                for(int j=0;j<capacity;j++){
                    contFSMinPC = 0; //reset
                    if(so[j]){
                        for(int k=j;k<capacity;k++){
                            if(so[j]){
                                contFSMinPC++;
                            }else{
                                break;
                            }
                        }
                        if(contFSMinPC < FSMinPC){
                            //poner como ocupados
                            for(int l=0;l < contFSMinPC;l++){
                                so[j + l] = false;
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
        int cores = 0;

        for (Link link: links) {
            cores = link.getCores().size();
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

                BFRLinks +=  (1 - maxBlock/(capacity-ocuppiedSlotCount));
            }

        }

        return BFRLinks;
    }
    public static double BFR(Graph g, int capacity){
        double ocuppiedSlotCount = 0;
        double freeBlockSize = 0;
        double maxBlock = 0;
        double BFRLinks = 0;
        int cores = 0;

        BFRLinks = BFRLinks( (List<Link>) g.edgeSet(),capacity);

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
                for (int i=core.getFs().size() - 1; i >= 0; i--){
                    if (core.getFs().get(i).isFree()){
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
        int cores =( (List<Link>)g.edgeSet()).get(0).getCores().size();
        double MSILink = MSILinks((List<Link>)g.edgeSet());

        return MSILink/g.edgeSet().size()*cores;
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

        for (int i = 0; i < probabilities.length -1 ; i++) {
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
        }
        return indexOrder[index];
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


}
