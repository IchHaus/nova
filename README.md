# Nova Client (NeoForge 26.3)

Client-seitiger Utility-Mod mit ClickGUI, ESP, Bewegungs-, Kampf- und Welt-Hacks.
Gedacht für Singleplayer und private Server, auf denen so etwas erlaubt ist.

## Bauen (ohne lokales Setup)

1. Neues GitHub-Repository anlegen und alle Dateien dieses Ordners hochladen (inklusive `.github`).
2. Unter **Actions** läuft der Workflow **Build** automatisch.
3. Danach unter dem Lauf bei **Artifacts** die Datei `NovaClient-jar` herunterladen.
4. Die `.jar` in den `mods`-Ordner von NeoForge 26.3 legen.

Lokal geht es mit JDK 25: `./gradlew build` (Jar landet in `build/libs`).

Falls der Build meldet, dass `net.neoforged:neoforge` nicht gefunden wird, in `gradle.properties` bei
`neo_version` die neueste 26.3.0.x Version eintragen
(https://projects.neoforged.net/neoforged/neoforge).

## Bedienung

| Taste / Befehl | Wirkung |
| --- | --- |
| **Rechtsshift** | ClickGUI öffnen und schließen |
| Linksklick auf Modul | an/aus |
| Rechtsklick auf Modul | Einstellungen aufklappen |
| Mittelklick auf Modul | Taste belegen (Entf/Backspace löscht die Belegung) |
| Mausrad im GUI | alle Panels scrollen |
| Kopfzeile ziehen / Rechtsklick | Panel verschieben / einklappen |

Standard-Tasten: `R` Killaura, `G` Flight, `H` ESP, `C` Fullbright, `V` XRay, `X` OreESP, `Z` Zoom, `B` FastPlace,
`Ende` Panic.
Alle lassen sich im GUI oder mit `.bind` ändern.

Chat-Befehle (mit Punkt davor, werden nicht an den Server gesendet):

```
.help                              alle Befehle
.t <hack>                          Hack an/aus
.list                              alle Hacks, aktive in grün
.bind <hack> <taste|none>          Taste festlegen
.set <hack> <einstellung> <wert>   z. B.  .set killaura range 5
.friend add|remove|list [name]     Freunde werden nie angegriffen
.xray add|remove|list|reset [block]  X-Ray-Blockliste, z. B.  .xray add minecraft:glowstone
.librarian add|remove|list|clear|reset  Wunschbücher für AutoLibrarian
.panic                             Panic: alles aus, Client stumm
.panickey [taste]                  Panic-Taste anzeigen oder ändern
.nova on|off                       Client wieder an (nur im Panic-Zustand nötig) / Panic
```

Einstellungen, Tasten und Freunde stehen in `config/novaclient.json`.

## X-Ray

Echtes X-Ray wie bei Wurst: Alle Blöcke außer denen in der Liste werden unsichtbar, die Höhlen-Culling-Optimierung
wird abgeschaltet, damit auch tief liegende Erze erscheinen, und Nachtsicht macht alles hell (Einstellung *Brightness*).
Standardliste: alle Erze, Antiker Schutt, Gilded Blackstone, Spawner, Trial Spawner, Vault, Kisten, Endertruhen und
Endportalrahmen. Mit `.xray add/remove` änderst du die Liste (der Block-Name geht mit oder ohne `minecraft:`).
Flüssigkeiten bleiben sichtbar, und es gibt keinen Transparenz-Modus.

X-Ray greift per Mixin in den Chunk-Renderer ein. Wenn die Mixins nach einem Minecraft-Update nicht mehr passen,
schaltet sich X-Ray nach 3 Sekunden selbst aus und schreibt eine Meldung in den Chat. Der Rest des Clients läuft dann
trotzdem weiter. Die Ursache steht in `logs/latest.log`.

## AutoLibrarian

Wie bei Wurst: Du stellst dich so hin, dass ein Bibliothekar und sein Lectern in Reichweite sind (am besten direkt
neben den Lectern), und schaltest **AutoLibrarian** ein. Der Hack öffnet das Handelsfenster, liest das Buch-Angebot
und baut den Lectern ab, wenn es nicht passt. Dann wartet er, bis der Villager seinen Beruf verloren hat, setzt den
Lectern neu und schaut wieder nach. Das wiederholt sich, bis der Villager eines deiner Wunschbücher verkauft. Dann
schaltet sich der Hack aus.

```
.librarian add mending               Mending (höchste Stufe, bis 64 Smaragde)
.librarian add sharpness 5 40        Schärfe 5, aber höchstens 40 Smaragde
.librarian remove fortune            alle Fortune-Wünsche entfernen
.librarian list / clear / reset      Liste anzeigen / leeren / Standardliste (wie bei Wurst)
```

- Der abgebaute Lectern muss wieder aufgesammelt werden. Das klappt nur, wenn du direkt daneben stehst.
- Die Einstellung *Lock in trade* kauft nach dem Treffer etwas beim Villager (24 Papier und 9 Smaragde, oder
  1 Buch und 64 Smaragde), damit er seine Angebote nie mehr ändert.
- Villager, die schon Erfahrung haben, lassen sich nicht mehr trainieren. Der Hack überspringt sie und sucht einen anderen.
- Sperr den Villager ein, damit er nicht wegläuft. Nur Bibliothekare der Stufe 1 werden angesprochen.

## Panic

Die Panic-Taste (Standard **Ende**, änderbar mit `.panickey`) oder `.panic` schaltet den ganzen Client ab:

- alle Hacks werden ausgeschaltet (inklusive X-Ray, die Chunks werden neu geladen),
- ClickGUI, HUD, Tastenbelegungen, Befehle und FOV-Änderungen sind danach stumm,
- es erscheint absichtlich keine Chatmeldung,
- Chatnachrichten mit Punkt gehen im Panic-Zustand ganz normal an den Server, nur `.nova on` wird abgefangen.

Wieder einschalten: Panic-Taste noch einmal drücken oder `.nova on` tippen. Die Hacks bleiben dabei aus, bis du sie
selbst wieder einschaltest.

Panic **entlädt den Mod nicht**. Er bleibt im Spiel geladen, tut aber nichts mehr.


## Hacks (42)

**Combat:** Killaura, TriggerBot, AimAssist, AutoTotem, AutoEat, AutoLeave
**Movement:** Flight, Jetpack, Glide, NoFall, AutoSprint, Spider, Dolphin, HighJump, AirJump, FastLadder, Parkour, BunnyHop, AutoWalk, Sneak, Speed, Step
**Render:** ESP, Tracers, Fullbright, ChestESP, XRay, OreESP, Zoom, Radar
**Player:** AutoRespawn, AutoArmor, FastPlace, FastBreak, AutoTool, AntiAFK, ChestStealer
**World:** Nuker, Scaffold, AutoMine, AutoLibrarian
**Misc:** FancyChat

## Was bewusst fehlt

Der Mod nutzt nur für X-Ray Mixins (4 kleine, in `dev.nova.client.mixin`), alles andere läuft über Events.
Deshalb gibt es Hacks nicht, die tief in Minecraft eingreifen müssen: Freecam, Timer, Reach, NoSlowdown,
AntiKnockback, Jesus, Safewalk oder AutoFish.

## Aufbau

```
dev/nova/client
  NovaClient.java / NovaEvents.java   Einstieg und alle Event-Handler
  core/                               Hack-Basis, Einstellungen, HackManager, Commands, Freunde, Panic
  mixin/                              X-Ray (Blöcke ausblenden, Culling aus)
  hacks/                              die eigentlichen Hacks, nach Kategorie sortiert
  render/                             3D-Zeichnen durch Wände (ESP-Pipelines, Buffer)
  gui/                                ClickGUI, HUD
  util/                               Farben, Hilfsfunktionen
```

Neuen Hack hinzufügen: Klasse von `Hack` ableiten, in `HackManager` mit `register(...)` eintragen.

## Lizenz und Herkunft

Die Render-Technik (eigene Pipelines ohne Tiefentest, Vertex-Buffer) orientiert sich an
[Wurst Client](https://github.com/Wurst-Imperium/Wurst7) (GPLv3). Deshalb steht auch dieses Projekt
unter GPL-3.0-or-later.
