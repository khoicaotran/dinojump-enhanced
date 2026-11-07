import java.io.IOException;
import java.nio.file.*;
import java.util.*;

// Achievements.java - badges that the player unlocks by doing special things.
// The progress is saved in the file achievements.txt (one line per badge: id TAB progress).
public final class Achievements {
    static final Path FILE = Path.of("achievements.txt");

    static class Item {
        String id, title, text;     // text = what the player must do
        int goal, progress;         // the badge is unlocked when progress reaches goal
        Item(String id, String title, String text, int goal) {
            this.id = id; this.title = title; this.text = text; this.goal = goal;
        }
        boolean unlocked() { return progress >= goal; }
    }

    List<Item> items = new ArrayList<>();

    Achievements() {
        items.add(new Item("dragon", "Dragon Slayer", "Survive the boss fight", 1));
        items.add(new Item("meteor", "Meteor Dodger", "Dodge 10 meteors", 10));
        items.add(new Item("storm",  "Storm Chaser",  "Dodge 10 lightning bolts", 10));
        items.add(new Item("croc",   "Croc Jumper",   "Jump over 5 crocodile ponds", 5));
        load();
    }

    // Add 1 to the progress of one badge.
    // Returns the title if the badge was unlocked just now, otherwise null.
    String add(String id) {
        for (Item item : items) {
            if (item.id.equals(id) && !item.unlocked()) {
                item.progress++;
                save();
                return item.unlocked() ? item.title : null;
            }
        }
        return null;
    }

    void load() {
        try {
            for (String line : Files.readAllLines(FILE)) {
                String[] parts = line.split("\t");
                if (parts.length != 2) continue;
                for (Item item : items) {
                    if (item.id.equals(parts[0])) item.progress = Math.min(item.goal, Integer.parseInt(parts[1].trim()));
                }
            }
        } catch (IOException | NumberFormatException e) {
            // no file yet: every badge starts at 0
        }
    }

    void save() {
        List<String> lines = new ArrayList<>();
        for (Item item : items) lines.add(item.id + "\t" + item.progress);
        try {
            Files.write(FILE, lines);
        } catch (IOException e) {
            System.out.println("Could not save the achievements: " + e.getMessage());
        }
    }

    String toJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"title\":\"").append(item.title).append("\",\"text\":\"").append(item.text)
              .append("\",\"progress\":").append(item.progress).append(",\"goal\":").append(item.goal).append("}");
        }
        return sb.append("]").toString();
    }
}
