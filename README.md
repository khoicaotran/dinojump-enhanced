# DinoJump Enhanced

Every time the Wi-Fi went down, I ended up playing the little dinosaur game in Chrome. It is simple and it is hard to stop playing, but after a while I started to wonder what would happen if the dinosaur had to face a dragon, a meteor shower or a flood. DinoJump Enhanced is my answer to that question

This project is a remake of Chrome's offline Dino game with bosses, natural disasters, new environments, a 2-player mode and achievements. I wanted to keep the same "one more try" feeling of the original while adding more things to learn and master

> 📸 *Screenshots and a gameplay GIF coming soon*

## Features

### Gameplay
- **Three dinosaurs with different jumps:** Rex (balanced), Blaze (quick, low jump) and Frosty (floaty, high jump)
- **Five environments:** Desert Day, Sunset, Night, Snow Land and Volcano, with smooth color transitions between them
- **Dark Cave:** every fourth stage takes you underground, with stalactites, bats and rolling rocks
- **Crocodile ponds:** jump over them or lose a heart (you can still hop out and keep running)
- **Screen shake** for meteors and lightning strikes

### Boss fight: the Fire Drake
- The dragon attacks from the **front** or from **behind**
- Fireballs fly at two heights, so you have to decide quickly whether to duck or jump
- Sometimes the dragon shoots **two fireballs at once** that block one way out
- Fireballs from behind come with no warning and get faster as the game speeds up
- Survive the fight to earn a 200-point bonus

### Natural disasters
- **Flood:** jump over the waves while lightning strikes near you
- **Meteor shower:** meteors fall from the top right, so watch the red warning on the ground and walk out of the way

### Modes and progress
- **1-player and 2-player** (on the same keyboard), each player with their own name and colored marker
- **Difficulty levels:** Easy (5 hearts), Normal (3 hearts) and Hard (1 heart), which also change the speed and the number of obstacles
- **Top 5 scoreboard** that is saved between sessions
- **Four achievements:**
  - *Dragon Slayer:* survive the boss fight
  - *Meteor Dodger:* dodge 10 meteors
  - *Storm Chaser:* dodge 10 lightning bolts
  - *Croc Jumper:* jump over 5 crocodile ponds
- **Pause** and **sound effects** made with the Web Audio API (no sound files needed)

## Controls

| Action | Player 1 | Player 2 |
|---|---|---|
| Jump | `W` or `Space` | `↑` |
| Duck | `S` | `↓` |
| Move left / right | `A` / `D` | `←` / `→` |
| Pause | `P` or `Esc` | |
| Mute sound | `M` | |

In 1-player mode, both sets of keys control the same dinosaur

## How to Run

You need **Java 14 or newer** (Java 17 or 21 is recommended)

```bash
# 1. Clone the repository
git clone https://github.com/khoicaotran/dinojump-enhanced.git
cd dinojump-enhanced

# 2. Compile all Java files
javac *.java

# 3. Start the game server
java Main
```

Then open **http://localhost:8080** in your browser

## How It Works

I split the project into two parts so that each part has only one job. Java is the "brain" and holds every game rule. The browser only draws what Java tells it and sends back the player's key presses

```
┌──────────────────────────┐   "jump", "duck", "start"   ┌────────────────────────────┐
│  index.html (JavaScript) │  ─────────────────────────► │  Java (Main + Game)        │
│  - draws the game        │                             │  - all game rules          │
│  - reads the keyboard    │  ◄───────────────────────── │  - collisions, hearts,     │
│  - plays sounds          │    game state as JSON       │    boss, disasters, score  │
└──────────────────────────┘                             └────────────────────────────┘
```

- `Main.java` starts a small web server (using Java's built-in `HttpServer`) and runs the game loop about 60 times per second
- The browser sends commands like `/jump` and `/duck`. It also asks for the game state through `/state` many times per second
- Java answers with the full game state as JSON and the page draws it on an HTML canvas

Because all the rules live in Java and none of the drawing does, the game logic can be tested on its own without opening a browser

## Project Structure

| File | What it does |
|---|---|
| `Main.java` | Starts the web server, handles commands from the browser and runs the game loop |
| `Game.java` | The game rules: speed, stages, obstacles, boss fight, disasters and the JSON state |
| `Player.java` | One dinosaur: jumping, gravity, ducking and its hitbox |
| `Obstacle.java` | Everything that can hurt the dinosaur, plus collision checks |
| `ScoreBoard.java` | Keeps the top 5 scores and saves them to `scores.txt` |
| `Achievements.java` | Tracks badge progress and saves it to `achievements.txt` |
| `index.html` | Draws the game, reads the keyboard and plays sounds |

## What I Learned

- How to split a project so that the logic and the display stay separate and how the two sides talk through simple HTTP requests and JSON
- How to build a game loop with a fixed time step and simple physics like gravity and jumping
- How to design difficulty that feels fair. For example, I changed the fireballs from behind so their speed grows with the game speed, because a fixed speed felt too easy early on and too unfair later
- How to use Git and GitHub to keep a clean history of my work

## Roadmap

- [ ] Unit tests with JUnit for collisions, hearts, difficulty and achievements
- [ ] GitHub Actions to run the tests on every push
- [ ] Power-ups (for example a shield or a double jump)
- [ ] Coins and a small shop for dinosaur costumes
- [ ] An online version that anyone can play without installing Java

## Acknowledgments

Inspired by the Chrome Dino game made by the Google Chrome team. All the drawings in this project are made with code on an HTML canvas

## Author

**Khoi Cao Tran**, Computer Science student at Seattle University
GitHub: [@khoicaotran](https://github.com/khoicaotran)
