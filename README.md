# DupersUnited Mod

A Minecraft mod to help debug plugins and servers. This Mod does **NOT** give you dupes on Minecraft servers, just helps in finding them.

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

# How to use this mod

### Linking proxies to accounts
Once you first install the mod, and exit the welcome screen, you'll see a button in the top right saying "DupersUnited",
<img width="1357" height="709" alt="image" src="https://github.com/user-attachments/assets/efc30b9c-ed27-4abc-8b38-a83fb3a32d86" />
after clicking on the "DupersUnited" button, it will open this screen. You can also access this screen via the Multiplayer menu <img width="1353" height="708" alt="image" src="https://github.com/user-attachments/assets/8f6fe394-8279-42ac-a912-a111e5ab4ace" />
<img width="1919" height="1012" alt="image" src="https://github.com/user-attachments/assets/0194edf3-07ff-4662-813d-973e1844a453" />
Now to link an account to a proxy, you'll first have to create a proxy, click on the "Proxy Manager" button and create one, once you do that go back to the config screen and click on the "Accounts" button.
<img width="1918" height="224" alt="image" src="https://github.com/user-attachments/assets/5438b65c-3207-44c8-8010-313443253600" />
click on the "Proxy" button next to the account you want to link
<img width="1919" height="168" alt="image" src="https://github.com/user-attachments/assets/8e44485c-e205-440d-b22b-156b5b5a5580" />
then go back to "Proxy Manager" and make sure Proxies are **ENABLED** for it to work, now everytime you launch with the account or swap to it using our account menu it will automatically swap you to that proxy.

# What do all the buttons in the Multiplayer screen do?

### Server Alerts

Server Alerts lists known servers who use exploits to track users & servers who **ETHICALLY** monetize their servers (Non P2W/Gambling), use [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) or [OpSec](https://github.com/aurickk/OpSec) to be fully safe from those exploits. All Server Alerts does is puts a warning screen before logging into a server that uses an exploit
<img width="923" height="289" alt="image" src="https://github.com/user-attachments/assets/b43daa2a-efd0-4298-8683-7cee12490fff" />

### RP Bypass
Bypasses required resource pack on a servers
### Brand Spoof
Spoofs your brand to a Vanilla Client
## In-Game
By default you will have inventory buttons, open your inventory or any container to see them

<img width="1919" height="1002" alt="image" src="https://github.com/user-attachments/assets/4f3f4717-f7ff-4cb8-9173-f58cc986c7b5" />

### What does each inventory button do?
- "Close Without Packet" closes your current GUI without sending a packet to the server. (To restore press your V key)
- "Clear GUI Cache" will clear all your currently saved GUIs
- "DC & Send Packets" sends all currently queued packets (if you have any) and disconnects you from the server.
- "Delay Packets" will only pause **GUI** related packets.
- "Save GUI" saves your current GUI without closing it
- "Chat or command" allows you to type commands while inside a container.
- "Fabricate Packet" allows you to create a custom ClickSlotC2SPacket and ButtonClickC2SPacket within a window it creates.
- "Sync ID" number that makes sure the game knows which screen or menu the data is for..
- "Revision" number that increases every time something changes, so the game knows it has the newest version.
- "Copy GUI as JSON" copies GUI NBT as a JSON

To access the other modules in the mod, press your "K" key while ingame.

<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/4bec7de7-a015-4d9e-a177-e87e71b5d778" />

To access the commands run "/du help"
<img width="566" height="338" alt="image" src="https://github.com/user-attachments/assets/341289f1-2e6a-48a1-9b50-ed0cd537976a" />

