package eden.common;

import java.io.PrintStream;

/** The {@code Modal} class is a partial frontend to {@code PrintStream}
    providing the ability to print tagged messages.

    @author     Brendon
    @version    u0r5, 08/22/2017
*/
public class Modal {

    // public constants

    /** The default {@code PrintStream} to be passed at construction */
    public static final PrintStream DEFAULT_PSTR = System.out;


    // static variables

    /** Used to uniquely identify {@code Modals} */
    private static short nextID = 10000;


    // instance constants

    /** Unique {@code Modal} ID */
    private final short ID;

    /** {@code Modal} node name */
    private final String node;

    /** {@code PrintStream} to invoke methods on */
    private final PrintStream pStr;

    /** Determines what message modes are filtered out */
    private final byte verbosity;

    /** Prefix setting determines symbol use */
    private final boolean useSymbol;


    // constructors

    /** Constructs an instance of this class with the default node
        name, {@code PrintStream} ({@code System.out}), verbosity (2) and prefix
        setting ({@code false})

        @throws     IllegalArgumentException
                    If the node name is less than 6 characters of length
    */
    public Modal() throws IllegalArgumentException {
        this("Modal$" + nextID, DEFAULT_PSTR, 2, false);
    }
    /** Constructs an instance of this class with a given {@code PrintStream},
        the default node name, verbosity (2) and prefix setting ({@code false})

        @param      ps
                    {@code PrintStream} to be passed

        @throws     IllegalArgumentException
                    If the node name is less than 6 characters of length
    */
    public Modal(PrintStream ps) throws IllegalArgumentException {
        this("Modal$" + nextID, ps, 2, false);
    }
    /** Constructs an instance of this class with a given node
        name, {@code PrintStream}, verbosity and prefix setting

        @param      node
                    Node name

        @param      ps
                    {@code PrintStream} to be passed

        @param      verbosity
                    Verbosity

        @param      useSymbol
                    Prefix setting

        @throws     IllegalArgumentException
                    If the node name is less than 6 characters of length
    */
    public Modal(String node, PrintStream ps, int verbosity,
        boolean useSymbol) throws IllegalArgumentException {

        // ID
        if (nextID == Short.MAX_VALUE) {

            printSelf(Mode.ERROR, "Too many Modal instances! Expecting "
                + "exception...");
        }
        ID = nextID++;

        // pStr
        if (ps == null) {

            printSelf(Mode.ERROR, "Null PrintStream passed! Constructing with "
                + "the default PrintStream");

            ps = DEFAULT_PSTR;
        }
        pStr = ps;

        // node
        // added prerequisite on r2
        if (node.length() >= 6) {

            if (node.substring(0, 6).equalsIgnoreCase("Modal$")) {

                // added on r1
                if (Long.parseLong(node.substring(6)) != ID)

                    printSelf(Mode.ERROR, "Illegal node! Setting to Modal$"
                        + ID);

                node = "Modal$" + ID;
            }
        } else {
            throw new IllegalArgumentException("Node name too short");
        }
        this.node = node;

        // verbosity
        if (verbosity > Mode.MODES) {

            printSelf(Mode.ERROR, "Invalid verbosity! Setting to "
                + Mode.MODES);

            verbosity = Mode.MODES;
        }
        this.verbosity = (byte) verbosity;

        // useSymbol
        this.useSymbol = useSymbol;
    }


    // methods

    /** Prints a {@code Modal} message. This can only be given by the Modal
        alone

        @param      msg
                    {@code String} message to be printed
    */
    private void printSelf(String msg) {
        pStr.println("[" + this.getClass().getName() + "] " + msg);
    }
    /** Prints a prefixed {@code Modal} message. This can only be given by this
        {@code Modal} alone

        @param      mode
                    Message {@code Mode}, determines verbosity level

        @param      msg
                    {@code String} message to be printed
    */
    private void printSelf(Mode mode, String msg) {
        pStr.println("[" + this.getClass().getName() + "/" + mode.prefix + "] "
            + msg);
    }

    /** Prints this {@code Modal} */
    public void print() {
        pStr.print(this);
    }
    // added on r1 {
    /** Prints a {@code boolean}

        @param      b
                    {@code boolean} to be printed
    */
    public void print(boolean b) {
        print(Boolean.toString(b));
    }
    /** Prints a {@code byte}

        @param      b
                    {@code byte} to be printed
    */
    public void print(byte b) {
        print(Byte.toString(b));
    }
    /** Prints a {@code char}

        @param      c
                    {@code char} to be printed
    */
    public void print(char c) {
        print(Character.toString(c));
    }
    /** Prints a {@code short}

        @param      s
                    {@code short} to be printed
    */
    public void print(short s) {
        print(Short.toString(s));
    }
    /** Prints an {@code int}

        @param      i
                    {@code int} to be printed
    */
    public void print(int i) {
        print(Integer.toString(i));
    }
    /** Prints a {@code long}

        @param      l
                    {@code long} to be printed
    */
    public void print(long l) {
        print(Long.toString(l));
    }
    /** Prints a {@code float}

        @param      f
                    {@code float} to be printed
    */
    public void print(float f) {
        print(Float.toString(f));
    }
    /** Prints a {@code double}

        @param      d
                    {@code double} to be printed
    */
    public void print(double d) {
        print(Double.toString(d));
    }
    // } added on r1
    /** Prints a message

        @param      msg
                    {@code String} message to be printed
    */
    public void print(String msg) {
        pStr.print("[" + node + "] " + msg);
    }
    /** Prints a prefixed message

        @param      mode
                    Message {@code Mode}, determines verbosity level

        @param      msg
                    {@code String} message to be printed
    */
    public void print(Mode mode, String msg) {

        if (verbosity >= mode.level) {

            if (useSymbol) {
                pStr.print("[" + node + "/" + mode.symbol + "] " + msg);
            } else {
                pStr.print("[" + node + "/" + mode.prefix + "] " + msg);
            }
        }
    }

