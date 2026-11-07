import java.util.*;

// Game.java - the "brain" of the game. All the rules live here.
// The HTML page only DRAWS what this class tells it.
public class Game {
    static final int WIDTH = 800;            // screen width
    static final int GROUND = 160;           // y position of the ground line
    static final double BOSS_TIME = 14;      // seconds the boss fight lasts
    static final double DISASTER_TIME = 12;  // seconds a disaster lasts
    // Speed of a fireball from the dragon behind = BASE + game speed * GROWTH.
    // So it gets faster together with the game (like the fireballs from the front).
    static final double BACK_FIREBALL_BASE = 250;
    static final double BACK_FIREBALL_GROWTH = 0.5;
    static final double BACK_WARNING_TIME = 0;      // seconds of warning arrow before a fireball from behind (0 = no warning, a surprise!)
    static final String[] THEME_NAMES = {"Desert Day", "Sunset", "Night", "Snow Land", "Volcano"};

    boolean paused = false;                  // is the game paused?
    ScoreBoard scoreBoard = new ScoreBoard(); // the best 5 scores (saved in scores.txt)
    Achievements achievements = new Achievements(); // badges (saved in achievements.txt)
    String toast = "";                       // pop-up text, e.g. "Achievement unlocked: ..."
    double toastTime;

    // Difficulty (Easy, Normal or Hard)
    String difficulty = "Normal";
    int startHearts = 3;
    double speedFactor = 1, gapFactor = 1;   // gapFactor: bigger = fewer obstacles
    String state = "menu";                   // "menu", "play" or "over"
    List<Player> players = new ArrayList<>();
    List<Obstacle> obstacles = new ArrayList<>();
    Random random = new Random();

    double distance, speed, spawnTimer, lastWidth;
    double shake;                            // how strongly the screen shakes
    int lastStage;

    // Disaster: "none", "flood" or "meteor"
    String disaster = "none";
    double disasterTime, meteorTimer, lightningTimer;
    int nextDisasterScore;

    // Boss (the Fire Drake)
    boolean bossActive, bossLeaving;
    int bossMode;                            // 0 = attacks from the front, 1 = from behind
    double bossX, bossBackX, bossTime, fireTimer, backTimer;
    int bossAttacks, bossPairs;              // attacks and double fireballs in this boss fight
    int nextBossScore;

    String message = "";                     // text shown in the middle of the screen
    double messageTime;

    // ---------- commands from the HTML page ----------

    synchronized void start(int count, int[] skins, String[] names, String level) {
        setDifficulty(level);
        players.clear();
        obstacles.clear();
        for (int i = 0; i < count; i++) {
            Player p = new Player(120 + i * 60, skins[i]);
            p.name = cleanName(names[i], i);
            p.hearts = startHearts;
            p.maxHearts = startHearts;
            players.add(p);
        }
        paused = false;
        distance = 0; speed = 300 * speedFactor; toast = ""; toastTime = 0; spawnTimer = 500; lastWidth = 20; shake = 0;
        lastStage = 0; messageTime = 0;
        bossActive = false; nextBossScore = 700;
        disaster = "none"; nextDisasterScore = 400;
        state = "play";
    }

    synchronized void jump(int p) {
        if (state.equals("play") && !paused && p < players.size()) players.get(p).jump();
    }

    synchronized void duck(int p, boolean on) {
        if (p < players.size()) players.get(p).ducking = on;
    }

    synchronized void move(int p, int direction) {
        if (p < players.size()) players.get(p).dir = direction;
    }

    synchronized void pause(boolean on) {
        if (state.equals("play")) paused = on;
    }

    // Put every player's score in the scoreboard (once, when the game is over)
    void saveScores() {
        scoreBoard.clearLatest();
        for (Player p : players) {
            if (p.score > 0) scoreBoard.add(p.name, p.score, difficulty);
        }
    }

    // Keep only letters, digits, spaces, "_" and "-", at most 12 characters
    String cleanName(String text, int index) {
        String name = text == null ? "" : text.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
        if (name.length() > 12) name = name.substring(0, 12).trim();
        return name.isEmpty() ? "Player " + (index + 1) : name;
    }

    // Is this dinosaur on the ground above the water of the pond?
    boolean overPond(Player p, Obstacle pond) {
        double center = p.x + 17;
        return p.y < 6 && center > pond.x + 8 && center < pond.x + pond.w - 8;
    }

