# Sniffa Hide & Seek

Die Event-Mod für **Marguhls Hide & Seek**, gemacht von **jakubxyz** für [Sniffa Studio](https://sniffa.studio).

Ohne diese Mod kommst du nicht auf den Event-Server. Sie zeigt dir alles, was du im Event brauchst: das
Fähigkeiten-Rad, Radar und Karte, die Border, deine Rolle und den Stand der Runde.

Minecraft Java 1.21.11 mit Fabric.

## Installieren

**Am einfachsten:** Lade dir das Modpack auf [sniffa.studio/hs](https://sniffa.studio/hs) herunter, für den
NoRiskClient oder die Modrinth App. Da ist alles drin: die Mod, Simple Voice Chat und ein paar Mods für mehr FPS.

**Von Hand:**

1. Installiere den [Fabric Loader](https://fabricmc.net/use/installer/) für Minecraft 1.21.11.
2. Lade `marguhl-hideandseek-<version>.jar` unter [Releases](https://github.com/sniffa-studio/sniffa-hideandseek/releases)
   herunter und leg die Datei in deinen `mods`-Ordner. Mehr brauchst du nicht.
3. Starte Minecraft und tritt dem Event über den Titelbildschirm bei.

Solange die Mod installiert ist, zeigt der Titelbildschirm das Event. Nach dem Event kannst du sie einfach wieder
löschen.

## Ist die Mod sicher?

Ja. Der komplette Quellcode liegt hier offen, jeder kann nachlesen, was die Mod macht.

**Was die Mod macht**

- Sie zeigt an, was der Event-Server ihr schickt.
- Beim Betreten des Event-Servers sagt sie dem Server ihre Version und welche Mods du installiert hast (nur die
  Namen). So kann der Server Mods wie Freecam oder Cheat-Clients abweisen.
- Auf dem Titelbildschirm schaut sie nach, ob das Event offen ist. Das ist dieselbe Abfrage, die Minecraft für jeden
  Server in deiner Serverliste macht.
- Der Discord-Knopf öffnet `discord.sniffa.studio` im Browser, erst nachdem du es bestätigt hast.
- Hast du Iris, schaltet sie die Shader auf dem Event-Server aus und danach wieder an.
- Sie speichert genau eine Datei: `config/hideandseek.properties`.

**Was die Mod nicht macht**

- Sie lädt nichts aus dem Internet nach und startet keine Programme.
- Sie liest keine anderen Dateien, keine Passwörter, keine Accounts.
- Sie schickt nur dem Event-Server etwas, über deine normale Minecraft-Verbindung. Auf anderen Servern schickt sie
  nichts.

**Selbst nachprüfen**

- In jedem Release stehen die SHA-256-Prüfsummen. Vergleich sie mit deiner Datei, unter Windows mit
  `certutil -hashfile marguhl-hideandseek-<version>.jar SHA256`.
- Manche Virenscanner schlagen bei `.jar`-Dateien grundlos an. Wenn du unsicher bist, lad die Datei bei
  [VirusTotal](https://www.virustotal.com) hoch.
- Du kannst die Mod auch selbst aus diesem Code bauen: `./gradlew build` (Java 21).

## Fragen

Auf [discord.sniffa.studio](https://discord.sniffa.studio) hilft dir das Team weiter.

## Lizenz

MIT, siehe [LICENSE](LICENSE). Das Icon und die Logos von Sniffa und Marguhl sind ausgenommen und dürfen nicht ohne
Erlaubnis verwendet werden.
