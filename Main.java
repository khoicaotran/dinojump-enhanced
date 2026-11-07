import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

// Main.java - starts a small web server and the game loop.
//
// HOW TO RUN (needs Java 11 or newer):
//   1. javac *.java
//   2. java Main
//   3. Open http://localhost:8080 in your browser
//
// How the parts talk to each other:
//   Browser (index.html)  --"jump!", "duck!", "start"-->  Java (Game)
//   Browser (index.html)  <--"here is the game state"--   Java (Game)
public class Main {
    static Game game = new Game();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // The web page itself
        server.createContext("/", ex -> send(ex, "text/html", Files.readString(Path.of("index.html"))));

        // The browser asks for the current game state many times per second
        server.createContext("/state", ex -> send(ex, "application/json", game.toJson()));

        // Commands from the browser
        server.createContext("/start", ex -> {
            Map<String, String> q = query(ex);
            int[] skins = { Integer.parseInt(q.get("s0")), Integer.parseInt(q.get("s1")) };
            String[] names = { q.getOrDefault("n0", ""), q.getOrDefault("n1", "") };
            game.start(Integer.parseInt(q.get("players")), skins, names, q.getOrDefault("d", "normal"));
            send(ex, "text/plain", "ok");
        });
        server.createContext("/jump", ex -> {
            game.jump(Integer.parseInt(query(ex).get("p")));
            send(ex, "text/plain", "ok");
        });
        server.createContext("/duck", ex -> {
            Map<String, String> q = query(ex);
            game.duck(Integer.parseInt(q.get("p")), q.get("on").equals("1"));
            send(ex, "text/plain", "ok");
        });
        server.createContext("/move", ex -> {
            Map<String, String> q = query(ex);
            game.move(Integer.parseInt(q.get("p")), Integer.parseInt(q.get("d")));
            send(ex, "text/plain", "ok");
        });
        server.createContext("/pause", ex -> {
            game.pause(query(ex).get("on").equals("1"));
            send(ex, "text/plain", "ok");
        });
        server.start();
        System.out.println("Game is running! Open http://localhost:8080");

        // The game loop: update the game about 60 times per second
        ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor();
        loop.scheduleAtFixedRate(() -> game.update(1 / 60.0), 0, 16, TimeUnit.MILLISECONDS);
    }

    // Send a text answer back to the browser
    static void send(HttpExchange ex, String type, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        ex.getResponseHeaders().add("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().add("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream responseBody = ex.getResponseBody()) {
            responseBody.write(bytes);
        }
    }

    // Read "?a=1&b=2" from the address into a Map
    static Map<String, String> query(HttpExchange ex) {
        Map<String, String> map = new HashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q != null) {
            for (String pair : q.split("&")) {
                String[] kv = pair.split("=");
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}
