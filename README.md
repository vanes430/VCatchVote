# 🗳️ VCatchVote

![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge)
![API](https://img.shields.io/badge/API-Paper_/_Folia-green?style=for-the-badge)
![License](https://img.shields.io/badge/License-GPLv3-orange?style=for-the-badge)

**VCatchVote** is a professional, high-performance vote listener designed specifically for modern **Paper** and **Folia** servers. It provides a robust suite of tools to reward your players, manage server-wide vote parties, and track statistics with maximum efficiency and thread safety. 🚀

---

## 🛑 Requirements

To ensure stability and performance, this plugin requires:
*   **Server Software:** [Paper](https://papermc.io/) 1.20.4+, [Folia](https://github.com/PaperMC/Folia), or their respective forks.
*   **Votifier:** [VotifierPlus](https://github.com/vanes430/VotifierPlus) or [NuVotifier](https://www.spigotmc.org/resources/nuvotifier.13449/) (Soft-dependency).
*   **PlaceholderAPI:** (Optional) For dynamic placeholders.

---

## ✨ Key Features

*   **⚡ Native Threading:** Fully supports Folia's region-based multithreading using specific schedulers (Entity, Global, Async).
*   **🎉 Intelligent Vote Party:** Trigger global events after reaching a vote goal. 
    *   *Anti-Lag:* Distributes player rewards sequentially with a configurable tick delay.
*   **📊 Persistent Stats:** Tracks total, weekly, and monthly votes per player using a dedicated **SQLite** database with asynchronous writes.
*   **🔥 Vote Streak:** Rewards players for voting consecutive days. Includes a placeholder `%vcatchvote_streak%`.
*   **📅 Auto-Reset System:** Weekly stats reset every Monday at 00:00, and Monthly stats reset on the 1st of every month at 00:00.
*   **💤 Offline Vote Catching:** Queues rewards for offline players and delivers them upon join (JSON or Memory storage).
*   **🎁 Multi-Tier Rewards:**
    *   *Chance Rewards:* Configure multiple rewards with independent probabilities (e.g., 100% money + 5% rare key).
    *   *Party Rewards:* Global and per-player rewards once the goal is met.
*   **🤖 Discord Integration:** Built-in Webhook support to send vote notifications and party alerts to your Discord server.
*   **⏰ Vote Reminders:** Automatically reminds players to vote if they haven't voted in the last 24 hours (configurable).
*   **🎨 MiniMessage & Adventure:** Support for modern RGB, gradients, and hover/click events in all messages and vote links.
*   **🔗 Clickable Vote Links:** Revamped `/vote` command with clean display and automatic domain extraction.

---

## 🔨 Building from Source

To build the project locally, you will need **JDK 21** and **Maven**.

```bash
git clone https://github.com/vanes430/VCatchVote.git
cd VCatchVote
mvn clean install
```
The compiled jar will be located in the `target/` directory.

## 📥 Installation

1.  **Download** the latest `VCatchVote-1.0.0.jar`.
2.  **Install Dependencies:** Votifier (Plus or Nu) and PlaceholderAPI (Optional).
3.  **Drop** the jar into your `/plugins/` folder.
4.  **Restart** your server.
5.  **Configure** `config.yml` to your liking.

---

## ⚙️ Configuration Preview

The `config.yml` is organized into clean, dedicated sections:

```yaml
# Vote Links Settings (Input data)
vote-links-settings:
  links:
    - name: "PlanetMinecraft"
      url: "https://www.planetminecraft.com/server/example/vote/"

# Vote List Display (Used in /vote command)
# Placeholders: %name%, %url%, %domain%
vote-list-display:
  header: "<blue>==== <white>Vote Links</white> ====</blue>"
  format: "<gray>- <yellow>%name%</yellow>: <click:open_url:'%url%'><underlined><aqua>%domain%</aqua></underlined></click></gray>"

# Messages Settings
messages:
  prefix: "<gray>[<blue>VCatchVote</blue>] </gray>"
  
  # Sent to everyone
  broadcast:
    enabled: true
    message: "<green>Player %player% voted! <gray>(%current%/%target%)"

  # Sent to the voter
  private:
    enabled: true
    message: "<green>Thanks for voting on %service%!"

# Vote Party Settings
vote-party:
  enabled: true
  target: 50
  rewards:
    per-player:
      enabled: true
      delay-ticks: 10 
      commands: ["give %player% diamond 1"]

# Chance-based Rewards
normal-rewards:
  enabled: true
  rewards:
    - chance: 100.0 # Always give this
      commands:
        - "give %player% emerald 1"
    - chance: 5.0 # Rare bonus
      commands:
        - "give %player% diamond_block 1"

# Vote Streak Rewards
vote-streak:
  enabled: true
  rewards:
    3:
      - "msg %player% <green>You reached a 3-day streak!"
```

---

## 💻 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/vote` | Displays the list of vote links. | `None` |
| `/vcatchvote reload` | Reloads the configuration files. | `vcatchvote.admin` |
| `/vcatchvote fakevote <p> <s>` | Simulates a vote for testing. | `vcatchvote.admin` |
| `/vcatchvote set <p> <n>` | Sets a player's total vote count. | `vcatchvote.admin` |
| `/vcatchvote reset <p>` | Resets a player's total votes to 0. | `vcatchvote.admin` |
| `/vcatchvote waiting list` | Shows current pending offline votes. | `vcatchvote.admin` |
| `/vcatchvote waiting clear` | Clears the entire waiting queue. | `vcatchvote.admin` |

---

## 🧩 Placeholders

Requires **PlaceholderAPI**.

*   `%vcatchvote_current%` - Progress toward the next Vote Party.
*   `%vcatchvote_target%` - Vote Party goal.
*   `%vcatchvote_needed%` - Votes remaining until the party.
*   `%vcatchvote_player_votes%` - Total votes of the player viewing.
*   `%vcatchvote_votes_weekly%` - Weekly votes of the player viewing.
*   `%vcatchvote_votes_monthly%` - Monthly votes of the player viewing.
*   `%vcatchvote_player_votes_<player>%` - Total votes of a specific player.
*   `%vcatchvote_streak%` - Current daily vote streak of the player.

---

## 🛡️ Security & Performance

*   **Asynchronous Database:** Database operations (SQLite) never block the main server thread.
*   **Data Integrity:** Player progress is saved in `data.json` and `database.db` to prevent data loss.
*   **Thread Safety:** Designed from the ground up to be safe for Folia's regionalized threading.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0**. See the `LICENSE` file for details.

Developed with precision by **vanes430**
