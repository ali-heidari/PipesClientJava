package com.elpixeler.pipesclient;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

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
                ((Consumer<Object>) data.get("pushResponse"))
                        .accept(Double.valueOf(data.get("a").toString()) + Double.valueOf(data.get("b").toString()));
                return true;
            });
            ArrayList<Object> theList = new ArrayList<>();
            cs.add("add", data -> theList.add(data.get("a")));
            cs.add("list", data -> {
                ((Consumer<Object>) data.get("pushResponse")).accept(theList);
                return true;
            });

            cs.add("message", data -> {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        ((Consumer<Object>) data.get("pushResponse")).accept("Time is " + new Date());
                    }
                }, 0L, 5000L);
                return true;
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

            ca.persist("cServiceJAVA", "message", null, data -> System.out.println(data.res));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
