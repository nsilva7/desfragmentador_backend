package py.una.pol.simulador.utils;
import com.fasterxml.jackson.databind.JsonNode;
import org.jgrapht.GraphMetrics;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.SimpleWeightedGraph;
import py.una.pol.simulador.algorithms.Algorithms;
import py.una.pol.simulador.model.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.jgrapht.Graph;
import py.una.pol.simulador.socket.SocketClient;
import sun.security.provider.certpath.Vertex;

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

    public static Map countCuts(Graph graph, List<GraphPath> ksp, int capacity, int core, int fs){
        Map<String, Integer> slotCuts = new HashMap<>();
        ArrayList<Map<String, Integer>> bestKspSlot = new ArrayList<>();

        for (int k = 0; k < ksp.size(); k++) {
//            System.out.println("----------------------------------");
//            System.out.println(ksp.get(k));
            slotCuts = numCuts(ksp.get(k), graph, capacity, core, fs);
//            System.out.println("Mejor slot: " + slotCuts.get("slot") + " con " + slotCuts.get("cuts") + " cuts");
            if(bestKspSlot.size() == 0 || slotCuts.get("cuts") < bestKspSlot.get(0).get("cuts")){ //Primera vez o si encuentra encuentra un resultado mejor (menos cuts)
                bestKspSlot.clear();//Limpiamos el array porque pueden haber más de un resultado guardado
                slotCuts.put("ksp", k);//Guardamos el indice del mejor ksp
                bestKspSlot.add(slotCuts);
            }else if(slotCuts.get("cuts") == bestKspSlot.get(0).get("cuts")){//Si tienen igual cantidad de cortes guardamos
                slotCuts.put("ksp", k);
                bestKspSlot.add(slotCuts);
            }
        }
//        if (slotCuts.size() == 1) //Solo un resultado
//            return bestKspSlot.get(0);

        int finalPath;
        finalPath = alignmentCalc(ksp, graph, bestKspSlot, core);
        return bestKspSlot.get(finalPath);
    }


    public static int alignmentCalc(List<GraphPath> ksp, Graph graph, ArrayList<Map<String, Integer>> kspSlot, int core) {
        int lessMisalign = -1;
        int lessMisalignAux;
        int bestIndex = 0;
        int c = 0;
        for (Map<String, Integer> k : kspSlot){
            lessMisalignAux = countMisalignment(ksp.get(k.get("ksp")), graph, core, k.get("slot"));
            if(lessMisalign == -1 || lessMisalignAux < lessMisalign){
                lessMisalign = lessMisalignAux;
//                bestIndex = k.get("ksp");
                bestIndex = c;//Tengo que guardar el indice en kspSlot, no el indice en ksp
            }
            c++;
        }
        return bestIndex;
    }

    public static int countMisalignment(GraphPath ksp, Graph graph, int core, int slot) {
        int missalign = 0;
        for (Object link : ksp.getEdgeList() ){//Por cada enlace
            for (Object fromNeighbour : graph.outgoingEdgesOf(((Link)link).getFrom())){//Vecinos por el nodo origen
                if(!ksp.getEdgeList().contains(fromNeighbour)){//Verificamos que el vecino no este en el camino
                    if(((Link)fromNeighbour).getCores().get(core).getFs().get(slot).isFree())//Si el slot elegido esta ocupado ocurre desalineación
                        missalign++;
                }
            }
            for (Object toNeighbour : graph.outgoingEdgesOf(((Link)link).getTo())){//Vecinos por el nodo destino
                if(!ksp.getEdgeList().contains(toNeighbour)){//Verificamos que el vecino no este en el camino
                    if(((Link)toNeighbour).getCores().get(core).getFs().get(slot).isFree())//Si el slot elegido esta ocupado ocurre desalineación
                        missalign++;
                }
            }
        }
//        System.out.println("DESALINEACIÓN: " + missalign);
        return missalign;
    }

    public static int countNeighbour(GraphPath ksp, Graph graph) {
        List neighbours = new ArrayList();
        for (Object link : ksp.getEdgeList() ){
            for (Object fromNeighbour : graph.outgoingEdgesOf(((Link)link).getFrom())){
                if(!ksp.getEdgeList().contains(fromNeighbour) && !neighbours.contains(fromNeighbour)){
                    neighbours.add(fromNeighbour);
                }
            }
            for (Object toNeighbour : graph.outgoingEdgesOf(((Link)link).getTo())){
                if(!ksp.getEdgeList().contains(toNeighbour) && !neighbours.contains(toNeighbour)){

                    neighbours.add(toNeighbour);
                }
            }
        }
        System.out.println("Vecinos: "+ neighbours);
        return neighbours.size();
    }


    public static int countFreeCapacity(GraphPath ksp, Graph graph,int core, int capacity) {

        int frees = 0;
        for (int i = 0; i < capacity; i++) {
            for (Object path: ksp.getEdgeList()){
                Link link = (Link) path;
                FrecuencySlot fs = link.getCores().get(core).getFs().get(i);
                if(fs.isFree()){
                    frees++;
                }
            }
        }

        return frees;
    }


    public static Map numCuts(GraphPath  ksp, Graph graph, int capacity, int core, int fs) {
        int cuts = -1;
        int slot = -1;
        Map<String, Integer> slotCuts = new HashMap<>();

        ArrayList<Integer> cIndexes;
        int cutAux = 0;
        cIndexes = searchIndexes(ksp, graph, capacity, core, fs, true);

        for (int slotIndex : cIndexes) {
            if (slotIndex != 0){
                for (Object link : ksp.getEdgeList()) {
                    if (((Link) link).getCores().get(core).getFs().get(slotIndex - 1).isFree()) {
                        cutAux++;//Se encontro un lugar vacio en el slot i - 1 del ksp actual
                    }
                }
            }
//            System.out.println("Para el cIndex: " + slotIndex + ", cuts = " + cutAux);
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

    public static List<GraphPath> twoLinksRoutes(Graph g) {
        List<GraphPath> paths = new ArrayList<>();
        for (int i = 0; i < g.vertexSet().size() ; i++) {
            for(Object link1: g.outgoingEdgesOf(i)) {
                for(Object link2: g.outgoingEdgesOf(((Link)link1).getTo())) {
                    if(!link1.equals(link2)){
                        List<Link> path = new ArrayList<>();
                        path.add(((Link)link1));
                        path.add(((Link)link2));

                        GraphWalk gw = new GraphWalk(g,i,((Link)link2).getTo(),path,1);
                        paths.add(gw);
                    }

                }
            }
        }

        return paths;
    }


    public static ArrayList<Integer> searchIndexes(GraphPath ksp, Graph graph, int capacity, int core, int fsQ, boolean checkFs){
        ArrayList<Integer> indexes = new ArrayList<Integer>();

        ArrayList<Integer> cIndexes = new ArrayList<Integer>();
        boolean free;
        boolean canBeCandidate = true;//Inicialmente el primer slot puede ser candidato
        int slots = 0;
        for (int i = 0; i < capacity; i++) {
            free = true;
            for (Object path: ksp.getEdgeList()){
                Link link = (Link) path;
                FrecuencySlot fs = link.getCores().get(core).getFs().get(i);
                if(!fs.isFree()){//Se verifica que todo el camino este libre en el slot i
                    free = false;
                    canBeCandidate = true;//Cuando encuentra un slot ocupado entonces el siguiente puede ser candidato
                    slots = 0;
                    break;
                }
            }
            if(free)//Si esta libre se aumenta el contador
                slots++;
            if (slots == fsQ && canBeCandidate){//Si puede contener la cantidad de fs y es candidadto valido entonces se agrega
                indexes.add(i - fsQ + 1);
                slots = 0;
                canBeCandidate = false;
            }
        }
        return indexes;
    }

    public static void assignFs(EstablisedRoute establisedRoute, int core){
        for (Object link: establisedRoute.getPath()){
            for (int i = establisedRoute.getFsIndexBegin(); i < establisedRoute.getFsIndexBegin() + establisedRoute.getFs(); i++){
                ((Link) link).getCores().get(core).getFs().get(i).setFree(false);
                ((Link) link).getCores().get(core).getFs().get(i).setLifetime(establisedRoute.getTimeLife());
            }
        }
    }

    public static void deallocateFs(Graph graph, EstablisedRoute establisedRoute){
        int core = establisedRoute.getCore();
        for (Link link: establisedRoute.getPath()){
            Link linkAux = (Link) graph.getEdge(link.getFrom(),link.getTo());
            for (int i = establisedRoute.getFsIndexBegin(); i < establisedRoute.getFsIndexBegin() + establisedRoute.getFs(); i++){
                linkAux.getCores().get(core).getFs().get(i).setFree(true);
                linkAux.getCores().get(core).getFs().get(i).setLifetime(0);
            }
        }
    }

    public static int getCore(int limit, boolean [] tested){
        Random r = new Random();
        int core = r.nextInt(limit);
        while(tested[core]){
            core = r.nextInt(limit);
        }
        tested[core] = true;
        return core;
    }

    public static double graphEntropyCalculation(Graph graph){
        List<FrecuencySlot> fs;
        double uelink=0;
        int ueCount = 0;
        int cores = 0;
        for (Object link: graph.edgeSet()) {
            cores = ((Link)link).getCores().size();
            for(int core = 0; core < cores; core++){
                fs = ((Link)link).getCores().get(core).getFs();
                ueCount = 0;
                for (int i = 0; i < fs.size() - 1 ; i++) {
                    if(fs.get(i).isFree() != fs.get(i+1).isFree()){
                        ueCount++;
                    }
                }
            }
            uelink += ueCount;
        }

        return uelink/graph.edgeSet().size()*cores;
    }

    public static double getPredIA(Graph graph, int FSMinPC, int capacity, int slotsBlocked, SocketClient client, BufferedWriter writer, Boolean blocked) throws IOException {
        double pred;
        double entropy = graphEntropyCalculation(graph);
        double pc = Algorithms.PathConsecutiveness(twoLinksRoutes(graph), capacity, FSMinPC);
        double bfr = Algorithms.BFR(graph, capacity);
        double shf = Algorithms.shf(graph, capacity);
        double msi = Algorithms.MSI(graph);
        double used = Algorithms.graphUsePercentage(graph);

        String json = "{" +
                                  "\"entropy\": " + String.format(Locale.US,("%.6f"), entropy) +
                                 ",\"pc\":" + String.format(Locale.US,("%.6f"), pc )+
                                 ",\"bfr\":" + String.format(Locale.US,("%.6f"), bfr )+
                                 ",\"shf\":" + String.format(Locale.US,("%.6f"), shf) +
                                 ",\"msi\":" + String.format(Locale.US,("%.6f"), msi) +
                                 ",\"used\":" + String.format(Locale.US,("%.6f"), used) +
                                 ",\"blocked\":" + slotsBlocked +
                                 "}";
        URL url = new URL("http://127.0.0.1:5000/estimador/ratio");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-type", "application/json");
//        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        con.setConnectTimeout(30000);
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            pred = Double.parseDouble(response.toString());
        }

        //String response = client.sendMessage(json);
        //pred = Double.parseDouble(response);


        if(blocked){
            writer.write(
                    String.format(Locale.US,("%.6f"),entropy )+ " , " +
                            String.format(Locale.US,("%.6f"), pc )+ " , " +
                            String.format(Locale.US,("%.6f"), msi ) + " , " +
                            String.format(Locale.US,("%.6f"), bfr )+ " , " +
                            String.format(Locale.US,("%.6f"), shf )+ " , " +
                            String.format(Locale.US,("%.6f"), used )+ " , " +
                            slotsBlocked + " , " +
                            String.format(Locale.US,("%.6f"), pred )+ " , "
            );
            writer.newLine();
        }

        return pred;
    }


    public static Graph copyGraph(Graph graph) {
        Graph<Integer, Link> copy = new SimpleWeightedGraph<>(Link.class);

        for (int i = 0; i < graph.vertexSet().size(); i++) {
            System.out.println(i);
            copy.addVertex(i);
        }
        List<Link> links = new ArrayList<>();
        links.addAll(graph.edgeSet());
        for(Link link : links) {
            List<Core> cores = new ArrayList<>();
            for (Core core : link.getCores()) {
                List<FrecuencySlot> espectro = new ArrayList<>();

                for (FrecuencySlot fs : core.getFs()){
                    FrecuencySlot fsCopy = new FrecuencySlot(core.getBandwidth()/ core.getFs().size());
                    fsCopy.setFree(fs.isFree());
                    fsCopy.setLifetime(fs.getLifetime());
                    espectro.add(fsCopy);
                }
                Core copyCore = new Core(core.getBandwidth(),espectro);
                cores.add(copyCore);
            }

            Link clink = new Link(link.getDistance(),cores, link.getFrom(), link.getTo());
            copy.addEdge(link.getFrom(),link.getTo(),clink);
            copy.setEdgeWeight(clink,link.getDistance());
        }
        return copy;
    }

    public static boolean compareRoutes(EstablisedRoute r1, EstablisedRoute r2){
        if(r1.getTimeLife() != r2.getTimeLife() ||
                r1.getFs() != r2.getFs() ||
                r1.getFsIndexBegin() != r2.getFsIndexBegin() ||
                r1.getFrom() != r2.getFrom() ||
                r1.getTo() != r2.getTo()
        ){
            return false;
        }

        if(r1.getPath().size() != r2.getPath().size())
            return false;
        String rs1, rs2;
        for (int l = 0; l < r1.getPath().size(); l++){
            rs1 = r1.getPath().get(l).getFrom() + "-" + r1.getPath().get(l).getTo();
            rs2 = r2.getPath().get(l).getFrom() + "-" + r2.getPath().get(l).getTo();
            if(!rs1.equals(rs2))
                return false;
        }
        return true;
    }

    public static <T> T deepCopy(T obj)
            throws Exception
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);

        out.writeObject(obj);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);

        obj = (T) in.readObject();
        return obj;
    }
}
