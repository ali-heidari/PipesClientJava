package com.elpixeler.pipesclient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.socket.client.Socket;
import io.socket.client.IO.Options;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;

public abstract class Client {
    private String _name;

    public Client(String name) throws Exception {
        this._name = name;

        establishConnection();
    }

    private void establishConnection() throws Exception {
        String token = connect();
        if (token.isEmpty())
            throw new Exception("No token received.");

        Options options = new Options();
        options.query = "name=" + this._name;

        Socket socket = IO.socket("http://localhost:3000", options);

        // Called upon transport creation.
        socket.addHeader("authorization", token);

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("EVENT_CONNECT");
                socket.emit("foo", "hi");
                socket.disconnect();
            }

        }).on("event", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("event");
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("EVENT_DISCONNECT");
            }

        });
        socket.connect();
    }

    private String connect() throws IOException {
        URL url = new URL("http://localhost:16916/auth");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write("name=guest".getBytes("utf8"));
        }
        if (connection.getResponseCode() == 200) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader stream = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                if (stream.ready())
                    sb.append(stream.readLine());
            }
            return sb.toString();
        }
        return connection.getResponseMessage();
    }
}