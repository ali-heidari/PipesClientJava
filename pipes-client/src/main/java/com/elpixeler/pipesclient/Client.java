package com.elpixeler.pipesclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.IO.Options;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Abstract class for client apps and services to connect PipesHub server
 */
public abstract class Client {
    // Fields
    private String _name;
    private Socket socket;

    private Map<String, Handler> __pipes__ = new HashMap<>();

    public Client(String name) throws Exception {
        this._name = name;

        establishConnection();
    }

    /**
     * Establish a socket connection to server after receiving token
     * 
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
        socket = IO.socket("http://localhost:3000", options);

        // Send token through headers
        socket.addHeader("authorization", token);

        // On connect event
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("EVENT_CONNECT");
            }

        })
                // On gateway event
                .on("gateway", new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        System.out.println(args[0].toString());
                        Map<String, Object> data;
                        if (args.length > 0 && args[0].getClass().equals(JSONObject.class)) {
                            data = (HashMap<String, Object>) args[0];
                            if (data.get("receiverId") != Client.this._name)
                                data.put("res", "I am not who you looking for :)");
                            else
                                data.put("res", __pipes__.get(data.get("operation"))
                                        .run((HashMap<String, Object>) data.get("input")));
                            if (Boolean.getBoolean(data.get("awaiting").toString()))
                                socket.emit("responseGateway", data);
                        }
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
     * 
     * @return Return token or response message in case of error
     * @throws IOException Throws exception if connection fail
     */
    private String connect() throws IOException {
        // Create connection to server rest api
        URL url = new URL("http://localhost:16916/auth");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // Send validation key inside body request. Currently a test username, will
        // change
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

    /**
     * Send a request to other unit and delivers the result
     * 
     * @param {*} unitId The receiver unit id
     * @param {*} operation Id or name of operation on other side
     * @param {*} input Input data receiver needs to run operation
     */
    public void ask(String unitId, String operation, Map<String, Object> input) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("senderId", this._name);
        data.put("receiverId", unitId);
        data.put("operation", operation);
        data.put("input", input);
        data.put("awaiting", true);
        socket.emit("gateway", data);
        // socket.on('responseGateway', function (data) {
        // resolve(data);
        // });
    }

    /**
     * Send a request to other unit and no result expected
     * 
     * @param {*} unitId The receiver unit id
     * @param {*} operation Id or name of operation on other side
     * @param {*} input Input data receiver needs to run operation
     */
    public void request(String unitId, String operation, Map<String, Object> input) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("senderId", this._name);
        data.put("receiverId", unitId);
        data.put("operation", operation);
        data.put("input", input);
        data.put("awaiting", false);
        socket.emit("gateway", data);
    }

    /**
     * Add a function to global __pipes prototype
     * 
     * @param {*} funcName A name for operation
     * @param {*} handler Operation body
     * @throws Exception
     */
    public void add(String funcName, Handler handler) throws Exception {
        if (__pipes__.containsKey(funcName)) {
            throw new Exception("This function already exists.");
        }
        __pipes__.put(funcName, handler);
    }
}