package org.mypsycho.netsend;

// standard packages
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

// third party packages

// project packages

/**
 * 
 * @author psycho
 * @version 1.0
 */
public class WavFile {


    public static final String[] SOUND_LIBRARY = {
          "DRUMROLL.WAV", "EXPLODE.WAV", "LASER.WAV", "oh_no2.WAV",  "TADA.WAV"
    };

    public static final boolean SOUND_AVAILABLE;
    static {
        System.out.println("test sound");
        boolean sound = true;
        if (AudioSystem.getMixerInfo().length == 0) {
            SOUND_AVAILABLE = false;
        } else try { // Pas le meilleur moyen de tester le son
            AudioInputStream stream = AudioSystem.getAudioInputStream(
                        WavFile.class.getResource(NetSend.AUDIO_PATH + SOUND_LIBRARY[0]));
            Line.Info info = new DataLine.Info(Clip.class, stream.getFormat());
            sound = AudioSystem.isLineSupported(info);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
            sound = false;
        } finally {
            System.out.println("Sound: " + sound);
            SOUND_AVAILABLE = sound;
        }
    }


    public static void playSound(int code) throws ArrayIndexOutOfBoundsException,
            UnsupportedAudioFileException {
        if (SOUND_AVAILABLE) {
            if ((code >= 0) && (code < SOUND_LIBRARY.length))
                playSound(WavFile.class.getResource(NetSend.AUDIO_PATH+SOUND_LIBRARY[code]));
            else
                throw new ArrayIndexOutOfBoundsException("No sound " + code + " in WavFile library");
        }
    }

    public static void playSound(URL pURL) throws UnsupportedAudioFileException {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(pURL);
            Line.Info info = new DataLine.Info(Clip.class, stream.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.addLineListener(lineCleaner);
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw (UnsupportedAudioFileException)
                    new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        }
    }

    static LineListener lineCleaner = new LineListener() {
        public void update(LineEvent le) {
            Line line = le.getLine();
            if (le.getType() == LineEvent.Type.STOP) {
                line.close();
            }
        }
    };

} // class WavFile