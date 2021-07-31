package py.una.pol.simulador.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.geom.Edge;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import py.una.pol.simulador.model.*;
import py.una.pol.simulador.socket.SocketClient;
import py.una.pol.simulador.utils.ResourceReader;
import py.una.pol.simulador.utils.Utils;
import py.una.pol.simulador.algorithms.Algorithms;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.rmi.CORBA.Util;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

@RestController
public class SimuladorController {
    private final SimpMessagingTemplate template;
    private static Object LOCK = new Object();
    @Autowired
    SimuladorController(SimpMessagingTemplate template){
        this.template = template;
    }


    @Autowired
    SocketClient socketClient;

//    @CrossOrigin(origins = "http://localhost:4300")
//    @PostMapping("/simular")
    @MessageMapping("/simular")
    public void simular(@RequestBody Options options) throws Exception {

        //socketClient.startConnection("127.0.0.1",9999);
        List<Demand> demands;
        List<EstablisedRoute> establishedRoutes = new ArrayList<EstablisedRoute>();
        Graph net = createTopology2("usnet.json", options.getCores(), options.getFsWidth(), options.getCapacity());
        List<List<GraphPath>> kspList = new ArrayList<>();
        int FSMinPC = (int) (options.getFsRangeMax() - ((options.getFsRangeMax() - options.getFsRangeMin()) * 0.3));
        FileWriter file = new FileWriter("bloqueos.csv");
        BufferedWriter writer = new BufferedWriter(file);
        double pred = 0;
        int slotsBlocked;
        int demandsQ = 0;
        int defragsQ = 0, blocksQ = 0, defragsF = 0;
        double ia_prob = 0.3;
        int aco_improv = 20;
        int antsq = 20;
        int last_defrag_time = 0;
        int tmin = 10;//Shortest interval between adjacent DF operations when the last failed
        int period = 100;
        boolean defragS = true;
        String aco_def_metric = "BFR";
        String demands_type = "Fijo";
        Map<String, Graph> defResult = new HashMap<>();
        writer.write("Entropy, Pc, Msi, Bfr, Shf, % Uso, Slots Bloqueados, Prediccion");
        writer.newLine();

        for (int i = 0; i < options.getTime(); i++) {
            boolean blocked = false;
            System.out.println("Tiempo: " + (i+1) + ", Predicción: " + pred + ", Cantidad de rutas activas: " + establishedRoutes.size());
            demands = Utils.generateDemands(
                    options.getLambda(), options.getTime(),
                    options.getFsRangeMin(), options.getFsRangeMax(),
                    net.vertexSet().size(), options.getErlang() / options.getLambda());


            KShortestSimplePaths ksp = new KShortestSimplePaths(net);
            slotsBlocked = 0;
            demandsQ += demands.size();

            if(pred >= ia_prob && (defragS || (i - last_defrag_time >= tmin))){

                System.out.println("Bfr antes de aco: " + Algorithms.BFR(net, options.getCapacity()));
                defResult = Algorithms.aco_def(net,establishedRoutes,antsq,aco_def_metric,FSMinPC,aco_improv,options.getRoutingAlg(),ksp,options.getCapacity(), kspList);

                defragsQ++;
                if(defResult.get("graph") == null){
                    System.out.println("Fallo desfragmentacion Aco");
                    defragsF++;
                    last_defrag_time = i;
                    defragS = false;
                }else{
                    net = defResult.get("graph");
                    defragS = true;
                }
                System.out.println("Bfr despues de aco: " + Algorithms.BFR(net, options.getCapacity()));

            }
            for(Demand demand : demands){
                //k caminos más cortos entre source y destination de la demanda actual
                List<GraphPath> kspaths = ksp.getPaths(demand.getSource(), demand.getDestination(), 5);
                try {
                    boolean [] tested = new boolean[4];
                    Arrays.fill(tested, false);
                    int core;
                    while (true){
                        core = getCore(options.getCores(), tested);
                        Class<?>[] paramTypes = {Graph.class, List.class, Demand.class, int.class, int.class};
                        Method method = Algorithms.class.getMethod(options.getRoutingAlg(), paramTypes);
                        Object establisedRoute = method.invoke(this, net, kspaths, demand, options.getCapacity(), core);
                        if(establisedRoute == null){
                            tested[core] = true;//Se marca el core probado
                            if(!Arrays.asList(tested).contains(false)){//Se ve si ya se probaron todos los cores
                                //Bloqueo
                                //System.out.println("BLOQUEO");
                                blocked = true;
                                //System.out.println("Va a desfragmentar con :" + establishedRoutes.size() + " rutas");
                                ///if((defragS || (i - last_defrag_time >= tmin))){
                                    //defragS = Algorithms.aco_def(net,establishedRoutes,antsq,aco_def_metric,FSMinPC,aco_improv,options.getRoutingAlg(),ksp,options.getCapacity(), kspList);
                                    //defragsQ++;
                                    //if(!defragS){
                                    //    defragsF++;
                                    //    last_defrag_time = i;
                                    //}
                                //}
                                demand.setBlocked(true);
                                //this.template.convertAndSend("/message",  demand);
                                //break;
                                slotsBlocked += demand.getFs();
                                blocksQ++;
                            }
                        }else{
                            //Ruta establecida
                            establishedRoutes.add((EstablisedRoute) establisedRoute);
                            kspList.add(kspaths);
                            Utils.assignFs((EstablisedRoute)establisedRoute, core);
                            //this.template.convertAndSend("/message",  establisedRoute);
                            //break;
                        }
                        if(establisedRoute != null || demand.getBlocked())
                            break;
                    }
                }catch (java.lang.Exception e){
                    e.printStackTrace();
                }
                try {
                    //wait = 1000/demands.size();
                    //TimeUnit.MILLISECONDS.sleep(wait);
                    //Object.wait (wait);
                }catch (java.lang.Exception e){
                    e.printStackTrace();
                }
            }
            pred = Utils.getPredIA(net, FSMinPC, options.getCapacity(), slotsBlocked, socketClient, writer, blocked);
            for(EstablisedRoute route : establishedRoutes){
                route.subTimeLife();
            }

            for (int ri = 0; ri < establishedRoutes.size(); ri++){
                EstablisedRoute route = establishedRoutes.get(ri);
                if(route.getTimeLife() == 0){
                    establishedRoutes.remove(ri);
                    kspList.remove(ri);
                    ri--;
                }
            }


            ReleasedSlots rSlots = new ReleasedSlots();
            rSlots.setTime(i + 2);
            rSlots.setReleased(true);
            rSlots.setReleasedSlots(this.setTimeLife(net));
            //this.template.convertAndSend("/message", rSlots);

        }
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("end", true);
        //this.template.convertAndSend("/message",  map);
        //socketClient.stopConnection();
        System.out.println("Desfragmentaciones en bloqueos");
        System.out.println(options.getErlang() + " erlangs, " + ia_prob + " para desfragmentar, " + aco_improv + " de mejora, " + antsq + " hormigas" + ", tipo de demandas " + demands_type);
        System.out.println("Cantidad de demandas: " + demandsQ);
        System.out.println("Cantidad de bloqueos: " + blocksQ);
        System.out.println("Cantidad de defragmentaciones: " + defragsQ);
        System.out.println("Cantidad de desfragmentaciones fallidas: " + defragsF);
        System.out.println("Fin Simulación");
        writer.close();
    }

