# WorldPluginBlocker

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/xfrost21/WorldPluginBlocker)](https://github.com/xfrost21/WorldPluginBlocker/releases/latest)
[![GitHub license](https://img.shields.io/github/license/xfrost21/WorldPluginBlocker)](https://github.com/xfrost21/WorldPluginBlocker/blob/main/LICENSE)

## 🚧 About

**WorldPluginBlocker** is a lightweight plugin for Minecraft servers (Paper/Spigot) that empowers server administrators to **block the usage of commands** from specific plugins within designated worlds. This is an ideal solution for maintaining order and feature isolation (e.g., blocking skyblock commands in a lobby/spawn world).

---

## ✨ Key Features

* **Command Blocking:** Full control over command usage based on the player's world.
* **Alias Support:** Includes a robust mechanism for manual alias definition (e.g., `/is` and `/island`) to ensure stable blocking of problematic plugins.
* **Fully Configurable:** Customizable messages and easy rule configuration via `config.yml`.
* **Low Latency:** Designed for performance and efficiency.

---

## 🛠️ Installation

1.  Download the latest stable **`WorldPluginBlocker.jar`** from the [**Releases**](https://github.com/xfrost21/WorldPluginBlocker/releases/latest) section.
2.  Place the file into your Paper or Spigot server's `plugins/` folder (recommended version 1.18+).
3.  Start the server to generate the default `config.yml`.
4.  Configure your blocking rules in the `plugins/WorldPluginBlocker/` folder.

## ⚙️ Configuration

For complete configuration instructions, detailed examples, and explanations on how to manually block tricky aliases (like those for **SuperiorSkyblock2**), please refer to our full documentation:

➡️ **FULL DOCUMENTATION (GITBOOK):**  [DOCUMENTATION](https://frostcraft.gitbook.io/frostplugin-wiki/frost-plugins/worldpluginblocker)

---

## 🤝 Contributing

We welcome bug reports, feature suggestions, and Pull Requests! Please review our guidelines:

* **[CONTRIBUTING.md](CONTRIBUTING.md)**: How to contribute code and submit changes.
* **[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)**: Rules for community interaction.

---

## 📄 License

This project is released under the **[GPL-3.0](LICENSE)**.
