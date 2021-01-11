package eden.simplecomms.client;

import java.util.Scanner;

import eden.common.GDMAudioEngine;
import eden.common.Modal;
import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/** The {@code SC_ClientPresenter} class handles communication between itself
    and a remote server.

    @author     Brendon
    @version    u0r2, 09/09/2017
*/
public class SC_ClientPresenter implements Runnable {

    private static final Modal MODAL
        = new Modal("System", System.out, 2, true);


    // public constants

    /** Unit Separator (US) character */
    public static final String DELIMITER = "\u001F";

    /** Start of Heading (SOH) character */
    public static final String SOH = "\u0001";

    /** End of Transmission (EOT) character */
    public static final String EOT = "\u0004";

    /** Client version */
    public static final String VERSION = "U0R0";

    /** Time to wait for relevant activites in milliseconds */
    public static final short TIMEOUT = 5000;


    // static constants

    /** Query lead-in string */
    private static final String SIGNATURE = "EDN_SC  ";

    /** Query send string */
    private static final String SEND = "SEND";

    /** Query receive string */
    private static final String RECV = "RECV";

    /** Command prefix */
    private static final String COMMAND = "/";

    // GDMAudioEngine
    private final GDMAudioEngine audio;
    private int audio_bell;


    // instance constants

    /** Socket from which to receive remote communication */
    private final SC_ClientSocket socket;

    /** Scanner from which to receive user input */
    private final Scanner scanner;


    // instance variables

    /** Thread on which to run scoket*/
    private Thread threadInput;

    /** Thread on which to receive user input*/
    private Thread threadOutput;

    /** Self client management */
    private ClientManager clientManager;


    // constructors

    /** Constructs an instance of this class with a given port number and the
        default host name and {@code Scanner}

        @param      port
                    Port number

        @throws     RuntimeException
                    If an exception is caught in which case details are in the
                    message
    */
    public SC_ClientPresenter(int port) throws RuntimeException {
        this("localhost", port, new Scanner(System.in));
    }
    /** Constructs an instance of this class with a given port number, host name
        and {@code Scanner}

        @param      host
                    Host name

        @param      port
                    Port number

        @param      s
                    {@code Scanner from which to receive user input}

        @throws     RuntimeException
                    If an exception is caught in which case details are in the
                    message
    */
    public SC_ClientPresenter(String host, int port, Scanner s) throws
        RuntimeException {

        // audio
        GDMAudioEngine.checkHost(false);
        audio = new GDMAudioEngine(1);

        try {
            audio_bell = audio.load("AUDIO/SPLASH.WAV");
        } catch (IOException | IllegalStateException | LineUnavailableException |
            UnsupportedAudioFileException e) {

            MODAL.println(Modal.Mode.ERROR,
                "exception caught while loading SPLASH.WAV: " + e.toString());
        }

        // socket, threadInput
        SC_ClientSocket temp;

        try {
            MODAL.println(Modal.Mode.INFO, "Connecting to server...");

            temp = new SC_ClientSocket(host, port, this);

            MODAL.println(Modal.Mode.INFO, "...connected to server");
            audio.playAndAwait(audio_bell);
            audio.unload(audio_bell);

            this.threadInput = new Thread(temp);
        } catch (IOException | IllegalArgumentException e) {

            MODAL.println(Modal.Mode.ERROR,
                "...can not connect to server: " + e.toString());

            throw new RuntimeException(e);
        }
            this.socket = temp;

            // scanner, clientManager
            this.scanner = s;
            this.clientManager = new ClientManager();
    }


    // methods

    /** Listens for queries and receives user input simulateously */
    @Override
    public void run() {

        try {
            audio_bell = audio.load("AUDIO/MSG_IN.WAV");
        } catch (IOException | IllegalStateException | LineUnavailableException |
            UnsupportedAudioFileException e) {

            MODAL.println(Modal.Mode.ERROR,
                "exception caught while loading MSG_IN.WAV: " + e.toString());
        }
        threadOutput = Thread.currentThread();
        threadInput.start();

        MODAL.println(Modal.Mode.INFO, "Client started");

        try {

            while (!threadInput.isInterrupted()
                && !threadOutput.isInterrupted()) {

                final String message;
                String temp;

                Thread.sleep(1000);
                System.out.print(">_");
                message = scanner.nextLine();

                if (!message.isEmpty()) {

                    command:
                    if (isCommand(message)) {
                        temp = clientManager.readLocalCommand(message);

                        if (temp != null) {
                            break command;
                        }
                    }
                    socket.sendQuery(makeQuery(message));
                } else {
                    MODAL.println(Modal.Mode.ERROR, "Input empty");
                }
            }
        } catch (InterruptedException e) {

            if (e instanceof InterruptedException) {
                MODAL.println(Modal.Mode.ALERT, "Client interrupted");
            }
        }
        MODAL.println(Modal.Mode.ALERT, "Client stopped");
    }