    private ArrayList<Map<String, String>> setTimeLife(Graph net){
        Map<String, String> releasedSlot = new LinkedHashMap<>();
        ArrayList<Map<String, String>> releasedSlots = new ArrayList<>();
        boolean released;
        FrecuencySlot slot;
        for(Object link : net.edgeSet()){
            for(int core = 0; core < ((Link) link).getCores().size(); core++){
                for(int fs = 0; fs < ((Link) link).getCores().get(core).getFs().size(); fs++){
                    slot = ((Link)link).getCores().get(core).getFs().get(fs);
                    released = slot.subLifetime();
                    if(released){
                        releasedSlot.put("released", "true");
                        releasedSlot.put("link", "l"  + ((Link) link).getFrom() + ((Link) link).getTo());
                        releasedSlot.put("core", Integer.toString(core));
                        releasedSlot.put("slot", Integer.toString(fs));
                        releasedSlots.add(releasedSlot);
//                        this.template.convertAndSend("/message",  releasedSlot);
                    }
                }
            }
        }
        return releasedSlots;
    }

    @GetMapping("/getTopology")
    public String getTopología() {
       /* Graph g = createTopology2("nsfnet.json",4,12.5,350);
        KShortestSimplePaths ksp = new KShortestSimplePaths(g);

            //k caminos más cortos entre source y destination de la demanda actual
            List<GraphPath> kspaths = ksp.getPaths(2, 6, 5);
            comprobarKspVocConfia(kspaths);
        DOTExporter<Integer, Link> exporter =
                new DOTExporter<>(v -> v.toString().replace('.', '_'));
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        Writer writer = new StringWriter();
        exporter.exportGraph(g, writer);
        return writer.toString();*/
       return "x";
    }

