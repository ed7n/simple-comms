package eden.simplecomms.server;

/** This class serves as the entry point to {@code SimpleCommunicationServer}

    @author     Brendon
    @version    u0r2, 09/09/2017
*/
public class SimpleCommunicationServer {

    // static constants

    /** Program name */
    private static final String NAME = "Simple Communication Server";

    /** Program version */
    private static final String VERSION = "u0r2 by Brendon, 09/09/2017";

    /** Program usage */
    private static final String USAGE
        = "java -jar simplecomms-server.jar";


    // constructors

    /** To prevent instantiation of this class */
    private SimpleCommunicationServer() {}


    // static methods

    /** The main method is the entry point to the program

        @param      args
                    Command-line arguments to be passed on execution
    */
    public static void main(String[] args) {
        printSplash(0);

        // makes a presenter, adds its shutdown hook, then run it
        final SC_ServerPresenter presenter = new SC_ServerPresenter();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            presenter.shutdown();
        }));
        presenter.run();
    }


    // helper methods

    /** Prints a splash message onto the standard output stream

        @param      code
                    Defines the message to be printed
    */
    private static void printSplash(int code) {
        final String out;

        switch (code) {

            case 4:
                System.out.println("Can not parse arguments");
            case 3:
                System.out.print("Usage: " + USAGE + "\n\n");
                break;
            case 2:
                out = "Something went wrong";

                System.out.print(
                    getSeperator(out.length()) + "\n" + out + "\n\n");

                break;
            case 1:
                out = "End of " + NAME;

                System.out.print(
                    getSeperator(out.length()) + "\n" + out + "\n\n");

                break;
            case 0:
                out = "Welcome to " + NAME + "!";

                System.out.print("\n\n" + out + "\n"
                    + getSeperator(out.length()) + "\n" + VERSION + "\n\n");
        }
    }

    /** Returns a line of dashes (-) at a specified length

        @param      int
                    Length of line

        @return     Line of dashes
    */
    private static String getSeperator(int length) {
        String out = new String();

        for (int i = 0; i < length; i++) {
            out += '-';
        }
        return out;
    }
}