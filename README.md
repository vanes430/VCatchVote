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
*   **📊 Persistent Stats:** Tracks total votes per player using a dedicated **SQLite** database with asynchronous writes.
*   **💤 Offline Vote Catching:** Queues rewards for offline players and delivers them upon join (JSON or Memory storage).
*   **🎁 Multi-Tier Rewards:**
    *   *Normal Rewards:* Executed instantly on every vote.
    *   *Party Rewards:* Global and per-player rewards once the goal is met.
*   **🎨 MiniMessage & Adventure:** Support for modern RGB, gradients, and hover/click events in all messages and vote links.
*   **🔗 Clickable Vote Links:** Easy `/vote` command for players to access voting sites.

---

## 📥 Installation

1.  **Download** the latest `VCatchVote-1.0.0.jar`.
2.  **Install Dependencies:** Votifier (Plus or Nu) and PlaceholderAPI (Optional).
3.  **Drop** the jar into your `/plugins/` folder.
4.  **Restart** your server.
5.  **Configure** `config.yml` to your liking.

---

## ⚙️ Configuration Preview

The `config.yml` is designed to be intuitive and safe:

```yaml
# Vote Party Settings
vote-party:
  enabled: true
  target: 50
  rewards:
    per-player:
      enabled: true
      delay-ticks: 10 # Prevents lag spikes on large servers
      commands: ["give %player% diamond 1"]
    global:
      enabled: true
      commands: ["broadcast <gold>Vote Party Started!"]

# Normal Rewards (Runs on every single vote)
normal-rewards:
  enabled: true
  commands:
    - "give %player% emerald 1"
    - "say %player% just voted!"

# Offline Support
waiting:
  enabled: true
  storage: "JSON" # JSON (persistent) or MEMORY (resets on restart)
  join-delay-ticks: 60 # Wait for player to fully load before giving rewards
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
*   `%vcatchvote_player_votes_<player>%` - Total votes of a specific player.

---

## 🛡️ Security & Performance

*   **Asynchronous Database:** Database operations (SQLite) never block the main server thread.
*   **Data Integrity:** Player progress is saved in `data.json` and `database.db` to prevent data loss.
*   **Thread Safety:** Designed from the ground up to be safe for Folia's regionalized threading.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0**. See the `LICENSE` file for details.

Developed with precision by **vanes430**
