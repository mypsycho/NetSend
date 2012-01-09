package org.mypsycho.netsend;


import java.util.LinkedList;
import java.util.List;

import javax.swing.treetable.AbstractTreeTableModel;
import javax.swing.treetable.TreeTableModel;

public class MessagesModel extends AbstractTreeTableModel {

    public static final int MAX = 300;
    
    // Names of the columns.
    static protected String[] cNames = { "history" }; 
    // not Displayable; used to get name for bundle 

    
    List<Message> messages = new LinkedList<Message>();
    
    // Root is not displayed so it can be anything
    // But Arraylist has changing hashCode
    public MessagesModel() { super(new Object()); }

    
    public void addMessage(Message newMessage) {
        messages.add(newMessage);
        while (messages.size() > MAX) {
            messages.remove(0);
        } 
        
        fireTreeNodesInserted(this, new Object[] { root }, // Parent path 
                new int[] {messages.size()-1}, new Object[] {newMessage});
        // fireTreeStructureChanged(this, new Object[] { messages }, null, null);
    }
    
    public void removeMessage(Message oldMessage) {
        int position = messages.indexOf(oldMessage);
        if (position == -1)
            return;
       
        messages.remove(oldMessage);
        
        
        fireTreeNodesRemoved(this, new Object[] { root }, 
                new int[] { position }, new Object[] { oldMessage });
    }

    public void clearMessages() {
        if (messages.isEmpty())
            return;
        
        messages.clear();
        fireTreeStructureChanged(this, new Object[] { root }, null, null);
    }
    
    public void acknowledge(Message ackMessage) {
        int position = messages.indexOf(ackMessage);
        if (position == -1) // Message no more exists
            return;
        
        Message old = messages.remove(position);
        messages.add(position, ackMessage);
        // Note: old object is required on modification
        fireTreeStructureChanged(this, new Object[] { root, old }, null, null);
    }
    
    public Class<?> getColumnClass(int column) { // 1 column
        return TreeTableModel.class; 
    }
    
    public int getColumnCount() { return cNames.length;}
    public String getColumnName(int column) { return cNames[column]; }
    
    public Object getValueAt(Object node, int column) { return node; }

    public Object getChild(Object parent, int index) {
        if (parent == root) {
            return messages.get(index);
        } else {
            return ((Message) parent).getReceiver(index);
        }
    }

    public boolean isLeaf(Object node) { return (node instanceof MessageReceiver); }
    
    public int getChildCount(Object parent) {
        if (parent == root) {
            return messages.size();
        } else if (parent instanceof Message){
            return ((Message) parent).getReceiversCount();
        } else {
            return 0;
        }
    }

}
