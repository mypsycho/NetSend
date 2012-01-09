package org.mypsycho.netsend;

import java.io.Serializable;


public class Message implements Serializable {

    transient int nbReceiversToCall = 0; 
    
    String content;
    
    String authorName;
    String authorAddress;
    long id; // message identification from sender

    MessageReceiver[]/*<MessageReceiver>*/ receivers;
    enum Status { 
        NOT_ACKNOWLEDGED, ALL_RECEIVED, SOME_FAILED, ALL_FAILED;
        
        String toString = "Message." + name();
        
        public String toString() { return toString; }
    }
    Status result = Status.NOT_ACKNOWLEDGED;
    
    // -1 : This JVM is the author, otherwise
    int receiverIndex = -1; // This Message 
    
    public Message(long i, String n, String a, String c, MessageReceiver[] r) {
        id = i;
        authorName = n;
        authorAddress = a;
        content = c;
        
        receivers = r;
        nbReceiversToCall = r.length;
        for (MessageReceiver receiver : receivers) {
            receiver.setParent(this);
        }
    }

    public String getAuthorAddress() {
        return authorAddress;
    }

    
    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }
    
    public Status getStatus() {
        return result;
    }
    
    public long getId() {
        return id;
    }

    public boolean isSenderLocal() {
        return (receiverIndex == -1);
    }
    
    public int getReceiversCount() {
        return (receiverIndex == -1) ? receivers.length : receivers.length-1;
    }
    
    public MessageReceiver getReceiver(int index) {
        if ((receiverIndex == -1) || (index<receiverIndex))
            return receivers[index];
        else 
            return receivers[index+1];
    }

    public void setReceiver(int receiverIndex) {
        this.receiverIndex = receiverIndex;
    }

    
    // This part is only applicable in sender

    public boolean isAllReceiversCalled() {
        return (nbReceiversToCall==0);
    }
    
    public int decreasedCalledCounter() {
        /* Message must be synchronized  */
        nbReceiversToCall--;
        
        if (isAllReceiversCalled()) {
            int received = 0;
            for (MessageReceiver receiver : receivers) {
                if (receiver.isReceived())
                    received++;
            }
            
            if (received == receivers.length) {
                result = Status.ALL_RECEIVED;
            } else if (received == 0) {
                result = Status.ALL_FAILED;
            } else {
                result = Status.SOME_FAILED;
            }
        }
        
        return nbReceiversToCall; 
    }
    
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Message))
            return false;
        
        Message other = (Message) o;
        return (id==other.id) && authorAddress.equals(other.authorAddress);
    }

    
}
