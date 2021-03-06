package com.elpixeler.pipesclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import io.reactivex.rxjava3.core.Observable;
import io.socket.client.IO;
import io.socket.client.IO.Options;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Abstract class for client apps and services to connect PipesHub server
 */
public abstract class Client {
    private final static Type type = new TypeToken<HashMap<String, Object>>() {
    }.getType();
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
                        Map<String, Object> data;
                        if (args.length > 0 && args[0].getClass().equals(JSONObject.class)) {

                            data = new Gson().fromJson(args[0].toString(), type);

                            if (!data.get("receiverId").toString().equals(getName()))
                                data.put("res", "I am not who you looking for :)");
                            else {
                                if (Boolean.valueOf(data.get("awaiting").toString()))
                                    if (!data.containsKey("input"))
                                        data.put("input", new HashMap<>());

                                Consumer<Object> func = res -> {
                                    data.put("res", res);
                                    socket.emit("responseGateway", data);
                                };
                                LinkedTreeMap<String, Object> input = (LinkedTreeMap<String, Object>) data.get("input");
                                if (input == null)
                                    input = new LinkedTreeMap<String, Object>();
                                input.put("pushResponse", func);
                                __pipes__.get(data.get("operation")).run(input);
                            }
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
     * Get name of client
     * 
     * @return name of client
     */
    public String getName() {
        return _name;
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
    public Observable<Protocol> ask(String unitId, String operation, Map<String, Object> input) {
        return Observable.create(subscriber -> {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("senderId", this._name);
            data.put("receiverId", unitId);
            data.put("operation", operation);
            data.put("input", input);
            data.put("awaiting", true);
            socket.emit("gateway", data);
            socket.on("responseGateway", args -> {
                Protocol p = new Gson().fromJson(args[0].toString(), Protocol.class);
                subscriber.onNext(p);
            });
        });
    }

    /**
     * Send a request to other unit and delivers the result
     * 
     * @param {*} unitId The receiver unit id
     * @param {*} operation Id or name of operation on other side
     * @param {*} input Input data receiver needs to run operation
     * @param {*} onResponse Called while new data received
     */
    public void persist(String unitId, String operation, Map<String, Object> input, Consumer<Protocol> onResponse) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("senderId", this._name);
        data.put("receiverId", unitId);
        data.put("operation", operation);
        data.put("input", input);
        data.put("awaiting", true);
        socket.emit("gateway", data);
        socket.on("responseGateway", args -> {
            Protocol p = new Gson().fromJson(args[0].toString(), Protocol.class);
            onResponse.accept(p);
        });
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