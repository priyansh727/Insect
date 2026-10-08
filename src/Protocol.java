/**
 * Shared constants for the ScreenBridge network protocol.
 *
 * Connection flow:
 *   1. Client sends the password (UTF string).
 *   2. Host replies with a boolean (true = accepted).
 *   3. If accepted, host sends its screen width and height (two ints).
 *   4. Host then streams frames forever: [int length][JPEG bytes].
 *   5. At the same time, client sends input events: [int type][int args...].
 */
public final class Protocol {
    public static final int DEFAULT_PORT = 5900;

    // Input event types sent from client to host
    public static final int MOUSE_MOVE = 1;    // args: x, y
    public static final int MOUSE_PRESS = 2;   // args: button mask
    public static final int MOUSE_RELEASE = 3; // args: button mask
    public static final int MOUSE_WHEEL = 4;   // args: notches
    public static final int KEY_PRESS = 5;     // args: key code
    public static final int KEY_RELEASE = 6;   // args: key code

    private Protocol() {
    }
}
