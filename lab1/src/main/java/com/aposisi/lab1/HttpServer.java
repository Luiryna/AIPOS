package com.aposisi.lab1;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Optional;
import java.util.StringTokenizer;



public class HttpServer implements Runnable {
    private static final Logger logger = LogManager.getLogger(HttpServer.class);

    private static final String ROOT = "/sharedFiles";
    private static final String DEFAULT_FILE = "index.html";
    private static final String FILE_NOT_FOUND = "404.html";
    private static final String METHOD_NOT_SUPPORTED = "501.html";

    private Socket connect;

    private BufferedReader in;
    private PrintWriter out;
    private BufferedOutputStream dataOut;

    HttpServer(Socket connect) {
        this.connect = connect;
        try {
            connect.setKeepAlive(true);
            connect.setSoTimeout(3500);
        } catch (SocketException e) {
            System.exit(0);
        }

    }

    @Override
    public void run() {
        String fileRequested = null;
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();
            logger.log(Level.INFO, "Input:\n" + input);
            StringTokenizer parse;
            try {
                parse = new StringTokenizer(input);
            } catch (NullPointerException e) {
                return;
            }

            String method = parse.nextToken().toUpperCase();
            logger.log(Level.INFO, "Request type: " + method);
            fileRequested = parse.nextToken().toLowerCase();
            Optional<Request> requestType = Request.of(method);

            if (!requestType.isPresent()) {
                methodNotSupported(method);
            } else {
                switch (requestType.get()) {
                    case GET:
                        processGet(fileRequested);
                        break;
                    case POST:
                        processPost();
                    case OPTIONS:
                        processOptions();
                }
                logger.log(Level.INFO, "File " + fileRequested + " returned");
            }
        } catch (FileNotFoundException fnfe) {
            try {
                logger.log(Level.WARN, "File:" + fileRequested + " not found, load " + FILE_NOT_FOUND);
                fileNotFound(fileRequested);
            } catch (IOException ioe) {
                logger.log(Level.WARN, "File error" + ioe.getMessage());
            }
        } catch (IOException ioe) {
            logger.log(Level.ERROR, "Server error : " + ioe);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close();
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error closing stream : " + e.getMessage());
            }
            logger.log(Level.INFO, "Connection closed");
        }
    }

    private void processGet(String fileRequested) throws IOException {
        logger.log(Level.INFO, "GET request accepted");
        if (fileRequested.endsWith("/")) {
            fileRequested += DEFAULT_FILE;
        }
        InputStream inputStream = findFile(fileRequested, true);
        Content content = Content.findByFileName(fileRequested);
        byte[] data = content.getReader().read(inputStream);
        createResponse(HttpCodes.OK, content, data.length, data);
        logger.log(Level.INFO, "File " + fileRequested + " of type " + content.getText() + " returned");
    }

    private void processPost() throws IOException {
        logger.log(Level.INFO, "POST request accepted");

        createResponse(HttpCodes.CREATED, Content.PLAIN, 0, new byte[]{});
    }

    private void processOptions() throws IOException {
        logger.log(Level.INFO, "OPTIONS request accepted");
        InputStream inputStream = findFile("options.txt", false);
        byte[] data = Content.PLAIN.getReader().read(inputStream);
        createResponse(HttpCodes.OK, Content.PLAIN, data.length, data);
    }

    private void methodNotSupported(String method) throws IOException {
        logger.log(Level.WARN, "Unknown method: " + method);
        InputStream inputStream = findFile(METHOD_NOT_SUPPORTED, false);
        byte[] data = Content.HTML.getReader().read(inputStream);
        createResponse(HttpCodes.NOT_IMPLEMENTED, Content.HTML, data.length, data);
    }

    private void fileNotFound(String fileRequested) throws IOException {
        InputStream inputStream = findFile(FILE_NOT_FOUND, false);
        byte[] data = Content.HTML.getReader().read(inputStream);
        createResponse(HttpCodes.NOT_FOUND, Content.HTML, data.length, data);
        logger.log(Level.WARN, "File " + fileRequested + " not found");

    }

    private InputStream findFile(String fileName, boolean clientFile) throws FileNotFoundException {
        if (clientFile) {
            fileName = ROOT + fileName;
        } else {
            fileName = "/" + fileName;
        }
        InputStream inputStream = this.getClass().getResourceAsStream(fileName);
        logger.log(Level.INFO, "requested path of the file: " + this.getClass().getResource(fileName));
        if (inputStream == null) {
            throw new FileNotFoundException();
        }
        return inputStream;
    }

    private void createResponse(HttpCodes code, Content content, int fileLength, byte[] fileData) throws IOException {
        out.println("HTTP/1.1 " + code.getCode() + " " + code.getDescription());
        out.println("Server: Java HTTP Server");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content.getText());
        out.println("Content-length: " + fileLength);
        out.println("Access-Control-Allow-Origin: " + "localhost");
        out.println("Access-Control-Allow-Methods: " + "GET, POST, OPTIONS");
        out.println();
        out.flush();
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        logger.log(Level.INFO, "type " + content.getExtension() + " size " + fileLength);
        try {
            Thread.sleep(fileLength / 100);
        } catch (InterruptedException e) {

        }
        logger.log(Level.INFO, "Creating header of response with code " + code.getCode());
    }

}