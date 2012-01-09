package org.mypsycho.netsend;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * 
 * @author psycho
 * @version 1.0
 */
public class SoundMessage extends JComboBox {
    public  static final char SOUND_TOKEN ='§';

    private String[] sounds;
    protected String previous = "";
    protected NetSend owner;
    public SoundMessage(NetSend p) {
        owner = p;
        
        // Init sounds list
        sounds = new String[WavFile.SOUND_LIBRARY.length + 1];
        sounds[0] = owner.getSoundNames().get("");
        for (int i = 0; i < WavFile.SOUND_LIBRARY.length; i++) {
            sounds[i+1] = owner.getSoundNames().get(WavFile.SOUND_LIBRARY[i]);
            if (sounds[i+1] == null) {
                sounds[i+1] = WavFile.SOUND_LIBRARY[i];
            }
        }
        
        
        setModel(new DefaultComboBoxModel(sounds));
        setEditable(true);
        setSelectedItem(previous);
        
        addActionListener(new MessageBuilder());

    }

    protected class MessageBuilder implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int ind = getSelectedIndex();
            if (ind == -1) { // Return AND Lostfocus !!!
                // parent.sendMessage();
            } else if (ind == 0) {
                setSelectedItem(getMessageBody(previous));
            } else if (ind > 0){
                setSelectedItem(SOUND_TOKEN + String.valueOf(ind-1) + " " +
                                getMessageBody(previous));
            }
        }
    }

    public static int getSoundCode(String source) {
        if ( (source.length()>2) && (source.charAt(0) == SOUND_TOKEN) ) {
            int endSound = source.indexOf(' ');
            String soundCode;
            if (endSound > -1) {
                soundCode = source.substring(1, endSound);
            } else {
                soundCode = source.substring(1);
            }
            try {
                return Integer.parseInt(soundCode);
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static String getMessageBody(String source) {
        int code = getSoundCode(source);
        if (code>=0) {
            return source.substring(1 + String.valueOf(code).length()).trim();
        }
        return source;
    }

    public void setSelectedIndex(int anIndex) {
        previous = ((JTextField) editor.getEditorComponent()).getText();
        super.setSelectedIndex(anIndex);
    }

} // class SoundMessage