    private int getCore(int limit, boolean [] tested){
        Random r = new Random();
        int core = r.nextInt(limit);
        while(tested[core]){
            core = r.nextInt(limit);
        }
        tested[core] = true;
        return core;
    }

    private Graph createTopology(String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Graph<Integer, Link> g = new SimpleDirectedGraph<>(Link.class);
            InputStream is = ResourceReader.getFileFromResourceAsStream(fileName);
            JsonNode object = objectMapper.readTree(is);

            for (int i = 0; i < object.get("network").size(); i++) {
                g.addVertex(i);

            }
            int vertex = 0;
            for (JsonNode node: object.get("network")) {
                for (int i = 0; i < node.get("connections").size(); i++) {
                    int connection = node.get("connections").get(i).intValue();
                    int distance = node.get("distance").get(i).intValue();
                    List<Core> cores = new ArrayList<>();

                    for (JsonNode coreNode: node.get("links").get(i).get("cores")) {
                        Core core = new Core(coreNode.get("fs_brandwith").doubleValue(),coreNode.get("fs_available").intValue());
                        cores.add(core);
                    }


                    Link link = new Link(distance,cores, vertex, i);
                    if(g.addEdge(vertex,connection,link)) {
                        System.out.println("Add edge "+ vertex + " -> " + connection);
                        System.out.println("Link: "+ link);
                    }


                }
                vertex++;
            };
             return g;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Graph createTopology2(String fileName, int numberOfCores, double fsWidh, int numberOffs) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            //Graph<Integer, Link> g = new SimpleDirectedGraph<>(Link.class);
//            Graph<Integer, Link> g = new SimpleDirectedWeightedGraph<>(Link.class);
            Graph<Integer, Link> g = new SimpleWeightedGraph<>(Link.class);
            InputStream is = ResourceReader.getFileFromResourceAsStream(fileName);
            JsonNode object = objectMapper.readTree(is);

            for (int i = 0; i < object.get("network").size(); i++) {
                g.addVertex(i);
            }
            int vertex = 0;
            for (JsonNode node: object.get("network")) {
                for (int i = 0; i < node.get("connections").size(); i++) {
                    int connection = node.get("connections").get(i).intValue();
                    int distance = node.get("distance").get(i).intValue();
                    List<Core> cores = new ArrayList<>();

                    for (int j = 0; j < numberOfCores; j++){
                        Core core = new Core(fsWidh,numberOffs);
                        cores.add(core);
                    }

                    Link link = new Link(distance,cores, vertex, connection);
                    g.addEdge(vertex,connection,link);
                    g.setEdgeWeight(link,distance);
                }
                vertex++;
            };
            return g;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Graph createTopologyWithFs(String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            //Graph<Integer, Link> g = new SimpleDirectedGraph<>(Link.class);
            Graph<Integer, Link> g = new SimpleWeightedGraph<>(Link.class);
            InputStream is = ResourceReader.getFileFromResourceAsStream(fileName);
            JsonNode object = objectMapper.readTree(is);

            for (int i = 0; i < object.get("network").size(); i++) {
                g.addVertex(i);
            }
            int vertex = 0;
            for (JsonNode node: object.get("network")) {
                for (int i = 0; i < node.get("connections").size(); i++) {
                    int connection = node.get("connections").get(i).intValue();
                    int distance = node.get("distance").get(i).intValue();
                    List<Core> cores = new ArrayList<>();

                    Core core = new Core(12.5,12);
                    cores.add(core);

                    Link link = new Link(distance,cores, vertex, connection);
                    int slot = 0;
                    for (JsonNode coreNode: node.get("links").get(i)) {
                        link.getCores().get(0).getFs().get(slot).setFree(coreNode.intValue() == 0 ? true : false);
                        slot++;
                    }
//                    g.add
                    g.addEdge(vertex,connection,link);
                    g.setEdgeWeight(link,distance);
                }
                vertex++;
            }
            return g;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    private void comprobarKspVocConfia(List<GraphPath> kspaths){
        for(GraphPath kspath : kspaths){
            System.out.println("--------De :" + kspath.getStartVertex() + " a :" + kspath.getEndVertex() + "--------");
            for (Object path: kspath.getEdgeList()){
                System.out.println(path);
            }
        }
    }

    private void estadoDeFs(Graph net){
        System.out.println("ESTADO DE LOS FS");
        Link link;
        for (Object edge : net.edgeSet()){
            link = (Link)edge;
            System.out.println(link.getFrom() + " -> " + link.getTo());
            System.out.print("|");
            for(int i = 0; i < 12; i++){
                if(!link.getCores().get(0).getFs().get(i).isFree())
                    System.out.print(" x |");
                else
                    System.out.print("  |");
            }
            System.out.println();
        }
    }
}