    /** Parses a query and perform its respective action

        @param      query
                    Query to be parsed
    */
    public void readQuery(final String query) {
        MODAL.println(Modal.Mode.DEBUG, "Got query: " + query);

        final String[] queries = query.split(DELIMITER, 4);
        final String message;

        // lead in; signature
        if (!isValidQuery(queries)) {
            MODAL.println(Modal.Mode.DEBUG, "Bad query ignored");
            return;

        // command
        } else if (isCommand(queries[3])) {
            message = clientManager.readRemoteCommand(queries[3]);

        // message from recipient
        } else {
            message = queries[3];
        }

        // print
        printMessage(queries[2], message);
    }

    public void kick() {
    }

    /** Closes its underlying socket. This notifies the server of the
        shutdown event before performing the actual shutdown */
    public void shutdown() {
        MODAL.println(Modal.Mode.ALERT, "Shutting down client...");
        audio.await(audio_bell);
        audio.unloadAll();

        if (!socket.isClosed()) {
            socket.sendQuery(makeQuery("/disconnect Client shutdown"));
            socket.shutdown();
        }
        threadInput.interrupt();
        threadOutput.interrupt();
        MODAL.println(Modal.Mode.ALERT, "...client shutdown");
    }


    // helper methods

    /** Returns whether a query is valid

        @param      query
                    Query split into query chunks to be validated

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    private boolean isValidQuery(String[] query) {

        return (query.length == 4)
            && (query[0].equals(SIGNATURE))
            && (query[1].equals(RECV))
            && (!query[3].matches(".*" + DELIMITER + "+.*|.*" + EOT + "+.*"));
    }

    /** Returns whether a {@code String} is a command

        @param      {@code String} to be validated

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    private boolean isCommand(String input) {

        return ((input.length() > 1)
            && (input.startsWith(COMMAND))
            && (!input.matches(COMMAND + "{2,}.*")));
    }

    /** Makes a send query to be sent to the server

        @param      message
                    Message to be contained

        @return     Query {@code String}
    */
    private String makeQuery(String message) {

        return (SOH + SIGNATURE
            + DELIMITER + SEND
            + DELIMITER + message
            + EOT);
    }

    /** Prints a message onto {@code System.out}

        @param      sender
                    Node name to be printed

        @param      message
                    Message to be printed
    */
    private void printMessage(String sender, String message) {
        audio.play(audio_bell);
        System.out.print("\n[" + sender + "]\n" + message + "\n\n");
    }


    // helper classes

    /** The {@code ClientManager} inner class manages client registration via
        user commands
    */
    private class ClientManager {

        // methods

        /** Parses a local command and perform its respective action(s) and/or
            return its appropriate response(s)

            @param      input
                        Command to be parsed

            @return     Response {@code String}
        */
        private String readLocalCommand(final String input) {
            final String[] inputs = input.split(" ", 2);
            final String command = inputs[0];
            final String arguments = (inputs.length == 1) ? "" : inputs[1];

            // disconnect
            if (command.equalsIgnoreCase("/disconnect")) {
                return disconnect(arguments);
            }
            return null;
        }

        /** Parses a remote command and perform its respective action(s) and/or
            return its appropriate response(s)

            @param      input
                        Command to be parsed

            @return     Response {@code String}
        */
        private String readRemoteCommand(final String input) {
            final String[] inputs = input.split(" ", 2);
            final String command = inputs[0];
            final String arguments = (inputs.length == 1) ? "" : inputs[1];

            // kick
            if (command.equalsIgnoreCase("/kick")) {
                return kick(arguments);
            }
            return "Command not interpreted on client";
        }

        private String disconnect(final String reason) {
            socket.sendQuery(makeQuery("/disconnect " + reason));
            threadInput.interrupt();
            threadOutput.interrupt();

            try {
                threadInput.join(TIMEOUT);
            } catch (InterruptedException e) {
                // dont care
            }
            return "Disconnected";
        }

        private String kick(final String reason) {
            threadInput.interrupt();
            threadOutput.interrupt();
            return "Kicked from server: " + reason;
        }
    }
}