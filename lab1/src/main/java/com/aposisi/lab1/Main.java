package com.aposisi.lab1;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final String HELP = "\n-h --help\tto print info\n -p --port\tto set custom port\n";
    private static final int DEFAULT_PORT = 8080;

    public static void main(String... args) {
        int port = 8080;
        List<String> arguments = Arrays.stream(args)
                .filter(x -> x.startsWith("-") | x.startsWith("--"))
                .collect(Collectors.toList());
        if (!arguments.isEmpty()){
            Optional<String> helpArgumentOptional = arguments.stream()
                    .filter(x -> x.startsWith("-h") || x.startsWith("--help"))
                    .findFirst();
            if (helpArgumentOptional.isPresent()){
                logger.log(Level.INFO, HELP);
                return;
            }
            Optional<String> portArgumentOptional = arguments.stream()
                    .filter(x -> x.startsWith("-p") || x.startsWith("--port"))
                    .findFirst();
            if (portArgumentOptional.isPresent()){
                String portArgument = portArgumentOptional.get();
                try {
                    String portString = portArgument.substring(portArgument.indexOf("=")+1);
                    port = Integer.parseInt(portString);
                    logger.log(Level.INFO, "New port: " + port);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e){
                    logger.log(Level.WARN, "Invalid port! Using default (8080) port.");
                }
            }
        }

        try {
            ServerSocket serverConnect = new ServerSocket(port);
            logger.log(Level.INFO, "Server started. Waiting for incoming connections on port: " + port);

            while (true) {
                HttpServer newServer = new HttpServer(serverConnect.accept());

                System.out.println("Connection opened. (" + new Date() + ")");

                Thread thread = new Thread(newServer);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server connection error : " + e.getMessage());
        }
    }


}
