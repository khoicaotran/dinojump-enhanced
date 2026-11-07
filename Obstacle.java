// Obstacle.java - anything that can hurt the dinosaur.
// type: "cactus", "bird", "rock", "stalactite", "bat", "wave", "pond", "fireball",
//       "backFireball" (from behind),
//       "meteor" and "lightning".
public class Obstacle {
    String type;          // the HTML page uses this name to decide how to draw it
    double x, y, w, h;    // position (x, y = top-left corner) and size
    double extraSpeed;    // some obstacles (rock, fireball) move faster than the ground
    double delay = 0;     // warning time before a meteor / lightning / back fireball starts
    boolean used = false; // true after it hit someone (then it disappears)
    boolean touched = false; // a pond: did a dinosaur run through it? (then it was not jumped over)

    // Some obstacles fly on their own path (they do not move with the ground)
    boolean free;
    double vx, vy;        // their speed to the right and down (pixels per second)

    double tx, ty;        // target spot: where a meteor lands (for the warning)
    double life = 0;      // how long a lightning bolt stays visible
    boolean struck = false; // has the lightning bolt already struck?

    Obstacle(String type, double w, double h, double y, double extraSpeed) {
        this.type = type;
        this.w = w;
        this.h = h;
        this.y = y;
        this.extraSpeed = extraSpeed;
        this.x = Game.WIDTH + 20;   // start just outside the right side of the screen
        this.free = type.equals("meteor") || type.equals("backFireball");
    }

    void update(double dt, double speed) {
        if (delay > 0) {                                        // still showing the warning
            delay -= dt;
            return;
        }
        if (free) { x += vx * dt; y += vy * dt; }               // fly on its own path
        else if (type.equals("lightning")) life -= dt;          // the bolt disappears after a moment
        else x -= (speed + extraSpeed) * dt;                    // everything else moves left
    }

    boolean isOffScreen() {
        if (type.equals("meteor")) return y + h >= Game.GROUND;                 // crashed into the ground
        if (type.equals("lightning")) return delay <= 0 && life <= 0;
        if (type.equals("backFireball")) return x > Game.WIDTH + 20;
        return x + w < -10;
    }

    // Do the two rectangles touch? (We make both a little smaller to be fair.)
    // Warnings and ponds never hit like this (the pond is checked in Game.java).
    boolean hits(Player p) {
        if (delay > 0 || type.equals("pond")) return false;
        double pLeft = p.x + 4,     pRight  = p.x + p.width() - 4;
        double pTop  = p.top() + 4, pBottom = Game.GROUND - p.y - 4;
        return pLeft < x + w - 3 && pRight > x + 3
            && pTop < y + h - 2  && pBottom > y + 2;
    }
}
