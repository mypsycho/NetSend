package org.mypsycho.netsend;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.MessageFormat;

/**
 * 
 * @author psycho
 * @version 1.0
 */
public class NetReceiver implements NetReceiverRmi {

    public static final String RMI_NAME = "NetSend";
    public static final int DEFAULT_PORT = 2024;

    protected NetSend owner = null;


    public NetReceiver(NetSend o) throws RuntimeException {
        owner = o;
        try {
            UnicastRemoteObject.exportObject(this, 0);
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    protected int port = 0;
    public int getPort() { return port; }
    protected Registry rmiRegistry = null;
    public synchronized void setPort(int newPort) throws IOException { 
        if (port == newPort) // Return true is success
            return;

        if (port != 0) {
            throw new IOException("LIMITATION: Impossible to dynamically change port");
        }
        
        // !!! We do not know how to stop a started registry !!!

        // Create new registry
        
        Registry reg = null;
        try {
            rmiRegistry = LocateRegistry.createRegistry(newPort);
            port = newPort; 
            reg = rmiRegistry;
        } catch (RemoteException re) { // Port busy
            reg = LocateRegistry.getRegistry(newPort);
            try {
                NetReceiverRmi alreadyBound = (NetReceiverRmi) reg.lookup(RMI_NAME);
                if (alreadyBound instanceof NetReceiverRmi) {
                    ((NetReceiverRmi) alreadyBound).message(null, -1);
                    throw new AlreadyStartedException(
                                owner.getText("NetReceiver.ErrAlreadyStarted"));
                }
            } catch (RemoteException re2) { // Port busy by not rmiRegistry
                // Param set as String to prevent Locale format for Integer
                String msg = owner.getText("NetReceiver.ErrPortBusy");
                msg = MessageFormat.format(Integer.toString(newPort), 
                        Integer.toString(NetReceiver.DEFAULT_PORT));
                
                throw new IOException(msg);    
            } catch (NotBoundException nbe) {
                // Not bound, try to bind
            }
        }
        reg.rebind(RMI_NAME, this);
        port = newPort;
    }

    // For pop-up request message may be null
    public void message(Message message, int destIndex) throws RemoteException {
        if (message != null)
            message.setReceiver(destIndex);
        owner.message(message);
    }
    
    public void acknowledge(Message message, int destIndex) throws RemoteException {
        message.setReceiver(destIndex);
        owner.acknowledge(message);
    }


} // endclass NetReceiver