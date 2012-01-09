package org.mypsycho.netsend;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.treetable.JTreeTable;

public class MessagesTree extends JTreeTable {
    
    // Popup Menus
    protected JPopupMenu messageMenu;
    protected enum MessageMenu {
        Forward, MessageRemove, SelectExp, AddExp, SelectAll, AddAll;

    }
    protected JPopupMenu receiverMenu;
    protected enum ReceiverMenu {
        Select, Add;
    }
    protected JPopupMenu invalidMenu;
 
    
    protected NetSend owner;
    MessagesModel model;
    
    final EventManager eventManager = new EventManager();
    
    public MessagesTree(NetSend p, MessagesModel m) {
        super(m);
        model = m;
        owner = p;

        
        // Acknowledged message are PLAIN, pending are ITALIC 
        getTreeRenderer().setCellRenderer(renderer);
        getTreeRenderer().setRootVisible(false);
        getTreeRenderer().setEditable(false);
        
        // getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setTableHeader(null); // N
        


        

        

        addMouseListener(eventManager);
    }
    
    enum IconId {
        inProgess, received, lost, someLost, send
    }
    
    Map<String, Icon> icons = new HashMap<String, Icon>();
    
    /**
     * Returns the icons.
     *
     * @return the icons
     */
    public Map<String, Icon> getIcons() {
        return icons;
    }

    /**
     * Returns the icons.
     *
     * @return the icons
     */
    public Icon getIcon(IconId id) {
        return icons.get(id.name());
    }
    
    static final Icon IN_PROGRESS_ICON = getIcon("grey_light.gif"); 
    static final Icon RECEIVED_ICON    = getIcon("green_light.gif"); 
    static final Icon LOST_ICON        = getIcon("red_light.gif");
    static final Icon SOME_LOST_ICON   = getIcon("yellow_light.gif");
    static final Icon NET_SEND_ICON    = SOME_LOST_ICON;
    
    static Icon getIcon(String name) {
        return new ImageIcon(MessagesTree.class.getResource("image/" + name)); 
    }

    public Dimension getPreferredSize() {
        // Default width is computed be Row preferred size
        return new Dimension(getTreeRenderer().getPreferredSize().width,
                super.getPreferredSize().height);
    }
    
    public boolean getScrollableTracksViewportWidth() {
        // when not in viewport, no reason to be called
        if ((autoResizeMode != AUTO_RESIZE_OFF) && (getParent() instanceof JViewport)) 
            return (((JViewport)getParent()).getWidth() > getPreferredSize().width);
        return false; 
    }
    