    /** Prints an empty line */
    public void println() {
        pStr.println();
    }
    // added on r1 {
    /** Prints a {@code boolean} line

        @param      b
                    {@code boolean} to be printed
    */
    public void println(boolean b) {
        println(Boolean.toString(b));
    }
    /** Prints a {@code byte} line

        @param      b
                    {@code byte} to be printed
    */
    public void println(byte b) {
        println(Byte.toString(b));
    }
    /** Prints a {@code char} line

        @param      c
                    {@code char} to be printed
    */
    public void println(char c) {
        println(Character.toString(c));
    }
    /** Prints a {@code short} line

        @param      s
                    {@code short} to be printed
    */
    public void println(short s) {
        println(Short.toString(s));
    }
    /** Prints an {@code int} line

        @param      i
                    {@code int} to be printed
    */
    public void println(int i) {
        println(Integer.toString(i));
    }
    /** Prints a {@code long} line

        @param      l
                    {@code long} to be printed
    */
    public void println(long l) {
        println(Long.toString(l));
    }
    /** Prints a {@code float} line

        @param      f
                    {@code float} to be printed
    */
    public void println(float f) {
        println(Float.toString(f));
    }
    /** Prints a {@code double} line

        @param      d
                    {@code double} to be printed
    */
    public void println(double d) {
        println(Double.toString(d));
    }
    // } added on r1
    /** Prints a message line

        @param      msg
                    {@code String} message to be printed
    */
    public void println(String msg) {
        print(msg + "\n");
    }
    /** Prints a prefixed message followed by a carriage return or line feed

        @param      mode
                    Message {@code Mode}, determines verbosity level

        @param      msg
                    {@code String} message to be printed
    */
    public void println(Mode mode, String msg) {
        print(mode, msg + "\n");
    }

    /** Prints a headerless message

        @param      msg
                    {@code String} message to be printed
    */
    public void printCln(String msg) {
        pStr.print(msg);
    }
    /** Prints a headerless prefixed message

        @param      mode
                    Message {@code Mode}, determines verbosity level

        @param      msg
                    {@code String} message to be printed
    */
    public void printCln(Mode mode, String msg) {

        if (verbosity >= mode.level) {
            pStr.print(msg);
        }
    }

    /** Returns the unique ID of this {@code Modal}
        @return     ID
    */
    public short getID() {
        return ID;
    }

    /** Returns the node name of this {@code Modal}
        @return     Node name
    */
    public String getNode() {
        return node;
    }

    /** Returns the {@code PrintStream} of this {@code Modal}
        @return     {@code PrintStream}
    */
    public PrintStream getPrintStream() {
        return pStr;
    }

    /** Returns the verbosity of this {@code Modal}
        @return     Verbosity
    */
    public int getVerbosity() {
        return (int) verbosity;
    }

    /** Returns the prefix setting of this {@code Modal}
        @return     prefix setting
    */
    public boolean getUseSymbol() {
        return useSymbol;
    }

    /** Compares this and another {@code Modal} by their attributes

        @param      m
                    {@code Modal} to compare between

        @return     {@code true} if they are equal;
                    {@code false} if m is null or otherwise
    */
    public boolean equals(Modal m) {

        if (m != null) {

            if (!this.node.equals(m.node)
                || !this.pStr.equals(m.pStr)
                || (this.verbosity != m.verbosity)
                || (this.useSymbol != m.useSymbol)) {

                return false;
            }
            return true;
        }
        return false;
    }


    // helper classes

    /** Defines message modes. A {@code Mode} has a word and symbol prefixes and
        a verbosity level.
    */
    public enum Mode {

        PROMPT("PROMPT", '?', 0),
        INFO("INFO", 'i', 0),   // added on r1
        ALERT("ALERT", '!', 1),
        ERROR("ERROR", 'X', 2),
        DEBUG("DEBUG", '$', 3);

        /** Maximum verbosity level */
        public static final byte MODES = 4;


        /** Word prefix */
        public final String prefix;

        /** Symbol prefix */
        public final char symbol;

        /** Verbosity level */
        public final byte level;


        /** Constructs a constant of this enum with a given word and symbol
            prefixes and verbosity level

            @param      prefix
                        Word prefix

            @param      symbol
                        Symbol prefix

            @param      level
                        Verbosity level
        */
        Mode(String prefix, char symbol, int level) {
            this.prefix = prefix;
            this.symbol = symbol;
            this.level = (byte) level;
        }
    }
}


/*
r3  docm: rewrote class description
    docm: realigned

r5  code: protected and package-private -> public
    code: removed EOF signature
    docm: now uses @code tags
    docm: reformatted
    docm: corrected some methods
    impl: equals() now returns false under one long conditional
    code: moved Mode to bottom
    code: divided code sections (constructors, methods, etc)
*/