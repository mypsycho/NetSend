package org.mypsycho.netsend;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.mypsycho.swing.app.Application;
import org.mypsycho.swing.app.ApplicationListener;
import org.mypsycho.swing.app.FrameView;
import org.mypsycho.swing.app.SingleFrameApplication;
import org.mypsycho.swing.app.beans.MenuFrame;

/**
 * 
 * @author psycho
 * @version 1.0
 */
public class NetSendApplication extends SingleFrameApplication /* use Workshop */ {

    
    public class AppFrame extends MenuFrame {
        // Must be public to be accessed by reflection
        
        AppFrame() {
            super(NetSendApplication.this);
            setMain(netSend);
            setConsoleVisible(false);
        }
        
        public void send() {
            netSend.sendMessage();
        }
        
        public void clear() {
            netSend.clearHistory();
        }
        
        public void loadAddresses(ActionEvent ae) {
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    netSend.loadAddresses(fc.getSelectedFile());
                } catch (Exception e) {
                    exceptionThrown(Level.INFO, "fileChooser", e.getMessage(), e);
                }
            }
        }
        
    }
    

    protected NetSend netSend = null;

    protected void initialize(String[] args) {
        try {
            File f = new File(NetSend.ADDRESS_FILE);
            if (args.length > 0) {
                f = new File(args[0]);
            }
            if (f.exists()) {
                netSend = new NetSend(f);
            } else {
                exceptionThrown(Level.CONFIG, "initialize", "No file '" + f.getPath() + "'", null);
                netSend = new NetSend();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        

    }
    
    protected void startup() {
        
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new XmlFilter(getProperty("AddressesFilter")));

        netSend.addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("port".equals(evt.getPropertyName())) {
                    updateTitle();
                } else if ("status".equals(evt.getPropertyName())) {
                    setStatus((String) evt.getNewValue());
                }
                
            }
        });

        
        netSend.setDividerLocation(0.33);
        show(new FrameView(this, new AppFrame()));
    }

    protected void ready() {
        updateTitle();
        JMenuBar menuBar = getMainFrame().getJMenuBar();
        if (menuBar == null) {
            System.err.println("aaaa");
            return;
        }
        JMenu sounds = (JMenu) menuBar.getMenu(2).getMenuComponent(2);
        
        ActionListener menuManager = new SoundAction();
        for (int indSound=0; indSound<WavFile.SOUND_LIBRARY.length; indSound++) {
            String name = netSend.getSoundNames().get(WavFile.SOUND_LIBRARY[indSound]);
            if (name == null) {
                name = WavFile.SOUND_LIBRARY[indSound];
            }
            JMenuItem sound = new JMenuItem(name);
            sound.setActionCommand(Integer.toString(indSound));
            sound.addActionListener(menuManager);
            sounds.add(sound);
        }
        sounds.setEnabled(WavFile.SOUND_AVAILABLE);
    }
    

    public void updateTitle() {
        String title = getProperty("view(mainFrame).titleFormat");
        title = MessageFormat.format(title, netSend.getAuthor(),
                // Tech. number : Avoid local interpretation of number
                String.valueOf(netSend.getPort()),
                (netSend.getPort() != NetReceiver.DEFAULT_PORT) ? 1 : 0
        );
        getMainFrame().setTitle(title);
    }

    protected JFileChooser fc = new JFileChooser(".");

    public AppFrame getPagedFrame() {
        return (AppFrame) getMainFrame();
    }


    public void setStatus(String status) {
        getPagedFrame().setStatus(status);
    }


    class SoundAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                JMenuItem soundItem = (JMenuItem) e.getSource();
                WavFile.playSound(Integer.parseInt(soundItem.getActionCommand()));
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        Application app = new NetSendApplication();
        app.addApplicationListener(ApplicationListener.console);
        
        try {
            app.launch(true, args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


} // endclass NetSendFrame