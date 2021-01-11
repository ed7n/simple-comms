package eden.simplecomms.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import eden.common.ConfigFileReader;
import eden.common.Modal;
import eden.simplecomms.client.SC_ClientObject;

/** The {@code SC_ServerPresenter} class handles communication between itself
    and remote clients.

    @author     Brendon
    @version    u0r2, 09/09/2017
*/
public class SC_ServerPresenter implements Runnable {

    /** Configuration filename */
    public static final String CONFIG = "SC_CFG";

    private static final Modal MODAL
        = new Modal("ServerPresenter", System.out, 3, true);


    // public constants

    /** Unit Separator (US) character */
    public static final String DELIMITER = "\u001F";

    /** Start of Heading (SOH) character */
    public static final String SOH = "\u0001";

    /** End of Transmission (EOT) character */
    public static final String EOT = "\u0004";

    /** Server name */
    public static final String NAME;

    /** Server version */
    public static final String VERSION;

    /** Maximum number of clients registered at any given time */
    public static final byte MAX_CLIENTS;


    // static constants

    /** Query lead-in string */
    private static final String SIGNATURE = "EDN_SC  ";

    /** Query send string */
    private static final String SEND = "SEND";

    /** Query receive string */
    private static final String RECV = "RECV";

    /** Command prefix */
    private static final String COMMAND = "/";

    /** Post-registration lecture */
    private static final String LECTURE;

    /** Self response */
    private static final String RESPONSE;

    /** Help message */
    private static final String HELP;

    /** Time to wait for relevant activites in milliseconds */
    public static final short TIMEOUT;

    /** Maximum amount of time between messages in milliseconds */
    private static final int SPAM_TIMEOUT;

    /** Maximum number of spam messages sent */
    private static final short SPAM_THRESHOLD;