    // A dinosaur that runs through a pond did not jump over it
    void markPonds(Player p) {
        for (Obstacle o : obstacles) {
            if (o.type.equals("pond") && overPond(p, o)) o.touched = true;
        }
    }

    // Add 1 to an achievement. If it is unlocked now, show a pop-up.
    void progress(String id) {
        String title = achievements.add(id);
        if (title != null) { toast = "Achievement unlocked: " + title; toastTime = 3.5; }
    }

    void setDifficulty(String level) {
        switch (level == null ? "" : level.toLowerCase()) {
            case "easy" -> {
                difficulty = "Easy";
                startHearts = 5;
                speedFactor = 0.85;
                gapFactor = 1.25;
                // slower, fewer obstacles
            }
            case "hard" -> {
                difficulty = "Hard";
                startHearts = 1;
                speedFactor = 1.2;
                gapFactor = 0.85;
                // faster, more obstacles
            }
            default -> {
                difficulty = "Normal"; startHearts = 3; speedFactor = 1.0;  gapFactor = 1.0;
            }
        }
    }

    // The height of one fireball (its top edge). Fireballs from the front and from behind use the same two heights:
    //   112 = flies low: duck under it (a ducking dino has its top at 134) or jump over it
    //   142 = rolls on the ground (GROUND - 18): jump over it
    double fireballHeight() {
        return random.nextInt(3) == 0 ? 112 : GROUND - 18;     // 1 in 3 flies low
    }

    double backFireballSpeed() {
        return BACK_FIREBALL_BASE + speed * BACK_FIREBALL_GROWTH;
    }

    // ---------- simple helper methods ----------

    int score()     { return (int) (distance / 10); }
    int stage()     { return score() / 500; }       // a new stage every 500 points
    boolean inCave(){ return stage() % 4 == 3; }    // every 4th stage is a cave
    int theme()     { return stage() % 5; }         // 5 environments
    void say(String text, double seconds) { message = text; messageTime = seconds; }

    // ---------- the main update: called about 60 times per second ----------

    synchronized void update(double dt) {
        if (paused) return;                            // paused: nothing moves
        messageTime -= dt;
        toastTime -= dt;
        shake = Math.max(0, shake - dt * 12);          // the shaking slowly stops
        if (!state.equals("play")) return;

        speed = Math.min(300 + score() * 0.13, 720) * speedFactor;   // the game gets faster
        distance += speed * dt;

        if (stage() != lastStage) {                    // new environment?
            lastStage = stage();
            if (!bossActive) say("Entering: " + (inCave() ? "Dark Cave" : THEME_NAMES[theme()]), 2.2);
        }

        updateDisaster(dt);
        updateBoss(dt);

        // Create a new obstacle after a random distance (not while the boss is here)
        if (!bossActive) spawnTimer -= speed * dt;
        if (!bossActive && spawnTimer <= 0) {
            spawnObstacle();
            double gap = 0.7 + random.nextDouble() * 0.8;
            if (disaster.equals("flood")) gap = 1.9 + random.nextDouble() * 1.1;   // fewer waves: the lightning is hard enough
            if (disaster.equals("meteor")) gap = 2.6 + random.nextDouble() * 1.5;  // very few obstacles during meteors
            spawnTimer = speed * gap * gapFactor + lastWidth;
        }

        for (Obstacle o : obstacles) {
            o.update(dt, speed);
            if (o.type.equals("lightning") && o.delay <= 0 && !o.struck) {    // BOOM!
                o.struck = true;
                shake = Math.max(shake, 4);
            }
        }
        for (Obstacle o : obstacles) {                   // obstacles that are leaving the game: did the player dodge them?
            if (!o.isOffScreen()) continue;
            if (o.type.equals("meteor")) { shake = 6; if (!o.used) progress("meteor"); }   // crashed without hitting anyone
            if (o.type.equals("lightning") && !o.used) progress("storm");                 // the bolt struck, but missed us
            if (o.type.equals("pond") && !o.touched) progress("croc");                    // nobody ran through the pond
        }
        obstacles.removeIf(o -> o.isOffScreen() || o.used);

        // Move the dinosaurs and check if they hit something
        boolean anyAlive = false;
        for (Player p : players) {
            if (!p.alive) continue;
            p.update(dt);
            markPonds(p);
            if (p.hurtTimer <= 0) checkHits(p);
            if (p.alive) anyAlive = true;
        }
        if (!anyAlive) { state = "over"; saveScores(); }   // everybody is dead
    }

