# DupersUnited Mod

This is a Minecraft 26.2 Fabric mod to help debug plugins and servers. This Mod does **NOT** give you dupes on Minecraft servers, just helps in finding them.

## Installation

1. Download the latest release from the [releases](https://github.com/DupersUnited/dupersunited-mod/releases) page
2. Place the `.jar` file in your `mods` folder
3. Launch Minecraft with Fabric

## Building

Clone the repository and navigate into it:

```bash
git clone https://github.com/DupersUnited/dupersunited-mod.git
cd dupersunited-mod
```

Build the project:

```
./gradlew build
```

The compiled mod will be located in:

```
build/libs/
```

# Features

To access the config menu & other features that the mod has, press "K", and you will see the ClickGUI.

<img width="1919" height="1022" alt="image" src="assets/clickgui.png" />

## Account Manager

Our account manager imports your accounts from Prism Launcher, MultiMC Launcher & Meteor Account Manager, you have to have your accounts signed into those for it to show up in our account manager.

## GUI UTILS
- "Close Without Packet" closes your current GUI without sending a packet to the server. (To restore press your V key)
- "Clear GUI Cache" will clear all your currently saved GUIs.
- "DC & Send Packets" sends all currently queued packets (if you have any) and disconnects you from the server.
- "Delay Packets" will only pause **GUI** related packets.
- "Save GUI" saves your current GUI without closing it.
- "Chat or command" allows you to type commands while inside a container.
- "Fabricate Packet" allows you to create a custom ClickSlotC2SPacket and ButtonClickC2SPacket within a window it creates.
- "Sync ID" number that makes sure the game knows which screen or menu the data is for..
- "Revision" number that increases every time something changes, so the game knows it has the newest version.
- "Copy GUI as JSON" copies GUI NBT as a JSON.
- "Desync" Desyncs whatever GUi you are currently in.
