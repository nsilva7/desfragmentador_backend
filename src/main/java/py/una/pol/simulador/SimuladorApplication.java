package py.una.pol.simulador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.traverse.DepthFirstIterator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import py.una.pol.simulador.model.Core;
import py.una.pol.simulador.model.Link;
import py.una.pol.simulador.socket.SocketClient;

import java.io.*;
import java.util.*;

@SpringBootApplication
public class SimuladorApplication {

	public static void main(String[] args) {

		SpringApplication.run(SimuladorApplication.class, args);


	}

	@Bean
	public SocketClient socketClient() {
		return new SocketClient();
	}

	private static InputStream getFileFromResourceAsStream(String fileName) {

		// The class loader that loaded the class
		ClassLoader classLoader = SimuladorApplication.class.getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(fileName);

		// the stream holding the file content
		if (inputStream == null) {
			throw new IllegalArgumentException("file not found! " + fileName);
		} else {
			return inputStream;
		}

	}

}
