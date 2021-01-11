package eden.simplecomms.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eden.common.ConfigFileReader;
import eden.common.Modal;
import eden.simplecomms.client.SC_ClientObject;

/** The {@code SC_ServerSocket} class sends and receives queries between itself
    and remote clients. It is a {@code ServerSocket} but with additional methods
    for application.
    <br><br>
    A server socket waits for requests to come in over the network. It performs
    some operation based on that request, and then possibly returns a result to
    the requester. -Java SE 8 Docs-

    @author     Brendon
    @version    u0r2, 09/09/2017
*/
public class SC_ServerSocket extends ServerSocket implements Runnable {

    private static final Modal MODAL
        = new Modal("ServerSocket", System.out, 3, true);


    // public constants

    /** End of Transmission (EOT) character */
    public static final String EOT = SC_ServerPresenter.EOT;

    /** Amount of data to be buffered in bytes */
    public static final short BUFFER_SIZE;

    static {

        try {

            BUFFER_SIZE = Short.parseShort(
                    ConfigFileReader.read(SC_ServerPresenter.CONFIG, 8));

        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }


    // instance constants

    /** Attached presenter to invoke methods on */
    private final SC_ServerPresenter presenter;

    /** Pool of threads to handle clients on */
    private final ExecutorService threadPool;


    // constructors

    /** Constructs an instance of this class with a given port number and
        Presenter

        @param      port
                    Port number

        @param      presenter
                    Presenter to invoke methods on

        @throws     IOException
                    If an I/O error occurs when opening the socket
    */
    public SC_ServerSocket(int port, SC_ServerPresenter presenter) throws
        IOException {

        super(port);

        MODAL.println(Modal.Mode.INFO,
            "New node at " + getLocalSocketAddress().toString());

        // presenter
        this.presenter = presenter;

        // threadPool
        this.threadPool =
            Executors.newFixedThreadPool(SC_ServerPresenter.MAX_CLIENTS);
    }


    // methods

    /** Listens for clients and handles them appropriately */
    @Override
    public void run() {

        try {

            while (!Thread.currentThread().isInterrupted()) {
                MODAL.println(Modal.Mode.INFO, "Listening for client...");

                final SC_ClientObject newClient = presenter.register(accept());

                if (newClient != null) {

                    threadPool.execute(() -> {
                        runCommunication(newClient);
                    });
                }
            }
        } catch (IOException e) {

            MODAL.println(Modal.Mode.ERROR,
                "...exception caught while listening for client: "
                + e.toString());
        }
        // TODO: implement blocking when > SC_ServerPresenter.MAX_CLIENTS
    }

    /** Closes this {@code SC_ServerSocket} and its handles */
    public void shutdown() {
        MODAL.println(Modal.Mode.ALERT, "Shutting down ServerSocket...");
        threadPool.shutdownNow();

        try {
            close();
            MODAL.println(Modal.Mode.ALERT, "...ServerSocket closed...");
        } catch (IOException e) {
            // dont care
        }
        MODAL.println(Modal.Mode.ALERT, "...ServerSocket shutdown");
    }

    /** Listens for client queries

        @param      client
                    Client from which queries are to be listened
    */
    private void runCommunication(SC_ClientObject client) {

        final Modal modal = new Modal(
                client.getSocket().getRemoteSocketAddress().toString(),
                System.out, 3, true);

        modal.println(Modal.Mode.INFO, "...got client");

        try (
            final BufferedReader reader = client.getReader();
        ) {
            String queryBuffer = new String();

            while (!Thread.currentThread().isInterrupted()
                && !client.getSocket().isClosed()) {

                final String[] queries;
                final int i;
                int bytes;
                char[] buffer = new char[BUFFER_SIZE];

                modal.println(Modal.Mode.INFO, "Listening for query...");

                // fetch data until finds EOT
                do {
                    bytes = reader.read(buffer, 0, buffer.length);

                    if (bytes > 0) {
                        queryBuffer += String.valueOf(buffer, 0, bytes);
                    }
                } while (!queryBuffer.contains(EOT));

                // split to array of queries
                queries = queryBuffer.split(EOT);

                // if the last query is incomplete
                if (!queryBuffer.endsWith(EOT)) {
                    i = queries.length - 1;
                    queryBuffer = queries[queries.length - 1];
                } else {
                    i = queries.length;
                    queryBuffer = new String();
                }

                modal.println(Modal.Mode.DEBUG, "...fetched " + i
                    + " queries at length " + queryBuffer.length());

                // handle every single query buffered
                for (int j = 0; j < i; j++) {
                    presenter.readQuery(client, queries[j].trim());
                }
                modal.println(Modal.Mode.INFO, "...fetched and handled query");
            }
        } catch (Exception e) {

            modal.println(Modal.Mode.ERROR,
                "...exception caught while listening for/handling query: "
                + e.toString());

            presenter.kick(client, "Something went wrong");
        } finally {
            modal.println(Modal.Mode.INFO, "Client EOS");
        }
    }

    /** Sends a query to a client

        @param      address
                    Client to send this query to

        @param      query
                    Query to be sent
    */
    public void sendQuery(final SC_ClientObject address, final String query) {
        final BufferedWriter writer = address.getWriter();

        final String addressString
            = address.getSocket().getRemoteSocketAddress().toString();

        try {
            MODAL.println(Modal.Mode.INFO, "Sending query...");

            MODAL.println(Modal.Mode.DEBUG,
                "...query length: " + query.length() + "...");

            writer.write(query, 0, query.length());
            writer.flush();

            MODAL.println(Modal.Mode.INFO, "...query sent");
        } catch (IOException e) {

            MODAL.println(Modal.Mode.ERROR,
                "...exception caught while sending query: " + e.toString());

            Thread.currentThread().interrupt();
        }
    }
}