    static {

        try {
            VERSION = ConfigFileReader.read(CONFIG, 0);
            NAME = ConfigFileReader.read(CONFIG, 1);
            MAX_CLIENTS = Byte.parseByte(ConfigFileReader.read(CONFIG, 2));
            LECTURE = ConfigFileReader.read(CONFIG, 3);
            RESPONSE = ConfigFileReader.read(CONFIG, 4);
            HELP = ConfigFileReader.read(CONFIG, 5);

            TIMEOUT = Short.parseShort(
                    ConfigFileReader.read(SC_ServerPresenter.CONFIG, 7));

            SPAM_TIMEOUT = Integer.parseInt(ConfigFileReader.read(CONFIG, 9));

            SPAM_THRESHOLD = Short.parseShort(
                ConfigFileReader.read(CONFIG, 10));

        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /** Reserved keywords. Use on clients is disallowed */
    public static final Set<String> KEYWORDS = makeKEYWORDS();


    // instance variables

    /** Socket from which to receive remote communication */
    private final SC_ServerSocket socket;

    /** Thread to run socket on */
    private Thread thread;

    /** Client registrar and management */
    private final ClientManager clientManager;


    // constructors

    /** Constructs an instance of this class. Arguments are read from the
        configuration file

        @throws     RuntimeException
                    If an exception is caught at socket construction, in which
                    case details are in the message
    */
    public SC_ServerPresenter() throws RuntimeException {

        // socket, thread
        SC_ServerSocket temp;

        try {
            MODAL.println(Modal.Mode.INFO, "Constructing ServerSocket...");

            temp = new SC_ServerSocket(
                Integer.parseInt(ConfigFileReader.read(CONFIG, 6)), this);

            MODAL.println(Modal.Mode.INFO, "...ServerSocket constructed");

            this.thread = new Thread(temp);
        } catch (IOException | IllegalArgumentException e) {

            MODAL.println(Modal.Mode.ERROR,
                "...exception caught at ServerSocket construction: "
                + e.toString());

            throw new RuntimeException(e);
        }
            this.socket = temp;

            // clientManager
            this.clientManager = new ClientManager(MAX_CLIENTS);
    }


    // methods

    /** Listens for queries */
    @Override
    public void run() {
        MODAL.println(Modal.Mode.INFO, "Server started");
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            MODAL.println(Modal.Mode.ALERT, "Server interrupted");
        }
        MODAL.println(Modal.Mode.ALERT, "Server stopped");
    }

    /** Parses a query and perform its respective action and/or send its
        appropriate response

        @param      address
                    Client from which the query is received

        @param      query
                    Query to be parsed
    */
    public void readQuery(final SC_ClientObject address, final String query) {

        MODAL.println(Modal.Mode.DEBUG,
            "<" + address.getName() + "> Got query: " + query);

        final String[] queries = query.split(DELIMITER, 3);
        final String sender;
        final SC_ClientObject recipient;
        final String message;

        // sender registration
        if (!clientManager.isRegistered(address.getName())) {
            clientManager.deregister(address);

            MODAL.println(Modal.Mode.ERROR,
                "<" + address.getName() + "> Not registered. Will be kicked");

            kick(address, "You are not registered");
            return;

        // lead in; signature
        } else if (!isValidQuery(queries)) {
            clientManager.deregister(address);

            MODAL.println(Modal.Mode.ERROR,
                "<" + address.getName() + "> Sent bad query. Will be kicked");

            kick(address, "Your client sent an illegal query");
            return;

        // command
        } else if (isCommand(queries[2])) {
            sender = NAME;
            recipient = address;
            message = clientManager.readCommand(address.getName(), queries[2]);

        // message to server
        } else if (address.getAddress().equalsIgnoreCase(NAME)
            || address.getAddress().equalsIgnoreCase("Server")) {

            sender = NAME;
            recipient = address;
            message = RESPONSE;

        // recipient registration
        } else if (!clientManager.isRegistered(address.getAddress())) {
            sender = NAME;
            recipient = address;
            message = address.getAddress() + " is not registered";

        // message to recipient
        } else {
            sender = address.getName();
            recipient = clientManager.getClient(address.getAddress());
            message = queries[2];
        }

        // push to recipient
        MODAL.println(Modal.Mode.DEBUG,
            "<" + sender + "> -> <" + recipient.getName() + ">: " + message);

        socket.sendQuery(recipient, makeQuery(sender, message));

        // added in r2
        // check spam
        if (clientManager.isSpamming(address, System.currentTimeMillis())) {

            MODAL.println(Modal.Mode.ALERT,
                "<" + address.getName() + "> Spammed. Will be kicked");

            kick(address, "You sent too many messages at a time");
        }
    }

    /** Registers a client

        @param      socket
                    Client socket to be wrapped

        @return     Registered client

        @throws     IOException
                    If an I/O error occurs when creating its I/O streams or if
                    its socket is not connected
    */
    public synchronized SC_ClientObject register(final Socket socket) throws
        IOException {

        SC_ClientObject newClient = clientManager.register(socket);

        if (clientManager.clients.size() <= MAX_CLIENTS) {
            this.socket.sendQuery(newClient, makeQuery(NAME, LECTURE));

            MODAL.println(Modal.Mode.INFO,
                "<" + newClient.getName() + "> Registered");

            return newClient;
        } else {
            kick(newClient, "Server fully occupied");

            MODAL.println(Modal.Mode.ALERT, "<"
                + newClient.getName() + "> Not registered. Server full");

            return null;
        }
    }

    /** Deregisters a client

        @param      client
                    Client to be deregistered

        @param      reason
                    Reason to deregister-ation
    */
    public synchronized void kick(final SC_ClientObject client, final String
        reason) {

        this.socket.sendQuery(client,
            makeQuery(NAME, "/kick " + reason));

        if (clientManager.deregister(client)) {

            MODAL.println(Modal.Mode.INFO,
                "<" + client.getName() + "> Deregistered");
        } else {

            MODAL.println(Modal.Mode.ERROR,
                "<" + client.getName()
                + "> Not deregistered. Not registered");
        }

        try {
            Thread.sleep(TIMEOUT);

            if (!client.getSocket().isClosed()) {
                client.getSocket().close();

                MODAL.println(Modal.Mode.INFO,
                    "<" + client.getName() + "> Force-disconnected");
            } else {
                MODAL.println(Modal.Mode.INFO,
                    "<" + client.getName() + "> Self-disconnected");
            }

        } catch (IOException | InterruptedException e) {

            MODAL.println(Modal.Mode.ERROR,
                "<" + client.getName() + "> Not disconnected. Whatever");
        }
    }

    /** Closes its underlying socket. This notifies all clients of the
        shutdown event before performing the actual shutdown */
    public void shutdown() {
        MODAL.println(Modal.Mode.ALERT, "Shutting down server...");

        clientManager.getStream()
            .forEach(c -> kick(c, "Server shutdown"));

        socket.shutdown();
        MODAL.println(Modal.Mode.ALERT, "...server shutdown");
    }


    // helper methods

    /** Returns whether a query is valid

        @param      query
                    Query split into query chunks to be validated

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    private boolean isValidQuery(String[] query) {

        return (query.length == 3)
            && (query[0].equals(SIGNATURE))
            && (query[1].equals(SEND))
            && (!query[2].matches(".*" + DELIMITER + "+.*|.*" + EOT + "+.*"));
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

    /** Makes a receive query

        @param      sender
                    Name of node from which this query is made

        @param      message
                    Message to be contained

        @return     Query {@code String}
    */
    private String makeQuery(String sender, String message) {

        return (SOH + SIGNATURE
            + DELIMITER + RECV
            + DELIMITER + sender
            + DELIMITER + message
            + EOT);
    }

    /** Helper method to construct the {@code KEYWORDS} constant

        @return     {@code Object} to be assigned to the {@code KEYWORDS}
                    constant
    */
    private static Set<String> makeKEYWORDS() {
        Set<String> temp = new HashSet<>();
        temp.add(NAME);
        temp.add("Server");
        temp.add("Client");
        temp.add("System");
        temp.add("Administrator");
        temp.add("Admin");
        return Collections.unmodifiableSet(temp);
    }


    // helper classes

    /** The {@code ClientManager} inner class manages client registration via
        user commands
    */
    private class ClientManager {

        // instance variables

        /** Registered clients */
        private final HashSet<SC_ClientObject> clients;


        // constructors

        /** Constructs an instance of this class with a given capacity

            @param      initialCapacity
                        Initial capacity number of registered clients
        */
        private ClientManager(int initialCapacity) {
            this.clients = new HashSet<>(initialCapacity);
        }


        // methods

        /** Returns whether a client is registered

            @param      name
                        Name of client to be checked

            @return     {@code true} if the condition is true;
                        [@code false} otherwise
        */
        private boolean isRegistered(final String name) {

            return clients.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name));
        }

