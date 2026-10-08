# ScreenBridge: a mini Parsec in Java

Remote desktop app: see another computer's screen live and control it with your mouse and keyboard.

## How to run
1. Install Java 17+ on both computers and connect both to the **same Wi-Fi/LAN**.
2. Double-click `run.bat` (or run `javac -d out src\*.java` then `java -cp out Main`).
3. On the PC to be controlled: choose **Share my screen (Host)**. It shows the IP, port and a 6-digit password.
4. On the other PC: choose **Connect to a PC (Client)**, enter that IP, port and password.

Testing on one laptop: run `run.bat` twice, start Host in one, and connect to `127.0.0.1` from the other.
If the connection fails between two PCs, allow Java through Windows Firewall on the host.

## How it works
| Part | Java feature used |
|---|---|
| Screen capture | `java.awt.Robot.createScreenCapture` |
| Compression | `ImageIO` JPEG writer (quality 0.6, max width 1280) |
| Networking | `ServerSocket` / `Socket` over TCP, `DataInput/OutputStream` |
| Concurrency | separate threads for sending frames and receiving input |
| Remote control | `Robot.mouseMove / mousePress / keyPress` |
| GUI | Swing (`JFrame`, `JPanel.paintComponent`, mouse/key listeners) |
| Security | random 6-digit password checked before streaming starts |

## Files
- `Main.java` – launcher (choose Host or Client)
- `HostServer.java` – captures screen, streams frames, replays input
- `ClientViewer.java` – displays frames, sends mouse/keyboard events
- `Protocol.java` – shared message types and port

## Limitations / future work
- ~15 FPS because it sends JPEG images, not a video stream (real Parsec uses H.264/H.265 hardware encoding over UDP).
- Same network only; over the internet it would need port forwarding or a relay server.
- Ideas to add: chat, file transfer, clipboard sync, audio, quality/FPS slider, encryption (TLS).
