package py.una.pol.simulador.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.geom.Edge;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import py.una.pol.simulador.model.Core;
import py.una.pol.simulador.model.Link;
import py.una.pol.simulador.model.Options;
import py.una.pol.simulador.model.Demand;
import py.una.pol.simulador.utils.ResourceReader;
import py.una.pol.simulador.utils.Utils;
import py.una.pol.simulador.algorithms.Algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;

@RestController
public class SimuladorController {

    @CrossOrigin(origins = "http://localhost:4300")
    @PostMapping("/simular")
    public String simular(@RequestBody Options options) {
        System.out.println("Opciones: " + options);
        List<Demand> demands;
        Graph net = createTopology2("nsfnet.json", 4, options.getFsWidth(), options.getCapacity());
        for (int i = 0; i < options.getTime(); i++) {
            demands = Utils.generateDemands(
                    options.getLambda(), options.getTime(),
                    options.getFsRangeMin(), options.getFsRangeMax(),
                    net.vertexSet().size(), options.getErlang() / options.getLambda());

            KShortestSimplePaths ksp = new KShortestSimplePaths(net);
            for(Demand demand : demands){
                System.out.println("DEMANDA: " + demand);
                //k caminos más cortos entre source y destination de la demanda actual
                List<GraphPath> kspaths = ksp.getPaths(demand.getSource(), demand.getDestination(), 5);
                //comprobarKspVocConfia(kspaths);
                try {
                    Class<?>[] paramTypes = {Graph.class, List.class, Demand.class, int.class, int.class};
                    Method method = Algorithms.class.getMethod(options.getRoutingAlg(), paramTypes);
                    method.invoke(this, net, kspaths, demand, options.getCapacity(), 0);
                }catch (java.lang.Exception e){
                    System.out.println("ERROR");
                    System.out.println(e);
                    System.out.println(e.getMessage());
                }

            }
        }

        return "SUCCESS";
    }


    @GetMapping("/getTopology")
    public String getTopología() {
        Graph g = createTopology("Networks.json");
        DOTExporter<Integer, Link> exporter =
                new DOTExporter<>(v -> v.toString().replace('.', '_'));
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        Writer writer = new StringWriter();
        exporter.exportGraph(g, writer);
        return writer.toString();
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


                    Link link = new Link(distance,cores);
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

                    for (int j = 0; j < numberOfCores; j++){
                        Core core = new Core(fsWidh,numberOffs);
                        cores.add(core);
                    }

                    Link link = new Link(distance,cores);
                    g.addEdge(vertex,connection,link);
                }
                vertex++;
            };
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
}
