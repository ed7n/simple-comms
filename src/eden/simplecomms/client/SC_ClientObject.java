package eden.simplecomms.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/** A {@code SC_ClientObject} holds client information, socket the server is
    bound to, and its underlying I/O streams.

    @author     Brendon
    @version    u0r2, 09/09/2017
*/
public class SC_ClientObject {

    // instance constants

    /** Socket the server is bound to */
    private final Socket socket;

    /** Wrapped {@code InputStream} to read data from */
    private final BufferedReader reader;

    /** Wrapped {@code OutputStream} to write data to */
    private final BufferedWriter writer;


    // instance variables

    /** Client's name for identification */
    private String name;

    /** Name of the destination address to which messages from this client will
        be sent
    */
    private String address;

    // added in r2 {
    /** Epoch time the last query was sent */
    private long lastTime;

    /** Number of spam messages sent */
    private short spams;
    // } added in r2


    // constructors

    /** Constructs an instance of this class with a given {@code Socket} and the
        default name and destination address

        @param      socket
                    {@code Socket} to be passed

        @throws     IOException
                    If an I/O error occurs when creating the I/O streams or if
                    the socket is not connected
    */
    public SC_ClientObject(Socket socket) throws IOException {
        this(socket, "Client" + socket.hashCode(), "Server");
    }
    /** Constructs an instance of this class with a given {@code Socket}, name
        and destination address

        @param      socket
                    {@code Socket} to be passed

        @param      name
                    Client's name for identification

        @param      address
                    Name of the destination address to which messages from this
                    client will be sent

        @throws     IOException
                    If an I/O error occurs when creating the I/O streams or if
                    the socket is not connected
    */
    public SC_ClientObject(Socket socket, String name, String address) throws
        IOException {

        // socket
        this.socket = socket;

        // reader
        this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        // writer
        this.writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));

        // name, address, lastTime, spams
        this.name = name;
        this.address = address;
        this.lastTime = 0;
        this.spams = 0;
    }


    // methods

    /** Returns the {@code Socket} of this client
        @return     {@code Socket}
    */
    public Socket getSocket() {
        return socket;
    }

    /** Returns the {@code BufferedReader} of this client to read data from
        @return     {@code BufferedReader}
    */
    public BufferedReader getReader() {
        return reader;
    }

    /** Returns the {@code BufferedWriter} of this client to write data to
        @return     {@code BufferedWriter}
    */
    public BufferedWriter getWriter() {
        return writer;
    }

    /** Returns the name of this client
        @return     name
    */
    public String getName() {
        return name;
    }

    /** Sets the name of this client

        @param      name
                    New name
    */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the destination address of this client
        @return     address
    */
    public String getAddress() {
        return address;
    }

    /** Sets the destination address of this client

        @param      address
                    New address
    */
    public void setAddress(String address) {
        this.address = address;
    }

    /** Returns the epoch time the last query was sent
        @return     lastTime
    */
    public long getLastTime() {
        return lastTime;
    }

    /** Sets the epoch time the last query was sent

        @param      lastTime
                    New lastTime
    */
    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    /** Returns the number of spam messages sent
        @return     spams
    */
    public short getSpams() {
        return spams;
    }

    /** Sets the number of spam messages sent

        @param      spams
                    New spams
    */
    public void setSpams(int spams) {
        this.spams = (short) spams;
    }
}