    // Did this dinosaur touch something dangerous?
    void checkHits(Player p) {
        for (Obstacle o : obstacles) {
            if (o.type.equals("pond")) {
                if (overPond(p, o)) {  // on the ground above the water
                    hurt(p);
                    say("Chomp! The crocodile bit you!", 1.5);
                    p.y = 0.01; p.vy = 450;                                   // hop out and continue
                    return;
                }
            } else if (o.hits(p)) {
                o.used = true;
                hurt(p);
                return;
            }
        }
    }

    void hurt(Player p) {
        p.hearts--;
        p.hurtTimer = 1.6;                                // a short time without damage
        if (p.hearts <= 0) { p.alive = false; p.score = score(); }
    }

    // ---------- obstacles ----------

    void spawnObstacle() {
        int points = score();
        List<String> pool = new ArrayList<>();
        if (disaster.equals("flood")) {
            pool.addAll(List.of("wave", "wave", "bigWave"));       // waves of water: jump!
        } else if (disaster.equals("meteor")) {
            pool.addAll(List.of("smallCactus", "tallCactus", "rock"));   // only easy ground obstacles
        } else if (inCave()) {
            pool.addAll(List.of("rock", "stalactite", "stalactite", "bat", "smallCactus"));
        } else {
            pool.addAll(List.of("smallCactus", "tallCactus", "cactusGroup"));
            if (points > 150) pool.addAll(List.of("lowBird", "highBird")); // birds come later
            if (points > 300) pool.add("rock");
            if (points > 250) pool.addAll(List.of("pond", "pond"));                            // crocodile pond
        }
        String pick = pool.get(random.nextInt(pool.size()));
        switch (pick) {
            case "smallCactus" -> add("cactus", 18, 34, GROUND - 34, 0);
            case "tallCactus" -> add("cactus", 24, 50, GROUND - 50, 0);
            case "cactusGroup" -> add("cactus", 58, 34, GROUND - 34, 0);
            case "lowBird" -> add("bird", 36, 20, 110, 0);
            // duck under it
            case "highBird" -> add("bird", 36, 20, 80, 0);
            // do NOT jump
            case "rock" -> add("rock", 28, 22, GROUND - 22, 130);
            // rolls fast
            case "stalactite" -> add("stalactite", 24, 90, 40, 0);
            // duck!
            case "bat" -> add("bat", 30, 22, 104, 0);
            // duck!
            case "wave" -> add("wave", 44, 26, GROUND - 26, 0);
            case "bigWave" -> add("wave", 60, 38, GROUND - 38, 0);
            case "pond" -> add("pond", 110, 40, GROUND, 0);
            // jump over the hole!
        }
    }

    void add(String type, double w, double h, double y, double extraSpeed) {
        obstacles.add(new Obstacle(type, w, h, y, extraSpeed));
        lastWidth = w;
    }

    // ---------- disasters (flood and meteor shower) ----------

    void updateDisaster(double dt) {
        if (disaster.equals("none")) {
            // start a disaster when the score is high enough (not during a boss or in a cave)
            if (score() >= nextDisasterScore && !bossActive && !inCave()) {
                disaster = random.nextBoolean() ? "flood" : "meteor";
                disasterTime = 0; meteorTimer = 1.0; lightningTimer = 2.0;
                say(disaster.equals("flood") ? "DISASTER: Flood! Jump the waves, dodge the lightning (A / D)"
                                             : "DISASTER: Meteor shower! Walk with A / D or Left / Right", 3);
            }
            return;
        }
        disasterTime += dt;
        if (disaster.equals("flood")) {
            lightningTimer -= dt;
            if (lightningTimer <= 0) {
                lightningTimer = 1.8 + random.nextDouble() * 1.2;
                strikeLightning();
            }
        }
        if (disaster.equals("meteor")) {
            shake = Math.max(shake, 2);                      // the screen shakes a little
            meteorTimer -= dt;
            if (meteorTimer <= 0) {
                meteorTimer = 1.6 + random.nextDouble() * 0.9;   // fewer meteors
                dropMeteor();
            }
        }
        if (disasterTime >= DISASTER_TIME) {
            disaster = "none";
            nextDisasterScore = score() + 900;
            say("The disaster is over!", 2);
        }
    }

