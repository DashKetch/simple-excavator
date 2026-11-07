# Simple Excavator ⛏️

A client-side Fabric mod for Minecraft 1.21.11 designed to streamline resource gathering and large-scale terraforming through automated, legitimate QoL mining. Configure complex geometric shapes, highlight your target area in-world, and let the mod handle efficient pathing, movement, and server-synced block breaking.

---

## ✨ Features

* **Geometric Shapes**: Easily configure and mine out:
  * Rectangular Prisms
  * Spheres
  * Vertical Cylinders
  * Horizontal Cylinders
* **Two-Corner Bounding Selection**: Define your work zone dynamically by selecting **Corner 1** and **Corner 2** via the configuration GUI.
* **In-World Wireframe Rendering**: Real-time visual previews outlining your remaining excavation targets.
* **Smart Pathing & Auto-Movement**: Automatically calculates the most efficient route (top-down progression prioritized for safety) and walks you toward out-of-reach blocks.
* **Server-Sync Protection**: Persistently monitors server states to prevent rollbacks, desyncs, or skipped blocks.
* **Fully Client-Side**: Works on multiplayer servers (as a legitimate client-side utility performing standard input/mining actions).

---

## ⌨️ Controls & Keybindings

| Key | Action |
| :--- | :--- |
| **`U`** | Open/Close Excavator Configuration GUI |
| **`P`** | Toggle Automated Excavation On / Off |

---

## 📖 How to Use

1. Press **`U`** to open the Excavator Configuration screen.
2. Stand at your first boundary point and click **"Set Corner 1 (Here)"**.
3. Move to the opposite corner of the area you wish to clear and click **"Set Corner 2 (Here)"**.
4. Cycle through the available shapes (**Rectangle**, **Sphere**, **Vertical Cylinder**, **Horizontal Cylinder**) to fit your desired excavation profile.
5. Close the GUI and press **`P`** to begin the automated excavation loop.

---

## 📦 Requirements

* **Minecraft:** `1.21.11`
* **Fabric Loader:** `0.20.1` or higher
* **Fabric API**
