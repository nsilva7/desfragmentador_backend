package py.una.pol.simulador.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import py.una.pol.simulador.model.*;
import py.una.pol.simulador.utils.ResourceReader;
import py.una.pol.simulador.utils.Utils;
import py.una.pol.simulador.algorithms.Algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.io.FileWriter;
import java.io.BufferedWriter;

import static java.lang.Double.NaN;

@RestController
public class SimuladorController {
    private final SimpMessagingTemplate template;
    private static Object LOCK = new Object();
    @Autowired
    SimuladorController(SimpMessagingTemplate template){
        this.template = template;
    }


//    @CrossOrigin(origins = "http://localhost:4300")
//    @PostMapping("/simular")
    @MessageMapping("/simular")
    public void simular(@RequestBody Options options) throws IOException {
//        boolean [] testedx = new boolean[4];
//        Arrays.fill(testedx, false);
//        for (int k = 1; k < 60; k++){
//            this.getCore(4, testedx);
//        }
//        pruebas();
//        if(1 == 1)
//            return ;

        System.out.println("Opciones: " + options);
        List<Demand> demands;
        List<EstablisedRoute> establishedRoutes = new ArrayList<EstablisedRoute>();
        int wait;
        Graph net = null;
        List<List<GraphPath>> kspList = new ArrayList<>();

        FileWriter file = new FileWriter("datos.csv");
        BufferedWriter writer = new BufferedWriter(file);
        ArrayList<Integer> slotsC = new ArrayList<>();
        ArrayList<Integer> blockedSlots = new ArrayList<>();
        int sumSlots = 0, sumBlockedSlots = 0;

        writer.write("entropy, pc, bfr, shf, msi, used, demandsq, blocked, bbp");
        writer.newLine();

        String[] topologies = {"eunet.json", "nsfnet.json", "usnet.json"};

        int Tc = 20;
        int Tmin = 8;
        double Bth = 0.015;
        double Bdt = 0;

        int cerocount = 0, cerocerocount = 0, unocount = 0, doscount = 0, trescount = 0, cuatrocount = 0, cincocount = 0;
        int seiscount = 0, sietecount = 0, ochocount = 0, nuevecount = 0, unounocount = 0;
        int demandsBlocked = 0;
        int slotsCount = 0;
        int blockedSlotsCount = 0;
        int cantidadDemandas = 0;

        for(int top = 0; top < topologies.length; top++){
            System.out.println("---TOPOLOGÍA: " +  topologies[top] + "---");
            for(int cc = 0; cc < 20; cc++){
                System.out.println("--CC: " + cc);
                for(int er = 400; er <= 1500; er = er + 100 ){
                    System.out.println("Erlangs: " +  er);
                    net = createTopology2(topologies[top], options.getCores(), options.getFsWidth(), options.getCapacity());
                    options.setErlang(er);
                    int k = 0, j = 1, dt = 0, wtk = Tmin, tnext = 1 + wtk, wtka = 0, U1  = 0, U0 = 0, Tk = 0, Tka = 0;
                    double U = 0, Ua = 0, Uaa = 0, Bk = 0, Rbk = 0, Bka = 0, Abk = 0, Rbka = 0;
                    for (int t = 1; t <= options.getTime(); t++) {
                        slotsCount = 0;
                        blockedSlotsCount = 0;
                        demandsBlocked = 0;
                        dt++;
                        if(t%100 == 0)
                            System.out.println("Tiempo: " + t);
                        demands = Utils.generateDemands(
                                options.getLambda(), options.getTime(),
                                options.getFsRangeMin(), options.getFsRangeMax(),
                                net.vertexSet().size(), options.getErlang() / options.getLambda());

                        cantidadDemandas = demands.size();
                        KShortestSimplePaths ksp = new KShortestSimplePaths(net);
                        for(Demand demand : demands){
                            List<GraphPath> kspaths = ksp.getPaths(demand.getSource(), demand.getDestination(), 5);
                            kspList.add(kspaths);
                            try {
                                boolean [] tested = new boolean[4];
                                Arrays.fill(tested, false);
                                int core;
                                while (true){
                                    core = getCore(options.getCores(), tested);
                                    t = t;
                                    Class<?>[] paramTypes = {Graph.class, List.class, Demand.class, int.class, int.class};
                                    Method method = Algorithms.class.getMethod(options.getRoutingAlg(), paramTypes);
                                    Object establisedRoute = method.invoke(this, net, kspaths, demand, options.getCapacity(), core);
                                    if(establisedRoute == null){
                                        tested[core] = true;//Se marca el core probado
                                        if(!Arrays.asList(tested).contains(false)){//Se ve si ya se probaron todos los cores
                                            demand.setBlocked(true);
                                        }
                                    }else{
                                        //Ruta establecida
                                        establishedRoutes.add((EstablisedRoute) establisedRoute);
                                        Utils.assignFs((EstablisedRoute)establisedRoute, core);
                                    }
                                    slotsCount += demand.getFs();
                                    if(demand.isBlocked()){
                                        demandsBlocked++;
                                        blockedSlotsCount += demand.getFs();
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

                        ReleasedSlots rSlots = new ReleasedSlots();
                        rSlots.setTime(t + 2);
                        rSlots.setReleased(true);
                        rSlots.setReleasedSlots(this.setTimeLife(net));
                        //this.template.convertAndSend("/message", rSlots);

                        //Calculo BBP//
                        slotsC.add(slotsCount);//Guardamos la cantidad de slots de las demandas
                        blockedSlots.add(blockedSlotsCount);//Guardamos la cantidad de slots que se bloquearon

                        if(t == j*Tc){//Si t = al tiempo minimo entre df
                            U = Algorithms.graphUsePercentage(net);//%uso de red
                            if(U > Ua && Ua < Uaa){//Se resetean valores
                                dt = 0;
                                k = 0;
                                j = 1;
                                wtk = Tmin;
                                Ua = 0;
                                Uaa = 0;
                                tnext = t + wtk;
                            }else{
                                Uaa = Ua;
                                Ua = U;
                                j++;
                            }
                        }
                        if(t == tnext){
                            //Calcular BBP(Dt)
                            if(dt == 0)
                                dt = 1;
                            if(dt <= blockedSlots.size()){//Solo si el dt a las demandas que tenemos guardadas
                                sumSlots = 0;
                                sumBlockedSlots = 0;
                                for (int tt = 0; tt < dt; tt++){
                                    sumSlots += slotsC.get(slotsC.size() - 1 - tt);
                                    sumBlockedSlots += blockedSlots.get(blockedSlots.size() - 1 - tt);
                                }
                                Bdt = (double)sumBlockedSlots / (double)sumSlots;

                                Boolean write = true;

                                if(Bdt == 0) {
                                    cerocount++;
                                    if(cerocount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0 && Bdt < 0.1){
                                    cerocerocount++;
                                    if(cerocerocount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.1 && Bdt < 0.2){
                                    unocount++;
                                    if(unocount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.2 && Bdt < 0.3){
                                    doscount++;
                                    if(doscount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.3 && Bdt < 0.4){
                                    trescount++;
                                    if(trescount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.5 && Bdt < 0.6){
                                    cuatrocount++;
                                    if(cuatrocount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.6 && Bdt < 0.7){
                                    seiscount++;
                                    if(seiscount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.6 && Bdt < 0.7){
                                    sietecount++;
                                    if(sietecount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.7 && Bdt < 0.8){
                                    ochocount++;
                                    if(ochocount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.8 && Bdt < 0.9){
                                    ochocount++;
                                    if(cincocount >= 10000){
                                        write = false;
                                    }
                                }
                                if(Bdt > 0.9 && Bdt < 1){
                                    nuevecount++;
                                    if(nuevecount >= 10000){
                                        write = false;
                                    }
                                }
                                if( Bdt == 1){
                                    unounocount++;
                                    if(unounocount >= 10000){
                                        write = false;
                                    }
                                }

                                if(write){
                                    int FSMinPC = (int) (options.getFsRangeMax() - ((options.getFsRangeMax() - options.getFsRangeMax()) * 0.3 ));
                                    writer.write(
                                            String.format(Locale.US, ("%.6f"), Utils.graphEntropyCalculation(net)) + ", " +
                                                    String.format(Locale.US, ("%.6f"), Algorithms.PathConsecutiveness(Utils.twoLinksRoutes(net), options.getCapacity(), FSMinPC)) + " , " +
                                                    String.format(Locale.US, ("%.6f"), Algorithms.BFR(net, options.getCapacity())) + " , " +
                                                    String.format(Locale.US, ("%.6f"), Algorithms.shf(net, options.getCapacity())) + " , " +
                                                    String.format(Locale.US, ("%.6f"), Algorithms.MSI(net)) + " , " +
                                                    String.format(Locale.US, ("%.6f"), Algorithms.graphUsePercentage(net)) + " , " +
                                                    cantidadDemandas + " , " +
                                                    demandsBlocked + " , " +
                                                    String.format(Locale.US, ("%.2f"), Bdt)
                                    );
                                    writer.newLine();
                                }
                                if(Bdt >= Bth){
                                    //"Desfragmentar"
                                    Tka = Tk;
                                    Tk = dt;
                                    Bk = Bdt;
                                    Rbka = Rbk;
                                    Rbk = (2*(Bk * Bka)) / (Tk + Tka);
                                    Abk = Rbk - Rbka;
                                    k++;
                                    if(Abk > 0) {
                                        wtk += 2;
                                    }else{
                                        wtk -= 2;
                                    }
                                    tnext = t + wtk;
                                    dt = 0;
                                }else{
                                    tnext++;
                                }
                            }
                        }

                    }
                }
            }
        }

        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("end", true);
        //this.template.convertAndSend("/message",  map);
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
