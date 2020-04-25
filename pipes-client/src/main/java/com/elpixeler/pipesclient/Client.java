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
import java.util.stream.Stream;

public abstract class Client {
    public Client() throws Exception {
        establishConnection();
    }

    private void establishConnection() throws Exception {
        String token = connect();
        if (token.isEmpty())
            throw new Exception("No token received.");

        System.out.println(token);
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