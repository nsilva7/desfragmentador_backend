package py.una.pol.simulador.utils;
import py.una.pol.simulador.model.Demand;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utils {

    public static List<Demand> generateDemands(int lambda, int totalTime, int fsMin, int fsMax, int cantNodos, int HT){
        int i, cantidadDemandas, j, origen, destino, fs, tVida;
        List<Demand> demands = new ArrayList<>();
        Random rand;
        cantidadDemandas = poisson(lambda);
        for (j = 0; j < cantidadDemandas; j++) {
            rand = new Random();
            origen = rand.nextInt(cantNodos);
            destino = rand.nextInt(cantNodos);
            fs = (int) (Math.random() * (fsMax-fsMin+1)) + fsMin;
            while (origen == destino) {
                destino = rand.nextInt(cantNodos);
            }
            tVida = getLifeTime(HT);
            demands.add(new Demand(origen, destino, fs, tVida));
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

    public static int getLifeTime(int ht) {
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
}
