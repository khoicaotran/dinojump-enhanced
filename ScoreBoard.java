import java.io.IOException;
import java.nio.file.*;
import java.util.*;

// ScoreBoard.java - keeps the best 5 scores and saves them in the file scores.txt,
// so the scores are still there the next time you start the game.
public class ScoreBoard {
    static final int MAX = 5;                              // we keep the best 5
    static final Path FILE = Path.of("scores.txt");        // each line: score TAB name TAB level

    static class Entry {
        String name, level;                                // level = Easy, Normal or Hard
        int score;
        boolean latest;                                    // true = from the game that just ended
        Entry(String name, int score, String level) { this.name = name; this.score = score; this.level = level; }
    }

    List<Entry> entries = new ArrayList<>();

    ScoreBoard() { load(); }

    void load() {
        try {
            for (String line : Files.readAllLines(FILE)) {
                String[] parts = line.split("\t", 3);
                if (parts.length >= 2) {
                    String name = parts[1].replaceAll("[^\\p{L}\\p{N} _-]", "");
                    String level = parts.length == 3 ? parts[2].replaceAll("[^A-Za-z]", "") : "Normal";
                    entries.add(new Entry(name, Integer.parseInt(parts[0].trim()), level));
                }
            }
            sortAndCut();
        } catch (IOException | NumberFormatException e) {
            // there is no file yet: we simply start with an empty table
        }
    }

    void clearLatest() {
        for (Entry e : entries) e.latest = false;
    }

    void add(String name, int score, String level) {
        Entry entry = new Entry(name, score, level);
        entry.latest = true;
        entries.add(entry);
        sortAndCut();
        save();
    }

    void sortAndCut() {
        entries.sort((a, b) -> b.score - a.score);         // highest score first
        while (entries.size() > MAX) entries.remove(entries.size() - 1);
    }

    void save() {
        List<String> lines = new ArrayList<>();
        for (Entry e : entries) lines.add(e.score + "\t" + e.name + "\t" + e.level);
        try {
            Files.write(FILE, lines);
        } catch (IOException e) {
            System.out.println("Could not save the scores: " + e.getMessage());
        }
    }

    String toJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(e.name).append("\",\"score\":").append(e.score)
              .append(",\"level\":\"").append(e.level).append("\",\"latest\":").append(e.latest).append("}");
        }
        return sb.append("]").toString();
    }
}
