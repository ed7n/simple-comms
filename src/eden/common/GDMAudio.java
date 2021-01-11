package eden.common;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/** The {@code GDMAudio} class eliminates the boilerplate required to play an
    {@code AudioInputStream} while providing a handful of relevant features.
    <br><br>
    Use the static methods found in {@code AudioSystem} to aid the construction
    of new instances. It is recommended for the {@code AudioInputStream} to
    support its {@code mark} and {@code reset} methods to make full use of this
    class. This can be achieved by wrapping its underlying {@code InputStream}
    to one that supports these methods, like a {@code BufferedInputStream}.
    <br><br>
    In the event of an {@code IOException}, the instance will be marked as
    {@code broken}, hence disallowing relavant activites. A broken instance must
    still be closed.

    @author     Brendon
    @version    u0r1, 09/09/2017
*/
public class GDMAudio implements Runnable {

    // public constants

    /** Amount of audio data to be buffered in bytes */
    public static final int BUFFER_SIZE = 4096;


    // instance constants

    // added on r1
    /** Audio resource name for reference */
    private final String name;

    /** Audio resource to be read from upon playback */
    private final AudioInputStream stream;

    /** Defines audio parameters */
    private final AudioFormat format;

    /** Temporary medium for data interchange */
    private final byte[] buffer;

    /** Data line to write audio data to */
    private final SourceDataLine line;


    // instance variables

    /** Reference to thread on which playback is being performed */
    private Thread thread;

    /** Whether playback is not paused */
    private boolean running;

    // added on r1 {
    /** To reduce memory leak */
    private int bytes;

    /** Number of bytes read */
    private int elapsed;

    /** Stream size in bytes. This variable is not fixed to handle continuous
        streams
    */
    private int total;

    /** Whether an IOException has occured, {@code true} disallows any relavant
        activites
    */
    private boolean broken;
    // } added on r1


    // constructors

    /** Constructs a new instance of this class with a given format and stream.
        If done right, playback at abnormal speeds can be achieved here

        @param      stream
                    Audio resource to be read from upon playback

        @param      format
                    {@code AudioFormat} defining audio parameters

        @throws     LineUnavailableException
                    If a line can not be opened because it is unavailable

        @throws     IOException
                    If an input or output error occurs
    */
    public GDMAudio(AudioInputStream stream, AudioFormat format)
        throws LineUnavailableException, IOException {

        this("Audio" + stream.hashCode(), stream, format);
    }
    /** Constructs a new instance of this class with a given name, format and
        stream. If done right, playback at abnormal speeds can be achieved here

        @param      name
                    Name of this audio resource

        @param      stream
                    Audio resource to be read from upon playback

        @param      format
                    {@code AudioFormat} defining audio parameters

        @throws     LineUnavailableException
                    If a line can not be opened because it is unavailable

        @throws     IOException
                    If an input or output error occurs
    */
    public GDMAudio(String name, AudioInputStream stream, AudioFormat format)
        throws LineUnavailableException, IOException {

        // name, elapsed, total, broken, format, buffer
        this.name = name;
        this.elapsed = 0;
        this.total = stream.available();
        this.broken = false;
        this.format = format;
        this.buffer = new byte[BUFFER_SIZE];

        // stream
        if (stream.markSupported()) {
            stream.mark(total);
        }
        this.stream = stream;

        // line
        this.line = AudioSystem.getSourceDataLine(format);
        line.open();
    }


    // methods

    /** Plays audio from its underlying {@code AudioInputStream} */
    @Override
    public void run() {

        if (!broken) {

            try {
                thread = Thread.currentThread();
                running = true;

                line.start();

                do {
                    // may be true upon loop, stream may be continuous
                    if (elapsed >= total) {
                        elapsed = 0;
                    }

                    // less expensive
                    while (running && (elapsed < total)) {
                        bytes = stream.read(buffer, 0, buffer.length);

                        if (bytes == -1) {
                            break;
                        }
                        line.write(buffer, 0, bytes);
                        elapsed += bytes;
                    }

                // more expensive
                } while (stream.available() > 0);

                if (running) {
                    line.drain();
                    stop();
                    reset();
                }
            } catch (IOException e) {
                broken = true;

                System.err.println("[GDMAudio]\n  "
                    + name + " caught IOException: " + e.toString());

            } catch (Exception e) {

                System.err.println("[GDMAudio]\n  "
                    + name + " caught exception: " + e.toString());
            }
        }
    }

    /** Pauses playback. Effective only when playback is ongoing */
    public void stop() {
        line.stop();
        thread = null;
        this.running = false;
    }

    /** Resets playback marker to its starting point

        @return     {@code false} if the operation was unsuccessful, in which
                    case the {@code AudioInputStream} either does not
                    support {@code mark} and {@code reset}, or had
                    an {@code IOException};
                    {@code true} otherwise
    */
    public boolean reset() {

        if (!broken) {

            try {
                stream.reset();
                elapsed = 0;
                stream.mark(total);
                return true;
            } catch (IOException e) {
                broken = true;

                System.err.println("[GDMAudio]\n  "
                    + name + " caught IOException: " + e.toString());

                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /** Skips playback marker by an amount of bytes

        @param      bytes
                    Amount of audio data to skip in bytes

        @return     {@code false} if the operation was unsuccessful, in which
                    case the {@code AudioInputStream} had
                    an {@code IOException};
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If {@code bytes < 0}
    */
    public boolean skip(long bytes) {

        if (!broken) {

            if (bytes < 0) {
                throw new IllegalArgumentException("Bad bytes: " + bytes);
            }

            try {
                stream.skip(bytes);
                return true;
            } catch (IOException e) {
                broken = true;

                System.err.println("[GDMAudio]\n  "
                    + name + " caught IOException: " + e.toString());

                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /** Awaits for playback to end, then returns. This awaits its playback
        thread to die, so care must be taken when playback is done on a thread
        pool

        @return     {@code false} if the operation was unsuccessful, in which
                    case playback has ended;
                    {@code true} otherwise
    */
    public boolean await() {

        try {
            if (thread != null) {
                thread.join();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns whether this {@code GDMAudio} is not playing

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    public boolean isFree() {
        return !running;
    }

    /** Returns whether this {@code GDMAudio} is closed

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    public boolean isClosed() {
        return !line.isOpen();
    }

    /** Releases any system resource associated to this {@code GDMAudio}

        @return     {@code false} if the operation was unsuccessful;
                    {@code true} otherwise
    */
    public synchronized boolean close() {

        try {
            line.close();
            stream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // added on r1 {
    /** Returns whether this {@code GDMAudio} had an {@code IOException}

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    public boolean isBroken() {
        return broken;
    }

    /** Returns the name of this {@code GDMAudio}
        @return     name
    */
    public String getName() {
        return name;
    }
    // } added on r1
}