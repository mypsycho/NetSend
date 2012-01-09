package org.mypsycho.netsend;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 
 * @author psycho
 * @version 1.0
 */
public interface NetReceiverRmi extends Remote {
    
    public void message(Message message, int destIndex) throws RemoteException;
    
    public void acknowledge(Message message, int destIndex) throws RemoteException;
    

} // interface NetReceiverRmi