    // A meteor falls near a random player. It starts at the top-right and flies down-left.
    // First there is a red warning on the ground where it will crash.
    void dropMeteor() {
        Player target = players.get(random.nextInt(players.size()));
        double landX = Math.max(30, target.x + random.nextDouble() * 80 - 40);   // where it crashes
        double flyTime = 0.39;                              // seconds it needs to fall
        Obstacle meteor = new Obstacle("meteor", 30, 30, -40, 0);
        meteor.x = landX + 150;                             // start: top-right of the landing spot
        meteor.vx = -150 / flyTime;                         // moves to the left...
        meteor.vy = 440;                                    // ...and down (a bit slower than before)
        meteor.tx = landX + 15; meteor.ty = GROUND;         // the warning is drawn here
        meteor.delay = 1.0;                                 // 1 second of warning
        obstacles.add(meteor);
    }

    // Lightning strikes near a random player. A yellow warning shows where. Walk away!
    void strikeLightning() {
        Player target = players.get(random.nextInt(players.size()));
        Obstacle bolt = new Obstacle("lightning", 40, GROUND, 0, 0);
        bolt.x = Math.max(20, Math.min(420, target.x - 10 + random.nextDouble() * 80 - 40));
        bolt.delay = 1.0;
        bolt.life = 0.25;                                   // the bolt is visible for a quarter second
        obstacles.add(bolt);
    }

    // ---------- the boss ----------

    void updateBoss(double dt) {
        // Start the boss when the score is high enough (never in a cave or during a disaster)
        if (!bossActive && score() >= nextBossScore && !inCave() && disaster.equals("none")) {
            bossActive = true; bossLeaving = false;
            bossMode = random.nextInt(2);                    // random side
            bossX = WIDTH + 130; bossBackX = -150;
            bossTime = 0; fireTimer = 1.6; backTimer = 2.2;
            bossAttacks = 0; bossPairs = 0;
            String[] sides = {"attacks from the FRONT", "attacks from BEHIND"};
            say("BOSS: Fire Drake " + sides[bossMode] + "!", 3);
        }
        if (!bossActive) return;

        bossTime += dt;
        boolean front = bossMode != 1, back = bossMode != 0;
        double slide = Math.min(1, dt * 2);

        if (bossTime < BOSS_TIME) {
            bossX += ((front ? 600 : WIDTH + 130) - bossX) * slide;    // slide in from the right
            bossBackX += ((back ? 10 : -150) - bossBackX) * slide;     // slide in from the left
            if (front) {
                fireTimer -= dt;
                if (fireTimer <= 0) {                        // shoot faster and faster
                    fireTimer = (Math.max(0.8, 1.5 - bossTime * 0.05) + random.nextDouble() * 0.6);
                    attack(true);
                }
            }
            if (back) {
                backTimer -= dt;
                if (backTimer <= 0) {
                    backTimer = (Math.max(1.2, 2.0 - bossTime * 0.05) + random.nextDouble() * 0.8);
                    attack(false);
                }
            }
        } else {                                             // time is over: the boss leaves
            if (!bossLeaving) { bossLeaving = true; say("Boss defeated! +200 points", 2.2); progress("dragon"); }
            bossX += 350 * dt;
            bossBackX -= 350 * dt;
            if (bossX > WIDTH + 160 && bossBackX < -160) {
                bossActive = false;
                distance += 2000;                            // bonus: 200 points
                nextBossScore = score() + 1200;
                spawnTimer = 600;
            }
        }
    }

