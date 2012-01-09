package org.mypsycho.netsend;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.mypsycho.beans.Injectable;
import org.mypsycho.beans.InjectionContext;
import org.mypsycho.swing.app.utils.SwingHelper;


/**
 * 
 * @author psycho
 * @version 1.0
 */
public class NetSend extends JSplitPane implements Injectable {

    public static String IMAGE_PATH = "image/";
    public static String AUDIO_PATH = "audio/";

    protected String[] hostIdentifications;
    protected String hostAddress;

    /*
    public static void displayError(Component comp, String error) {
        JOptionPane.showMessageDialog(comp, error, getLanguage().getString("netsend.ErrTitle"),
                JOptionPane.ERROR_MESSAGE);
    }
*/

    // protected SoundMessage message = new SoundMessage(this);
    protected JTextField message = new JTextField(30);


    public static final String ADDRESS_FILE = "NetSend.xml"; // File in working directory


    protected String author = System.getProperty("user.name", "unknown");
    protected final JTextField others = new JTextField();
    protected final JButton send = new JButton();
    // protected JScrollPane borderMessagesRecord = null;
    protected final MessagesModel messagesRecord = new MessagesModel();
    /*
       messages records to jtree : action 
         message -> Remove, Reply, ReplyAll
         user    -> Add to destinary, Select 
     */
    
    protected AddressTree addresses = null;
    protected String absents = null; // Must not be null

    protected NetReceiver receiver = new NetReceiver(this);

    private Map<String, String> texts = new HashMap<String, String>();
    private Map<String, String> soundNames = new HashMap<String, String>();
    

    /**
     * Build a NetSend object without registration
     * @throws IOException
     */
    public NetSend(Integer port) throws IOException {
        super(JSplitPane.HORIZONTAL_SPLIT, true);
        
        InetAddress host = InetAddress.getLocalHost();
        hostIdentifications = new String[] {
                host.getCanonicalHostName(), host.getHostName(), host.getHostAddress() 
        };
        hostAddress = hostIdentifications[0];
        
        init();
        
        // Set on default port
        if (port != null) {
            connect(port);
        }
    }

    public NetSend() throws IOException {
        this(NetReceiver.DEFAULT_PORT);
    }

    public NetSend(File file) throws IOException {
        this((Integer) null);
        
        loadAddresses(file);
        if (addresses.getPort() == AddressTree.NO_PORT) {
            connect(NetReceiver.DEFAULT_PORT);
        } else { 
            connect(addresses.getPort());
        }
        firePropertyChange("port", null, addresses.getPort());
    }
    

    public void initResources(InjectionContext context) {
        texts.putAll(context.getRootContext());
    }

        
    
    public boolean isConnected() {
        return (receiver.getPort() != 0);
    }
    
    public void connect(int port) throws IOException {
        receiver.setPort(port);
        hostAddress = hostIdentifications[0] + ":" + receiver.getPort();
    }
    
