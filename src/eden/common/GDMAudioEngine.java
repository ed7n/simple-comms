package eden.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

/** The {@code GDMAudioEngine} class is a simple audio engine that provides the
    ability to play multiple sampled audio simultaneously with ease.
    <br><br>
    A channel contains an audio resource from which it is mapped. Once loaded,
    it can be called for playback on demand. In an event where its audio
    resource is no longer needed, it can be unloaded for later reuse.
    <br><br>
    The {@code load} methods map an audio resource onto any available channel
    and returns its identification for later reference. Custom mapping may be a
    feature to be considered in a future release.
    <br><br>
    If the host either does not have or can not provide audio services, all
    audio resources map to a null channel ({@code -1}) . Invoking methods on
    this channel does nothing.
    <br><br>
    For mutual conclusions, it is suggested to invoke the {@code unloadAll}
    method before program shutdown.

    @author     Brendon
    @version    u0r2, 09/09/2017
*/
public class GDMAudioEngine {

    // public constants

    // added in r2
    /** Null channel number */
    public static final byte NULL_CHANNEL = -1;

    // instance constants

    /** An array of audio resources as channels */
    private final GDMAudio[] channels;

    // added in r1
    /** A dummy array of futures */
    private final Future[] futures;

    /** Threads to play audio on */
    private final ThreadPoolExecutor pool;


    // constructors

    /** Constructs an instance of this class with a given number of channels and
        buffer size

        @param      channels
                    Number of audio channels to be made available
    */
    public GDMAudioEngine(int channels) {

        // channels, futures
        this.channels = new GDMAudio[channels];
        this.futures = new Future[channels];

        // pool
        this.pool = (ThreadPoolExecutor)
            Executors.newCachedThreadPool(GDMThreadFactory.getInstance());
    }


    // methods