    // Two fireballs at the same time, one above the other. Together they block one way:
    //   "under" pair (heights 86 and 112): you cannot jump over them, so duck under them
    //   "over" pair (heights 112 and ground): you cannot duck, so jump high over them
    void shootPair(boolean fromFront) {
        double[] heights = random.nextBoolean() ? new double[]{86, 112} : new double[]{112, GROUND - 18};
        for (double y : heights) {
            if (fromFront) {
                Obstacle fireball = new Obstacle("fireball", 26, 18, y, 120);
                fireball.x = bossX - 40;                     // both start at the dragon's mouth
                obstacles.add(fireball);
            } else {
                Obstacle fireball = new Obstacle("backFireball", 26, 18, y, 0);
                fireball.x = 40;
                fireball.vx = backFireballSpeed();
                fireball.delay = BACK_WARNING_TIME;           // 0 = no warning arrow: a surprise
                obstacles.add(fireball);
            }
        }
    }

    // One attack of the dragon. fromFront = true: the dragon on the right. false: the dragon behind.
    void attack(boolean fromFront) {
        bossAttacks++;
        // Sometimes (1 in 5) two fireballs come together. Every boss fight has at least one double fireball.
        boolean pair = random.nextDouble() < 0.2 || (bossPairs == 0 && bossAttacks >= 5);
        if (pair) {
            bossPairs++;
            shootPair(fromFront);
            return;
        }
        if (fromFront) {
            Obstacle fireball = new Obstacle("fireball", 26, 18, fireballHeight(), 120);
            fireball.x = bossX - 40;                         // starts at the dragon's mouth
            obstacles.add(fireball);
        } else {
            Obstacle fireball = new Obstacle("backFireball", 26, 18, fireballHeight(), 0);   // the same heights as the front
            fireball.x = 40;
            fireball.vx = backFireballSpeed();                               // slower than the front fireballs
            fireball.delay = BACK_WARNING_TIME;           // 0 = no warning arrow: a surprise
            obstacles.add(fireball);
        }
    }

    // ---------- turn the game into text (JSON) for the HTML page ----------

    synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"state\":\"").append(state).append("\"")
          .append(",\"score\":").append(score())
          .append(",\"theme\":").append(theme())
          .append(",\"cave\":").append(inCave())
          .append(",\"distance\":").append((int) distance)
          .append(",\"disaster\":\"").append(disaster).append("\"")
          .append(",\"paused\":").append(paused)
          .append(",\"scores\":").append(scoreBoard.toJson())
          .append(",\"difficulty\":\"").append(difficulty).append("\"")
          .append(",\"toast\":\"").append(toastTime > 0 ? toast : "").append("\"")
          .append(",\"achievements\":").append(achievements.toJson())
          .append(",\"shake\":").append((int) (shake * 10) / 10.0)
          .append(",\"message\":\"").append(messageTime > 0 ? message : "").append("\"");

        sb.append(",\"players\":[");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"x\":").append((int) p.x).append(",\"y\":").append((int) p.y)
              .append(",\"duck\":").append(p.isDucking()).append(",\"alive\":").append(p.alive)
              .append(",\"skin\":").append(p.skin)
              .append(",\"name\":\"").append(p.name).append("\"")
              .append(",\"hearts\":").append(Math.max(0, p.hearts))
              .append(",\"maxHearts\":").append(p.maxHearts)
              .append(",\"hurt\":").append(p.hurtTimer > 0)
              .append(",\"score\":").append(p.alive ? score() : p.score).append("}");
        }

        sb.append("],\"obstacles\":[");
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"type\":\"").append(o.type).append("\",\"x\":").append((int) o.x)
              .append(",\"y\":").append((int) o.y).append(",\"w\":").append((int) o.w)
              .append(",\"h\":").append((int) o.h).append(",\"warn\":").append(o.delay > 0)
              .append(",\"tx\":").append((int) o.tx).append(",\"ty\":").append((int) o.ty)
              .append(",\"vx\":").append((int) o.vx).append(",\"vy\":").append((int) o.vy).append("}");
        }

        sb.append("],\"boss\":");
        if (bossActive) {
            boolean fighting = bossTime < BOSS_TIME;
            sb.append("{\"x\":").append((int) bossX).append(",\"backX\":").append((int) bossBackX)
              .append(",\"mode\":").append(bossMode)
              .append(",\"time\":").append(bossTime).append(",\"duration\":").append(BOSS_TIME)
              .append(",\"mouthOpen\":").append(fighting && bossMode != 1 && fireTimer < 0.35)
              .append(",\"backOpen\":").append(fighting && bossMode != 0 && backTimer < 0.35).append("}");
        } else {
            sb.append("null");
        }
        return sb.append("}").toString();
    }
}