    // Table cell renderer
    DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {

        Font newMessageFont;
        Font acknowledgedMessageFont;

        //  public DefaultTreeCellRenderer() 
        {
            setIcon(null); // No icon for messages
            newMessageFont = MessagesTree.this.getFont().deriveFont(Font.ITALIC);
            acknowledgedMessageFont = MessagesTree.this.getFont().deriveFont(Font.PLAIN);
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                  boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            // Inspired from super
            this.hasFocus = hasFocus;
            
            setForeground(sel ? getTextSelectionColor() : getTextNonSelectionColor());


            setEnabled(tree.isEnabled());
            setComponentOrientation(tree.getComponentOrientation());
            selected = sel;

            
            // Typical for NetSend display
            if (value instanceof Message) {
                Message msg = (Message) value;

                // Setting icon and font
                if (msg.getStatus() == Message.Status.NOT_ACKNOWLEDGED) {
                    setFont(newMessageFont);
                    setIcon(IN_PROGRESS_ICON);
                } else {
                    setFont(acknowledgedMessageFont);
                    if (msg.getStatus() == Message.Status.ALL_RECEIVED) {
                        setIcon(RECEIVED_ICON);
                    } else if (msg.getStatus() == Message.Status.SOME_FAILED) {
                        setIcon(SOME_LOST_ICON);
                    } else if (msg.getStatus() == Message.Status.ALL_FAILED) {
                        setIcon(LOST_ICON);
                    }
                }

                String head = "- ";
                if (!msg.isSenderLocal()) {
                    head = msg.getAuthorName() + "> ";
                }
                setText(head + msg.getContent());
                
                // Setting tooltip
                String ttt = owner.getText("Message.info");
                ttt = MessageFormat.format(ttt, new Date(msg.getId()),
                        owner.getText(msg.getStatus()));
                setToolTipText(ttt);
                return this;
            } 
            if (value instanceof MessageReceiver) {
                MessageReceiver displayedReceiver = (MessageReceiver) value;
                setFont(displayedReceiver.isReceived() ? acknowledgedMessageFont : newMessageFont);

                // Setting icon and font
                if (displayedReceiver.getReceived() == MessageReceiver.STATUS.NOT_CALLED) {
                    setFont(newMessageFont);
                    setIcon(IN_PROGRESS_ICON);
                } else {
                    setFont(acknowledgedMessageFont);
                    if (displayedReceiver.getReceived() == MessageReceiver.STATUS.RECEIVED) {
                        setIcon(RECEIVED_ICON);
                    } else if (displayedReceiver.getReceived() == MessageReceiver.STATUS.NOT_RECEIVED) {
                        setIcon(LOST_ICON);
                    } else if (displayedReceiver.getReceived() == MessageReceiver.STATUS.NET_SEND) {
                        setIcon(NET_SEND_ICON);
                    }
                }
                
                setText(displayedReceiver.getName() + " [" + displayedReceiver.getAddress() + "]");

                // Setting tooltip
                setToolTipText(owner.getText(displayedReceiver.getReceived().toString()));
                return this;
            }
            
            // root 
            setIcon(null);
            setFont(acknowledgedMessageFont);
            setText((value != null) ? value.toString() : "null");
            setToolTipText(null);

            return this;
        }
    };
    
    
    Object[] target = null; // Value is set before showing menu
    
    class EventManager extends MouseAdapter implements ActionListener {

        
        public void actionPerformed(ActionEvent ae) {
            String action = ((JMenuItem) ae.getSource()).getActionCommand();

            if (target instanceof Message[]) {
                boolean fistElement = true; // For multi-select
                for (Message selection : (Message[]) target) {
                    if (MessageMenu.Forward.toString().equals(action)) {
                        owner.setContent(selection.getContent());
    
                    } else if (MessageMenu.MessageRemove.toString().equals(action)) {
                        model.removeMessage(selection);
    
                    } else if (MessageMenu.AddAll.toString().equals(action)) {
                        owner.selectAllAsAddresses(selection, false);
        
                    } else if (MessageMenu.AddExp.toString().equals(action)) {
                        owner.selectExpAsAddresses(selection, false);
                        
                    } else if (MessageMenu.SelectAll.toString().equals(action)) {
                        owner.selectAllAsAddresses(selection, fistElement);
                        
                    } else if (MessageMenu.SelectExp.toString().equals(action)) {
                        owner.selectExpAsAddresses(selection, fistElement);
                            
                    } else  // Internal error
                        throw new RuntimeException("BUG: Unexpected message action: " + action);
                    fistElement = false;
                }
            } else { // (target instanceof MessageReceiver)
                boolean fistElement = true;
                for (MessageReceiver selection : (MessageReceiver[]) target) {
                    if (ReceiverMenu.Add.toString().equals(action)) {
                        owner.selectAsAddresses(selection, false);
                        
                    } else if (ReceiverMenu.Select.toString().equals(action)) {
                        owner.selectAsAddresses(selection, fistElement);
                        
                    } else // Internal error
                        throw new RuntimeException("BUG: Unexpected destinary action: " + action);
                    fistElement = false;
                }
            }
        }
        
