package eden.common;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;

/** The {@code ConfigFileReader} class reads human-readable data from a file. It
    can read from any file as long as it is formatted in the following manner:
    <br><br><code>
    config.cfg:<br>
    debug:<br>
    false<br>
    :debug<br>
    buffer size:<br>
    131072<br>
    :buffer size<br>
    latency:<br>
    100<br>
    :latency<br>
    :config.cfg<br>
    </code><br><br>
    Where {@code config.cfg} is the filename, entry 0 contains the
    data {@code false}, entry 1 contains the data {@code 131072}, and so on.
    <br><br>
    An entry block must contain a header line ending with a colon (:), data and
    a footer line starting with a colon. Since colons denote entry boundaries,
    care must be taken when an entry data contains them. An escape sequence will
    be implemented in a future release.
    <br><br>
    For readability, indentation to entry headers and footers, and empty lines
    between entry blocks are allowed. An entry data is read as is, any
    identation applied is considered as part of the data. Hence in most cases,
    they should not be indented.
    <br><br>
    As an example in context of the above file:
    <br><br><code>
    ConfigFileReader.read("config.cfg", 1);
    </code><br><br>
    returns {@code 131072} as a {@code String}, where it can later be parsed to
    primitive types.

    @author     Brendon
    @version    u0r0, 08/19/2017
*/
public class ConfigFileReader {

    /** Denotes an entry's start and end. Here for convenience */
    private static final String LEAD = ":";


    /** To prevent instantiation of this class */
    private ConfigFileReader(){}


    /** Reads data from a file at a specified entry index

        @param      filename
                    Path to file to be read from as a {@code String}

        @param      index
                    Index number defining the location of the data

        @return     The read data as a {@code String}

        @throws     IllegalArgumentException
                    If {@code index < 0}

        @throws     IOException
                    If either an error occurs while parsing the file, in which
                    case details are in the message, or an {@code IOException}
                    occurs while reading the file

        @throws     EOFException
                    If the end of file is reached before the specified index
    */
    public static String read(String filename, int index) throws
        IllegalArgumentException, IOException, EOFException {

        // prerequisite
        if (index < 0) {
            throw new IllegalArgumentException("Bad index: " + index);
        }
        int i = -1;

        try (
            final BufferedReader reader
                = new BufferedReader(new FileReader(filename));
        ) {
            String next = reader.readLine();
            // lead in; signature
            if (!next.equals(filename + LEAD)) {
                throw new IOException("Bad signature: " + next);
            }

            // skip to just before index
            while (i < (index - 1)) {

                if (reader.readLine().trim().startsWith(LEAD)) {
                    i++;
                }
            }

            // skip empty lines
            do {
                next = reader.readLine().trim();
            } while (next.isEmpty());


            // entry header
            if (next.endsWith(LEAD)) {
                String out = new String();
                next = reader.readLine();

                // read data until entry footer is reached
                while (!next.trim().startsWith(LEAD)) {
                    out += next;
                    next = reader.readLine();
                }
                return out;

            // lead out
            } else if (next.equals(LEAD + filename)) {
                throw new IOException("Lead out reached after index " + i);
            }
            throw new IOException("Bad header after index " + i);
        } catch (NullPointerException e) {

            throw new EOFException(
                "End of file reached at index " + i);

        } catch (IOException e) {
            throw new IOException(e.toString());
        }
    }
}