# MamZłeIntencje

A mobile app for Android that monitors IPC layers, processes broadcast records, and scores risks using a custom CVSS v3.1 engine.

## Authors
* Karol Adamski
* Bartłomiej Masiak
* Mateusz Kowalczuk

## Features
* **Real-Time IPC Watchdog:** Collects live device broadcast events using low-level API dumps via Shizuku.
* **Custom CVSS v3.1 Engine:** Dynamically calculates security vectors and severity scores for every intent.
* **Passive Sniffing Detection:** Detects malware that sits silently in RAM and registers dynamic receivers for critical system actions.

## Tech Stack
* Kotlin, Jetpack Compose, Shizuku API, SQLite, Room