        public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger()) {
                showPopupMenu(me);
            }
        }

        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger()) {
                showPopupMenu(me);
            }
        }

 
        private void showPopupMenu(MouseEvent me) {
            Point p = me.getPoint();

            // Locate element under the event location
            int hitRowIndex = rowAtPoint(p);
            int[] selectedRows = getSelectedRows();

            // if left clic matches selection, perform a treatement pour
            // else change selection to 1 row
            boolean inSelection = false; // false by default, as selection may be empty
            eachSelectedRow: for (int eachSelectedRow : selectedRows) {
                if (eachSelectedRow == hitRowIndex) {
                    inSelection = true;
                    break eachSelectedRow;
                }
            }
            if (!inSelection) {
                getSelectionModel().setSelectionInterval(hitRowIndex, hitRowIndex);
                selectedRows = new int[] {hitRowIndex};
            }
            
            // Check multi selection
            Object selected = getValueAt(selectedRows[0], 0);
            JPopupMenu menu;
            if (selected instanceof Message) {
                menu = setUpMessageMenu(selectedRows);
            } else { // (selected instanceof MessageReceiver)
                menu = setUpDestinaryMenu(selectedRows);
            }

            menu.show(me.getComponent(), me.getX(), me.getY());
        }
        

    }
    
    protected JPopupMenu setUpMessageMenu(int[] selectedRows) {
        target = new Message[selectedRows.length];
        
        if (messageMenu == null) { // Lazy init to get parent text
            messageMenu = new JPopupMenu(owner.getText("MessageMenu"));
            for (MessageMenu action : MessageMenu.values()) {
                JMenuItem menuItem = new JMenuItem(owner.getText(action));
                menuItem.setActionCommand(action.toString()) ;
                messageMenu.add(menuItem);
                menuItem.addActionListener(eventManager);
            }
        }
        
        messageMenu.getComponent(MessageMenu.SelectExp.ordinal()).setEnabled(true);
        messageMenu.getComponent(MessageMenu.AddExp.ordinal()).setEnabled(true);
        messageMenu.getComponent(MessageMenu.Forward.ordinal()).setEnabled(selectedRows.length==1);
        
        // Fetch selected messages
        for (int iSelection=0; iSelection<selectedRows.length; iSelection++) {
            target[iSelection] = getValueAt(selectedRows[iSelection], 0);
            
            // Check selection is consistant
            if (!(target[iSelection] instanceof Message)) {
                target = null;
                
                return getInvalidMenu();
            } 
            
            if (((Message) target[iSelection]).isSenderLocal()) {
                // If 1or+ message is local, cannot select expeditor
                messageMenu.getComponent(MessageMenu.SelectExp.ordinal()).setEnabled(false);
                messageMenu.getComponent(MessageMenu.AddExp.ordinal()).setEnabled(false);
            }
        }
        return messageMenu;
    }
    
    JPopupMenu getInvalidMenu() {
        if (invalidMenu == null) {
            // Invalid menu is use on a mixte selection
            invalidMenu = new JPopupMenu(owner.getText("InvalidMenu"));
            invalidMenu.add(new JMenuItem(owner.getText("InvalidMenu")));
            ((JMenuItem) invalidMenu.getComponent(0)).setEnabled(false);
        }
        return invalidMenu;
    }
    
    protected JPopupMenu setUpDestinaryMenu(int[] selectedRows) {
        target = new MessageReceiver[selectedRows.length];
        
        if (receiverMenu == null) {
            receiverMenu = new JPopupMenu(owner.getText("ReceiverMenu"));
            for (ReceiverMenu action : ReceiverMenu.values()) {
                JMenuItem menuItem = new JMenuItem(owner.getText(action));
                menuItem.setActionCommand(action.toString()) ;
                receiverMenu.add(menuItem);
                menuItem.addActionListener(eventManager);
            }
        }
        
        // Fetch selected messages
        for (int iSelection=0; iSelection<selectedRows.length; iSelection++) {
            Object selected = getValueAt(selectedRows[iSelection], 0);
            if (!(selected instanceof MessageReceiver)) {
                target = null;
                return invalidMenu;
            }
            target[iSelection] = (MessageReceiver) selected;
        }

        return receiverMenu;
    }
    



    
}
