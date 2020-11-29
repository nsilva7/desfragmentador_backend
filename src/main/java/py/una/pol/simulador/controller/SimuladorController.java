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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import py.una.pol.simulador.model.*;
import py.una.pol.simulador.utils.ResourceReader;
import py.una.pol.simulador.utils.Utils;
import py.una.pol.simulador.algorithms.Algorithms;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.rmi.CORBA.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.*;
import java.lang.reflect.Type;

@RestController
public class SimuladorController {
    private final SimpMessagingTemplate template;

    @Autowired
    SimuladorController(SimpMessagingTemplate template){
        this.template = template;
    }


//    @CrossOrigin(origins = "http://localhost:4300")
//    @PostMapping("/simular")
    @MessageMapping("/simular")
    public void simular(@RequestBody Options options) {
//        pruebas();
//        if(1 == 1)
//            return "x";
        System.out.println("Opciones: " + options);
        List<Demand> demands;
        Object result;
        Graph net = createTopology2("nsfnet.json", options.getCores(), options.getFsWidth(), options.getCapacity());
        for (int i = 0; i < options.getTime(); i++) {
            demands = Utils.generateDemands(
                    options.getLambda(), options.getTime(),
                    options.getFsRangeMin(), options.getFsRangeMax(),
                    net.vertexSet().size(), options.getErlang() / options.getLambda());

            KShortestSimplePaths ksp = new KShortestSimplePaths(net);
            System.out.println("Cantidad de de demandas: " + demands.size());
            for(Demand demand : demands){
                ///System.out.println("DEMANDA: " + demand);
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
//                        System.out.println("----RUTA ESTABLECIDA----");
//                        System.out.println(establisedRoute);
                        if(establisedRoute == null){
                            tested[core] = true;//Se marca el core probado
                            System.out.println("BLOQUEO");
                            this.template.convertAndSend("/message",  establisedRoute);
                            if(!Arrays.asList(tested).contains(false)){//Se ve si ya se probaron todos los cores
                                //Bloqueo
                                break;
                            }
                        }else{
                            //Ruta establecida
                            Utils.assignFs((EstablisedRoute)establisedRoute, core);
                            this.template.convertAndSend("/message",  establisedRoute);
                            break;
                        }
                    }
                }catch (java.lang.Exception e){
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000/demands.size());
                }catch (java.lang.Exception e){

                }
            }
        }
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("end", true);
        this.template.convertAndSend("/message",  map);
    }


    @GetMapping("/getTopology")
    public String getTopología() {
        pruebas();
        return "x";
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

    private void pruebas(){
        Graph net = createTopologyWithFs("Networks.json");
        estadoDeFs(net);
        Demand demand = new Demand(0,5,2,5);
        KShortestSimplePaths ksp = new KShortestSimplePaths(net);

        List<GraphPath> kspaths = ksp.getPaths(demand.getSource(), demand.getDestination(), 4);
        Algorithms.mtlsc(net, kspaths, demand, 12, 0);
//                try {
//                    boolean [] tested = new boolean[4];
//                    Arrays.fill(tested, false);
//                    int core;
//                    while (true){
//                        //core = getCore(3, tested);
//                        core = 0;
//                        System.out.println("CORE: " + core);
//                        Class<?>[] paramTypes = {Graph.class, List.class, Demand.class, int.class, int.class};
//                        Method method = Algorithms.class.getMethod(options.getRoutingAlg(), paramTypes);
//                        Object establisedRoute = method.invoke(this, net, kspaths, demand, options.getCapacity(), core);
//                        System.out.println("----RUTA ESTABLECIDA----");
//                        System.out.println((EstablisedRoute)establisedRoute);
//                        if(establisedRoute == null){
//                            tested[core] = true;//Se marca el core probado
//                            System.out.println("BLOQUEO");
//                            if(!Arrays.asList(tested).contains(false)){//Se ve si ya se probaron todos los cores
//                                //Bloqueo
//                                break;
//                            }
//                            break;
//                        }else{
//                            //Ruta establecida
//                            Utils.assignFs((EstablisedRoute)establisedRoute, core);
//                            break;
//                        }
//                    }
//                }catch (java.lang.Exception e){
//                    e.printStackTrace();
//                }



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