    protected void init() {


        SwingHelper helper = new SwingHelper(this);
        helper.with(RIGHT, new BorderLayout(0, 0)) // message editor
            .with("editor", new BorderLayout(3, 6), BorderLayout.PAGE_START)
        // component(right)(editor).border empty(3, 3, 3, 3)
                .with("labels", new GridLayout(0, 1, 0, 3), BorderLayout.LINE_START)
                    .add("message", new JLabel("", JLabel.TRAILING))
                    .add("others", new JLabel("", JLabel.TRAILING))
                .back()
                .with("fields", new GridLayout(0, 1, 0, 3), BorderLayout.CENTER)
                    .add("message", message) // a:sendMessage
                    .add("others", others) // tooltip, a:sendMessage
                .back()
                .add("button", send, BorderLayout.LINE_END) // ToolTipText, a:sendMessage
                .add("history", new JLabel(), BorderLayout.PAGE_END)
            .back()
            .add("history", new JScrollPane(new MessagesTree(this, messagesRecord),
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        final JScrollPane messagesSp = helper.get("history");
        // Manager for scroll down
        messagesSp.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (scrollLastMessage) {
                    JScrollBar bar = messagesSp.getVerticalScrollBar();
                    bar.setValue(bar.getMaximum());
                    scrollLastMessage = false;
                }   
            }
        });
        
        
    }
    protected boolean scrollLastMessage = false; // Differed order for scroll down
    public void showLastMessage() {
        // Differed order as table height computation is differed
        scrollLastMessage = true; 
    }

    public void setStatus(String status) {
        firePropertyChange("status", null, getText(status)); 
    }

    
    
    /**
     * Read a file and register on the specified port
     * 
     * @param fileName
     * @param defaultPortOnError Use the default port if a error is per
     * @throws RuntimeException
     **/
    public void loadAddresses(File file) throws IOException {
        addresses = new AddressTree(this, file);
        if (addresses.getAuthor().length() > 0) {
            author = addresses.getAuthor();
        }       
        
        JScrollPane addressView = new JScrollPane(addresses);
        /*
        Border space = BorderFactory.createEmptyBorder(3, 3, 3, 0);
        if (addressView.getBorder() != null) {
        	space = BorderFactory.createCompoundBorder(space, addressView.getBorder());
        } 
        addressView.setBorder(space);
        */
        setLeftComponent(addressView);
        setDividerLocation(0.33);

        firePropertyChange("port", null, addresses.getPort());
    }


    public int getPort() { 
        return receiver.getPort(); 
    }


    public void clearHistory() {
        messagesRecord.clearMessages();
        // Not need to scroll down: Nothing to display 
    }

    public void setContent(String content) { 
        message.setText(content); 
    }
    
    public void sendMessage() {
        String label = message.getText();
        if (label.trim().length() <= 0){
            setStatus("ErrNoMessage");
            return;
        }

        /* NO MORE SOUND
        
        final int sound = SoundMessage.getSoundCode(label);
        label = SoundMessage.getMessageBody(label);
        final String text = (label.length()>0) ? label : null;
*/
        
 
        // Map prevent double notification
        Map<String, MessageReceiver> toCall = (addresses != null) ? addresses.getSelectedTargets()
                : new HashMap<String, MessageReceiver>();
        
        
        for (StringTokenizer othersList = new StringTokenizer(others.getText(), " \t\f,;");
                othersList.hasMoreElements(); ) {
            String element = othersList.nextToken();
            String dest = element.toLowerCase();
             
            if (dest.lastIndexOf(":") == -1)
                dest += ":" + NetReceiver.DEFAULT_PORT;
             if (!toCall.containsKey(dest))
                 toCall.put(dest, new MessageReceiver(null, element, dest));
        }

        preventSelfNotification(toCall);
        if (toCall.isEmpty()) {
            setStatus("ErrNoDest");
            return;
        }

        MessageReceiver[] receivers = new MessageReceiver[toCall.size()];
        toCall.values().toArray(receivers);
        
        final Message messageToSend = new Message(System.currentTimeMillis(), 
                getAuthor(), hostAddress, label, receivers);
        
        setStatus("");

        // Fork for simultaneous send 
        for (int iReceiver=0; iReceiver<receivers.length; iReceiver++) {
            final MessageReceiver messageReceiver = receivers[iReceiver];
            final int destIndex = iReceiver;
            new Thread() {
                public void run() {
                    NetReceiverRmi dest = null;
                    try {
                        // Distant Call
                        dest = remoteCall(messageToSend, messageReceiver, destIndex);
                    } catch (Exception e) { 
                        // On any rmi exception
                        netSendCall(messageToSend, messageReceiver);
                    }
                    remoteAcknowlegdment(messageToSend, messageReceiver, destIndex, dest);
                }
            }.start();
        }

        // Add message to recorded messages
        messagesRecord.addMessage(messageToSend);
        showLastMessage();

        message.setText("");
    }
    
    
    private void remoteAcknowlegdment(Message messageToSend, MessageReceiver receiver, int destIndex, NetReceiverRmi dest) {
        // remote Acknoledgement
        synchronized (messageToSend) {
            messageToSend.decreasedCalledCounter();
            if (messageToSend.isAllReceiversCalled()) { // Is the last to called
                messageToSend.notifyAll();
                // Notify display that all elements are sent
                acknowledge(messageToSend);                                
            } else if (receiver.getReceived() == MessageReceiver.STATUS.RECEIVED) {
                while (!messageToSend.isAllReceiversCalled()) try {
                    messageToSend.wait();
                } catch (InterruptedException ignored) {}
            } // else not need to wait, no ackn can be send
        }
        
        // Callback all dest
        if (receiver.getReceived() == MessageReceiver.STATUS.RECEIVED) try {
            dest.acknowledge(messageToSend, destIndex);
        } catch (Exception ignore) { /* No matter if callback failed */
            ignore.printStackTrace(); 
        }
    }
    
    
    private NetReceiverRmi remoteCall(Message messageToSend, MessageReceiver receiver, int destIndex) throws Exception {
        // remote Call
        String url = "rmi://" + receiver.getAddress() + "/" + NetReceiver.RMI_NAME;
        NetReceiverRmi dest = (NetReceiverRmi) Naming.lookup(url);
        dest.message(messageToSend, destIndex);
        receiver.setReceived(MessageReceiver.STATUS.RECEIVED);
        return dest;
    }
    
    private void netSendCall(Message messageToSend, MessageReceiver receiver) {
        // NetSend Call
        String netSendDest = receiver.getAddress();
        // Remove port from address
        netSendDest = netSendDest.substring(0, netSendDest.lastIndexOf(':'));
        
        if ("*".equals(netSendDest)) { // * is forbidden as net send target
            receiver.setReceived(MessageReceiver.STATUS.NOT_RECEIVED);
            
        } else if (isLocalNetSend(netSendDest)) {
            receiver.setReceived(MessageReceiver.STATUS.NET_SEND);

        } else {
            // Remove unsupported car !!! TODO Which one
            String text = getAuthor() + "> " + messageToSend.getContent();
            
            try { // try a Windows Net Send

                Process p = Runtime.getRuntime().exec(new String[] {
                        "net", "send", netSendDest, text
                }); // To test
                
                if (p.waitFor() != 2)
                    receiver.setReceived(MessageReceiver.STATUS.NET_SEND);
                else
                    receiver.setReceived(MessageReceiver.STATUS.NOT_RECEIVED);
            } catch (Exception e2) {
                receiver.setReceived(MessageReceiver.STATUS.NOT_RECEIVED);
            }
        }
    }
    
    
    private void preventSelfNotification(Map<String, MessageReceiver> toCall) {

        // Note: a temporary list is needed.
        // If removed if call in key iteration, iterator is corrupted
        List<String> addresseesToRemove = new ArrayList<String>();
        // Serach all addresses to remove
        for (String adressee : toCall.keySet()) {
            if (adressee.startsWith("127.0.0.1:") || adressee.startsWith("localhost:")) {
                addresseesToRemove.add(adressee);
                continue;
            }
            
            for (int iHost=0; iHost<hostIdentifications.length; iHost++) {
                if (adressee.equals(hostIdentifications[iHost] + ":" + receiver.getPort())) {
                    addresseesToRemove.add(adressee);
                    continue;
                }
            }
        }

        for (String adresseeToRemove : addresseesToRemove) {
            toCall.remove(adresseeToRemove);
        }
    }

    private boolean isLocalNetSend(String toCall) {
        if (toCall.equals(System.getProperty("user.name")))
                return true;

        for (int iHost=0; iHost<hostIdentifications.length; iHost++)
            if (toCall.equals(hostIdentifications[iHost] + ":" + receiver.getPort()))
                return true;
        return false;
    }
    
    


    /**
     * Set a new exchanged message in display
     * Maybe a message send or received 
     * @param newMessage
     */
    public void message(final Message newMessage) {

        if (newMessage != null) { // Message may be null on double start
            EventQueue.invokeLater(new Runnable() { // Set in EventDispatch Thread
                public void run() {
                    messagesRecord.addMessage(newMessage);
                    showLastMessage();
                }
            });
        }

        // De-iconifie and to front
        Window canvas = SwingUtilities.getWindowAncestor(this);
        if ( (canvas != null) && (canvas instanceof Frame) ) {
           Frame frame = (Frame) canvas;
           if ((frame.getExtendedState() & JFrame.ICONIFIED) != 0) // is iconified
               frame.setExtendedState(frame.getExtendedState() & (~JFrame.ICONIFIED));
           canvas.toFront();
        }
    }

    public void acknowledge(final Message ackMessage) {
        EventQueue.invokeLater(new Runnable() { // Set in EventDispatch Thread
            public void run() {
                messagesRecord.acknowledge(ackMessage);
            }
        });
    }
    
    public String getAuthor() { return author; }


    
    public void selectExpAsAddresses(Message selection, boolean exclusive) {
        // Check for validity is performed when menu is displayed
        if (selection.isSenderLocal())
            throw new RuntimeException("Internal error: Expeditor cannot be selected from local sender");
        
        if (exclusive)
            addresses.clearChecked();
        
        if ((others.getText() == null) || (others.getText().length()==0) || exclusive) {
            others.setText(selection.getAuthorAddress());

        } else if (others.getText().indexOf(selection.getAuthorAddress()) == -1) {
            // Append
            others.setText(others.getText() + " " + selection.getAuthorAddress());
        }
    }

    public void selectAllAsAddresses(Message selection, boolean exclusive) {
        // Add sender
        if (!selection.isSenderLocal())
            selectExpAsAddresses(selection, exclusive);
        else if (exclusive)
            addresses.clearChecked();
        
        // Loop on destinaries
        for (int iReceiver=0; iReceiver<selection.getReceiversCount(); iReceiver++) {
            selectAsAddresses(selection.getReceiver(iReceiver) , false);
        }
    }

    public void selectAsAddresses(MessageReceiver selection, boolean exclusive) {
        if (exclusive) {
            addresses.clearChecked();
        }
        
        if (selection.isSourcePresent()) {
            addresses.getCheckModel().setSelected(selection.getSource(), true);

        } else if ((others.getText() == null) || (others.getText().length()==0)) {
            others.setText(selection.getAddress());

        } else if (others.getText().indexOf(selection.getAddress()) == -1) {
            // Not already selected
            others.setText(others.getText() + " " + selection.getAddress());
        }
            
    }

    public String getText(Enum<?> e) {
        return getText(e.getClass().getSimpleName() + "." + e.name());
    }
    
    /**
     * Returns the statuses.
     *
     * @return the statuses
     */
    public String getText(String id) {
        String text = texts.get(id);
        return (text != null) ? text : id;
    }

    public String getText(String id, Object... args) {
        String text = texts.get(id);
        if (text == null) {
            return id + Arrays.toString(args);
        }
        return MessageFormat.format(text, args);
    }
    
    
    /**
     * Returns the soundNames.
     *
     * @return the soundNames
     */
    public Map<String, String> getSoundNames() {
        return soundNames;
    }

    
    /**
     * Sets the soundNames.
     *
     * @param soundNames the soundNames to set
     */
    public void setSoundNames(Map<String, String> soundNames) {
        this.soundNames = soundNames;
    }
    



} // endClass NetSend