    /** Loads an audio file whose path is specified by a {@code String}

        @param      filepath
                    Path to file to be loaded

        @param      name
                    Optional name describing this audio resource

        @return     Channel number to which this audio resource is mapped

        @throws     IOException
                    If an I/O exception occurs

        @throws     UnsupportedAudioFileException
                    If the audio resource does not contain valid data of a
                    recognized file type and format

        @throws     IllegalStateException
                    If there are no free channels available for use

        @throws     LineUnavailableException
                    If a line can not be opened because it is unavailable
    */
    public int load(String filepath, String... name) throws
        IOException,
        UnsupportedAudioFileException,
        IllegalStateException,
        LineUnavailableException
    {
        if ((name.length > 0) && (name[0] != null)) {
            return load(new File(filepath), name[0]);
        }
        return load(new File(filepath));
    }
    /** Loads an audio file

        @param      file
                    File to be loaded

        @param      name
                    Optional name describing this audio resource

        @return     Channel number to which this audio resource is mapped

        @throws     IOException
                    If an I/O exception occurs

        @throws     UnsupportedAudioFileException
                    If the audio resource does not contain valid data of a
                    recognized file type and format

        @throws     IllegalStateException
                    If there are no free channels available for use

        @throws     LineUnavailableException
                    If a line can not be opened because it is unavailable
    */
    public int load(File file, String... name) throws
        IOException,
        UnsupportedAudioFileException,
        IllegalStateException,
        LineUnavailableException
    {
        if ((name.length > 0) && (name[0] != null)) {

            return makeChannel(AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new FileInputStream(file))),
                AudioSystem.getAudioFileFormat(file).getFormat(), name[0]);
        }
        return makeChannel(AudioSystem.getAudioInputStream(
                new BufferedInputStream(new FileInputStream(file))),
            AudioSystem.getAudioFileFormat(file).getFormat());
    }
    /** Loads an audio resource whose path is specified by a URL

        @param      url
                    URL to audio resource to be loaded

        @param      name
                    Optional name describing this audio resource

        @return     Channel number to which this audio resource is mapped

        @throws     IOException
                    If an I/O exception occurs

        @throws     UnsupportedAudioFileException
                    If the audio resource does not contain valid data of a
                    recognized file type and format

        @throws     IllegalStateException
                    If there are no free channels available for use

        @throws     LineUnavailableException
                    If a line can not be opened because it is unavailable
    */
    public int load(URL url, String... name) throws
        IOException,
        UnsupportedAudioFileException,
        IllegalStateException,
        LineUnavailableException
    {
        if ((name.length > 0) && (name[0] != null)) {

            return makeChannel(AudioSystem.getAudioInputStream(
                    new BufferedInputStream(url.openStream())),
                AudioSystem.getAudioFileFormat(url).getFormat(), name[0]);
        }
        return makeChannel(AudioSystem.getAudioInputStream(
                new BufferedInputStream(url.openStream())),
            AudioSystem.getAudioFileFormat(url).getFormat());
    }
    /** Loads audio data from an {@code InputStream}. This allows for continuous
        playback as long as the {@code InputStream} is open and/or has data

        @param      stream
                    {@code InputStream} containing audio data to be loaded

        @param      name
                    Optional name describing this audio resource

        @return     Channel number to which this audio resource is mapped

        @throws     IOException
                    If an I/O exception occurs

        @throws     UnsupportedAudioFileException
                    If the audio resource does not contain valid data of a
                    recognized file type and format

        @throws     IllegalStateException
                    If there are no free channels available for use

        @throws     LineUnavailableException
                    If a line can not be opened because it is unavailable
    */
    public int load(InputStream stream, String... name) throws
        IOException,
        UnsupportedAudioFileException,
        IllegalStateException,
        LineUnavailableException
    {
        if ((name.length > 0) && (name[0] != null)) {

            return makeChannel(AudioSystem.getAudioInputStream(
                    new BufferedInputStream(stream)),
                AudioSystem.getAudioFileFormat(stream).getFormat(), name[0]);
        }
        return makeChannel(AudioSystem.getAudioInputStream(
                new BufferedInputStream(stream)),
            AudioSystem.getAudioFileFormat(stream).getFormat());
    }

    /** Calls an audio channel for playback on a background thread. If the
        specified channel is busy this method does nothing

        @param      name
                    Channel name to be called

        @return     {@code false} if the operation was not commenced;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel name is invalid
    */
    public boolean play(String name) throws IllegalArgumentException {
        return play(getChannel(name));
    }
    /** Calls an audio channel for playback on a background thread. If the
        specified channel is busy this method does nothing

        @param      channel
                    Channel number to be called

        @return     {@code false} if the operation was not commenced;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel number is invalid
    */
    public boolean play(int channel) throws IllegalArgumentException {

        if (channel == -1) {
            return true;
        } else if (!isValidChannel(channel)) {
            throw new IllegalArgumentException("Bad channel: " + channel);
        }

        if (channels[channel].isFree()) {
            futures[channel] = pool.submit(channels[channel]);
            return true;
        }
        return false;
    }

    /** Calls an audio channel for playback on the current thread. If the
        specified channel is busy, this method does nothing

        @param      name
                    Channel name to be called

        @return     {@code false} if the operation was not commenced;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel name is invalid
    */
    public boolean playAndAwait(String name) throws IllegalArgumentException {
        return playAndAwait(getChannel(name));
    }
    /** Calls an audio channel for playback on the current thread. If the
        specified channel is busy, this method does nothing

        @param      channel
                    Channel number to be called

        @return     {@code false} if the operation was not commenced;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel number is invalid
    */
    public boolean playAndAwait(int channel) throws IllegalArgumentException {

        if (channel == -1) {
            return true;
        } else if (!isValidChannel(channel)) {
            throw new IllegalArgumentException("Bad channel: " + channel);
        }

        if (channels[channel].isFree()) {
            channels[channel].run();
            return true;
        }
        return false;
    }

    /** Awaits for a channel to end playback, then returns

        @param      name
                    Channel name to await for

        @return     {@code false} if the operation was unsuccessful;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel name is invalid
    */
    public boolean await(String name) throws IllegalArgumentException {
        return await(getChannel(name));
    }
    /** Awaits for a channel to end playback, then returns

        @param      channel
                    Channel number to await for

        @return     {@code false} if the operation was unsuccessful;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel number is invalid
    */
    public boolean await(int channel) throws IllegalArgumentException {

        /*  r1: in r0, if playback is handled by a thread in pool, this method
            does not return as such threads do not die after execution. This has
            been fixed with the use of Future
        */

        if (channel == -1) {
            return true;
        } else if (!isValidChannel(channel)) {
            throw new IllegalArgumentException("Bad channel: " + channel);
        }

        try {
            futures[channel].get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Pauses a channel playback. Effective only when its playback is ongoing

        @param      name
                    Channel name whose playback is to be paused

        @throws     IllegalArgumentException
                    If the channel name is invalid
    */
    public void stop(String name) throws IllegalArgumentException {
        stop(getChannel(name));
    }
    /** Pauses a channel playback. Effective only when its playback is ongoing

        @param      channel
                    Channel number whose playback is to be paused

        @throws     IllegalArgumentException
                    If the channel number is invalid
    */
    public void stop(int channel) throws IllegalArgumentException {

        if (channel == -1) {
            return;
        } else if (!isValidChannel(channel)) {
            throw new IllegalArgumentException("Bad channel: " + channel);
        }
        channels[channel].stop();
    }

    /** Unloads an audio channel. This releases any resource associated to the
        channel

        @param      name
                    Channel name to be unloaded

        @return     {@code false} if the operation was partially successful;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel name is invalid
    */
    public boolean unload(String name) throws IllegalArgumentException {
        return unload(getChannel(name));
    }
    /** Unloads an audio channel. This releases any resource associated to the
        channel

        @param      channel
                    Channel number to be unloaded

        @return     {@code false} if the operation was partially successful;
                    {@code true} otherwise

        @throws     IllegalArgumentException
                    If the channel number is invalid
    */
    public boolean unload(int channel) throws IllegalArgumentException {

        if (channel == -1) {
            return true;
        } else if (!isValidChannel(channel)) {
            throw new IllegalArgumentException("Bad channel: " + channel);
        }
        return channels[channel].close();
    }

    /** Unloads all audio channels. This releases any resource associated to all
        previously loaded channels

        @return     {@code false} if the operation was partially successful;
                    {@code true} otherwise
    */
    public boolean unloadAll() {
        boolean out = true;

        for (GDMAudio o : channels) {

            if (o != null) {

                if (!o.close()) {
                    out = false;
                }
            }
        }
        return out;
    }


    // helper methods

    /** Constructs a {@code GDMAudio} on a free channel

        @param      format
                    {@code AudioFormat} defining audio parameters

        @param      stream
                    Audio resource to be loaded

        @return     Channel number to which this audio resource is mapped

        @throws     IllegalStateException
                    If there are no free channels available for use

        @throws     IOException
                    If an input or output error occurs

        @throws     LineUnavailableException
                    If a line can not be opened because it is unavailable
    */
    private int makeChannel(AudioInputStream stream, AudioFormat format,
        String... name) throws
        IllegalStateException,
        IOException,
        LineUnavailableException
    {
        final int i = getFreeChannel();

        if (i < 0) {
            throw new IllegalStateException("No free channels.");
        }

        try {

            if ((name.length > 0) && (name[0] != null)) {
                channels[i] = new GDMAudio(name[0] , stream, format);
            } else {
                channels[i] = new GDMAudio(stream, format);
            }
            return i;
        } catch (IllegalArgumentException e) {
            channels[i] = null;
            return -1;
        }
    }

    /** Returns whether there is/are (a) free channel(s) available for use

        @return     -1 if the condition is false;
                    otherwise returns a free channel number
    */
    private int getFreeChannel() {

        for (int i = 0; i < channels.length; i++) {

            if (channels[i] == null || channels[i].isClosed()) {
                return i;
            }
        }
        return -1;
    }

    /** Returns whether a channel number is valid

        @param      channel
                    Channel number to check the condition with

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    private boolean isValidChannel(int channel) {

        return (channel >= 0)
            && (channel < channels.length)
            && (channels[channel] != null);
    }

    // added in r2
    /** Returns a channel number by its name

        @param      name
                    Channel name whose number is to be returned

        @return     Channel number under this name

        @throws     IllegalArgumentException
                    If there is no channel of this name
    */
    private int getChannel(String name) throws IllegalArgumentException {


        for (int i = 0; i < channels.length; i++) {

            if (channels[i].getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Bad name: " + name);
    }


    // static methods

    /** Optionally prints supported file types and available mixers, then
        returns whether audio services may be supported

        @param      verbose
                    Whether audio services information should be printed

        @return     {@code true} if the condition is true;
                    {@code false} otherwise
    */
    public static boolean checkHost(boolean verbose) {

        // types
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();

        if (verbose) {
            System.out.println("[GDMAudioEngine]\n  Supported file types:");

            if (types.length > 0) {
                System.out.print("   ");

                for (AudioFileFormat.Type t : types) {
                    System.out.print(" " + t.toString());
                }
            } else {
                System.out.println("    NONE");
            }
            System.out.println();
        }

        // mixers
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        if (verbose) {
            System.out.println("  Available mixers:");

            if (mixers.length > 0) {

                for (Mixer.Info m : mixers) {
                    System.out.println("    " + m.toString());
                }
            } else {
                System.out.println("    NONE");
            }
            System.out.println();
        }

        if ((types.length <= 0) || (mixers.length <= 0)) {

            System.out.println("[GDMAudioEngine]\n  "
                + "Audio support may be missing!");

            return false;
        }
        return true;
    }


    // helper classes

    /** A {@code GDMThreadFactory} makes new daemon threads designed for use in
        {@code GDMAudioEngine}
    */
    private static class GDMThreadFactory implements ThreadFactory {

        /** For indentification of individual threads */
        private static byte nextChannel = 0;

        /** Sole instance of this class */
        private static final GDMThreadFactory instance = new GDMThreadFactory();


        /** To prevent instantiation of this class */
        private GDMThreadFactory(){}


        /** Makes a new daemon thread

            @param      r
                        {@code Runnable} to be executed by this thread
        */
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("GDMChannel-" + nextChannel++);
            t.setDaemon(true);
            return t;
        }

        /** Returns the sole instance of this class
            @return     {@code GDMThreadFactory} instance
        */
        private static GDMThreadFactory getInstance() {
            return instance;
        }
    }
}