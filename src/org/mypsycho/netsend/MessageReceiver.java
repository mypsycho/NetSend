package org.mypsycho.netsend;

import java.io.Serializable;

public class MessageReceiver implements Serializable {
    
    transient AddressTree.AddressTreeNode source = null; // null when from sender is not local
    
    Message parent = null;
    
    String name;
    String address;
    
    public enum STATUS { 
        NOT_RECEIVED, NOT_CALLED, RECEIVED, NET_SEND;
        String toString = "Receiver." + name();
        public String toString() { return toString; }
    }
    STATUS received = STATUS.NOT_CALLED;
    
    
    public MessageReceiver(AddressTree.AddressTreeNode s, String n, String a) {
        source = s;
        name = n;
        address = a;
    }
    
    public boolean isSourcePresent() {
        return source != null;
    }
    
    public AddressTree.AddressTreeNode getSource() {
        return source;
    }

    public boolean isReceived() {
        return received.ordinal() >= STATUS.RECEIVED.ordinal();
    }

    public STATUS getReceived() {
        return received;
    }

    public void setReceived(STATUS received) {
        this.received = received;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public Message getParent() {
        return parent;
    }

    public void setParent(Message parent) {
        this.parent = parent;
    }

}
