// Player.java - one dinosaur player.
public class Player {
    double x;                 // left position on the screen
    double y = 0;             // height above the ground (0 = standing on the ground)
    double vy = 0;            // vertical speed (positive = going up)
    int dir = 0;              // -1 = moving left, 0 = standing still, 1 = moving right
    boolean ducking = false;  // is the duck key held down?
    boolean alive = true;
    int hearts = 3;           // lives
    int maxHearts = 3;        // how many hearts we started with
    double hurtTimer = 0;     // seconds after a hit when the dinosaur cannot be hurt again
    int score = 0;            // final score, saved when the dinosaur dies
    String name = "Player";   // the name shown on the scoreboard
    int skin;                 // 0 = Rex, 1 = Blaze, 2 = Frosty

    // Each skin jumps in a different way
    static final double[] JUMP_POWER = {520, 560, 470};
    static final double[] GRAVITY    = {1900, 2400, 1300};

    Player(double x, int skin) {
        this.x = x;
        this.skin = skin;
    }

    void jump() {
        if (alive && y == 0) {      // we can only jump from the ground
            vy = JUMP_POWER[skin];
            y = 0.01;
        }
    }

    // Move the dinosaur. dt = seconds since the last update.
    void update(double dt) {
        hurtTimer -= dt;
        x += dir * 220 * dt;                      // walk left or right (used to dodge meteors)
        x = Math.max(30, Math.min(400, x));       // stay inside the screen

        if (y > 0 || vy > 0) {
            double gravity = GRAVITY[skin];
            if (ducking) gravity *= 2.4;          // ducking in the air = fall faster
            vy -= gravity * dt;                   // gravity slows the jump down
            y += vy * dt;
            if (y <= 0) { y = 0; vy = 0; }        // landed
        }
    }

    boolean isDucking() { return ducking && y == 0; }   // we only duck on the ground
    double width()  { return isDucking() ? 44 : 34; }
    double height() { return isDucking() ? 26 : 44; }
    double top()    { return Game.GROUND - y - height(); } // top edge on the screen
}
