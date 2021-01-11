package eden.simplecomms.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import eden.common.Modal;

/** The {@code SC_ClientSocket} class sends and receives queries between itself
    and a remote server. It is a {@code Socket} but with additional methods for
    application.
    <br><br>
    A socket is an endpoint for communication between two machines.
    -Java SE 8 Docs-

    @author     Brendon
    @version    u0r2, 09/09/2017
*/
public class SC_ClientSocket extends Socket implements Runnable {

    private static final Modal MODAL
        = new Modal("ClientSocket", System.out, -1, true);


    // public constants

    /** End of Transmission (EOT) character */
    public static final String EOT = "\u0004";

    /** Amount of data to be buffered in bytes */
    public static final short BUFFER_SIZE = 256;


    // instance constants

    /** Attached presenter to invoke methods on */
    private final SC_ClientPresenter presenter;

    /** Wraps the {@code InputStream} of this {@code Socket} to read data from
    */
    private final BufferedReader reader;

    /** Wraps the {@code OutputStream} of this {@code Socket} to write data to
    */
    private final BufferedWriter writer;


    // constructors

    /** Constructs an instance of this class with a given host, port number and
        presenter

        @param      host
                    Hostname of the server to bind to

        @param      port
                    Port number of the server to bind to

        @param      presenter
                    Presenter to invoke methods on

        @throws     IOException
                    If an I/O error occurs when opening the socket
    */
    public SC_ClientSocket(String host, int port, SC_ClientPresenter presenter)
        throws IOException {

        super(host, port);

        MODAL.println(Modal.Mode.INFO,
            "New node at " + getLocalSocketAddress().toString());

        // reader
        this.reader = new BufferedReader(
                new InputStreamReader(getInputStream()));

        // writer
        this.writer = new BufferedWriter(
                new OutputStreamWriter(getOutputStream()));

        // presenter
        this.presenter = presenter;
    }


    // methods

    /** Listens for queries and handles them appropriately */
    @Override
    public void run() {

        try {
            String queryBuffer = new String();

            while (!Thread.currentThread().isInterrupted()) {
                final String[] queries;
                final int i;
                int bytes;
                char[] buffer = new char[BUFFER_SIZE];

                MODAL.println(Modal.Mode.INFO, "Listening for query...");

                // fetch data until finds EOT
                do {
                    bytes = reader.read(buffer, 0, BUFFER_SIZE);

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

                MODAL.println(Modal.Mode.DEBUG, "...fetched " + i
                    + " queries at length " + queryBuffer.length());

                // handle every single query buffered
                for (int j = 0; j < i; j++) {
                    presenter.readQuery(queries[j].trim());
                }
                MODAL.println(Modal.Mode.INFO, "...fetched and handled query");
            }
        } catch (IOException e) {

            MODAL.println(Modal.Mode.ERROR,
                "...exception caught while listening for/handling query: "
                + e.toString());

            presenter.kick();
        } finally {
            shutdown();
        }
    }

    /** Closes this {@code SC_ClientSocket} */
    public void shutdown() {
        MODAL.println(Modal.Mode.ALERT, "Shutting down Socket...");

        try {
            close();
            MODAL.println(Modal.Mode.ALERT, "...Socket closed...");
        } catch (IOException e) {
            // dont care
        }
        MODAL.println(Modal.Mode.ALERT, "...Socket shutdown");
    }

    /** Sends a query to the remote server

        @param      query
                    Query to be sent
    */
    public void sendQuery(final String query) {

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