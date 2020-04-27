package com.elpixeler.pipesclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    /**
     * Says hello to the world.
     * 
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        try {
            Client ca = new ClientApp();
            // Client cs = new ClientService();

            // cs.add("sum",
            //         data -> Integer.valueOf(data.get("a").toString()) + Integer.valueOf(data.get("b").toString()));

            Map<String, Object> input = new HashMap<>();
            input.put("a", 6);
            input.put("b", 2);
            ca.ask("cService", "sum", input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