        /** Returns a client by name

            @param      name
                        Name of client to be returned

            @return     Client with the given name;
                        {@code null} if this client does not exist
        */
        private SC_ClientObject getClient(final String name) {

            return clients.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
        }

        /** Returns a {@code Stream} of clients

            @return     {@code Stream} of clients
        */
        private Stream<SC_ClientObject> getStream() {
            return clients.stream();
        }

        /** Registers a client

            @param      socket
                        Client socket to be wrapped

            @returns    Registered client

            @throws     IOException
                        If an I/O error occurs when creating its I/O streams or
                        if its socket is not connected
        */
        private synchronized SC_ClientObject register(final Socket socket)
            throws IOException {

            SC_ClientObject newClient = new SC_ClientObject(socket);
            clients.add(newClient);
            return newClient;
        }

        /** Deregisters a client

            @param      client
                        Client to be deregistered

            @return     {@code true} if the operation was successful;
                        {@code false} otherwise
        */
        private synchronized boolean deregister(final SC_ClientObject client) {
            return clients.remove(client);
        }

        /** Returns whether a client is spamming

            @param      client
                        Client to be checked

            @param      time
                        Current epoch time

            @return     {@code true} if the condition is true;
                        {@code false} otherwise
        */
        private boolean isSpamming(final SC_ClientObject client, long time) {

            if ((time - client.getLastTime()) < SPAM_TIMEOUT) {

                client.setSpams(client.getSpams() + 1);

                if (client.getSpams() >= SPAM_THRESHOLD) {
                    return true;
                }
            } else {
                client.setSpams(0);
            }
            client.setLastTime(time);
            return false;
        }

        /** Parses a command and perform its respective action(s) and/or return
            its appropriate response(s)

            @param      address
                        Name of client from which the query is received

            @param      input
                        Command to be parsed

            @return     Response {@code String}
        */
        private String readCommand(final String address, final String input) {
            final String[] inputs = input.split("[ \t]", 2);
            final String command = inputs[0];
            final String arguments = (inputs.length == 1) ? "" : inputs[1];

            // help
            if (command.equalsIgnoreCase("/help")) {
                return HELP;

            // getName
            } else if (command.equalsIgnoreCase("/getName")) {
                return getName(address);

            // setName
            } else if (command.equalsIgnoreCase("/setName")) {
                return setName(address, arguments);

            // getAddress
            } else if (command.equalsIgnoreCase("/getAddress")) {
                return getAddress(address);

            // setAddress
            } else if (command.equalsIgnoreCase("/setAddress")) {
                return setAddress(address, arguments);

            // disconnect
            } else if (command.equalsIgnoreCase("/disconnect")) {
                return disconnect(address, arguments);
            }
            return "Command not interpreted";
        }

        private String getName(final String address) {
            return "Your are " + getClient(address).getName();
        }

        private String setName(final String address, final String newName) {

            if (!KEYWORDS.stream()
                    .anyMatch(s -> s.equalsIgnoreCase(newName))
                && !clients.stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(newName))
                && !newName.matches(".*" + DELIMITER + "+.*|.*" + EOT + "+.*"
                    + "|.*\\s+.*")
                && !isCommand(newName)
                && newName.length() > 0) {

                String oldName = getClient(address).getName();

                String newerName = (newName.length() > 16) ?
                    newName.substring(0, 16) : newName;

                getClient(address).setName(newerName);

                return ("Name set from " + oldName + " to " + newerName);
            }
            return "Name not set. Can not be used";
        }

        private String getAddress(final String address) {

            return "You are now addressing future messages to "
                + getClient(address).getAddress();
        }

        private String setAddress(final String address,
            final String newAddress) {

            if (KEYWORDS.stream()
                    .anyMatch(s -> s.equalsIgnoreCase(newAddress))
                || newAddress.matches(".*" + DELIMITER + "+.*|.*" + EOT + "+.*"
                    + "|.*\\s+.*")
                || isCommand(newAddress)
                || !(newAddress.length() > 0)) {

                return "Address not set. Name can not be used";
            }

            if (isRegistered(newAddress)) {
                getClient(address).setAddress(newAddress);
                return ("Future messages Will be sent to " + newAddress);
            }
            return ("Address not set. " + newAddress + " is not registered");
        }

        private String disconnect(final String address, final String reason) {
            Thread.currentThread().interrupt();
            deregister(getClient(address));
            return "Disconnect acknowledged";
        }
    }
}