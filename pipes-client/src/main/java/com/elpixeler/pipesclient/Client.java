package com.elpixeler.pipesclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.socket.client.IO;
import io.socket.client.IO.Options;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Abstract class for client apps and services to connect PipesHub server
 */
public abstract class Client {
    private String _name;

    public Client(String name) throws Exception {
        this._name = name;

        establishConnection();
    }

    /**
     * Establish a socket connection to server after receiving token
     * @throws Exception Throws an exception if no token received.
     */
    private void establishConnection() throws Exception {
        String token = connect();
        if (token.isEmpty())
            throw new Exception("No token received.");

        // Set app name as query    
        Options options = new Options();
        options.query = "name=" + this._name;

        // Create socket to the server address
        Socket socket = IO.socket("http://localhost:3000", options);

        // Send token through headers
        socket.addHeader("authorization", token);

        // On connect event
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("EVENT_CONNECT");
                socket.emit("foo", "hi");
                socket.disconnect();
            }

        })
        // On gateway event
        .on("gateway", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("event");
            }

        })
        // On disconnect event
        .on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("EVENT_DISCONNECT");
            }

        });
        socket.connect();
    }

    /**
     * Get token from server
     * @return Return token or response message in case of error
     * @throws IOException Throws exception if connection fail
     */
    private String connect() throws IOException {
        // Create connection to server rest api
        URL url = new URL("http://localhost:16916/auth");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // Send validation key inside body request. Currently a test username, will change
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write("name=guest".getBytes("utf8"));
        }
        // Successful response, get response body which is token
        if (connection.getResponseCode() == 200) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader stream = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                if (stream.ready())
                    sb.append(stream.readLine());
            }
            return sb.toString();
        }
        // Return response message which is server message why token did not send
        return connection.getResponseMessage();
    }
}