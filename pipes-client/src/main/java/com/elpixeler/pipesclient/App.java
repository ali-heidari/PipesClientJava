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
            Client cs = new ClientService();

            cs.add("sum", data -> {
                // data.get("pushResponse")
                //         .run(Integer.valueOf(data.get("a").toString()) + Integer.valueOf(data.get("b").toString()));
                        return 1;
            });

            Map<String, Object> input = new HashMap<>();
            input.put("a", 6);
            input.put("b", 2);
            ca.ask("cServiceJAVA", "sum", input).subscribe(x -> System.out.println("Result of sum is " + x.res));

            input = new HashMap<>();
            input.put("a", 555);
            ca.request("cServiceJAVA", "add", input);

            input = new HashMap<>();
            input.put("a", "test");
            ca.request("cServiceJAVA", "add", input);

            ca.ask("cServiceJAVA", "list", null).subscribe(x -> System.out.println("Result is " + x.res));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
