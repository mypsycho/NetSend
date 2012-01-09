package org.mypsycho.netsend;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;

public class NetSendTest {

    
    
    
    /**************************
     ************************** 
     *  SHORT TEST
     ************************** 
     **************************/
    
    
    public static void testNetSendProcess() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] {
                    "net", "send", "localhost", "Text >s" });         
             
            p.waitFor();
            System.out.println("result : " + p.exitValue());
            
            InputStream s = p.getInputStream();
            for (int car = s.read(); car != -1; car = s.read()) {
                System.out.print((char) car);
            }
            
            s = p.getErrorStream();
            for (int car = s.read(); car != -1; car = s.read()) {
                System.out.print((char) car);
            }
            
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }        
    }
    
    public static void testProcess() {
        try {
            final Process p = Runtime.getRuntime().exec("cmd /C pause");
        
            JFrame f = new JFrame("Tester");
            final JButton enter = new JButton("Enter");
            enter.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        p.getOutputStream().write("\n".getBytes());
                        p.getOutputStream().flush();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            });
            f.getContentPane().add(enter);
            f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    p.destroy();
                }
            });
            f.pack();
            f.setVisible(true);
            
            
            // Check process end
            new Thread() {public void run() {
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                enter.setBackground(Color.RED);
            }}.start();
            
            byte[] buf=new byte[100];
            
            for (int nbRead = p.getInputStream().read(buf); 
                    nbRead != -1; 
                    nbRead = p.getInputStream().read(buf)) {
                System.out.println("Read " + nbRead + " : <" + new String(buf, 0, nbRead)+">");
            } 
            System.out.println("Main ended");
            
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    
    public static void dislayAddresses(String title, InetAddress[] adds) {
        System.out.println(title);
        for (int i=0; i<adds.length; i++) {
            System.out.println("  1 : " + adds[i].toString());
        }
    }
    

    public static void testInetAddress() throws Exception {
        
        dislayAddresses("127.0.0.1", InetAddress.getAllByName("127.0.0.1"));
        dislayAddresses("localhost", InetAddress.getAllByName("localhost"));
        
        InetAddress host = InetAddress.getLocalHost();
        System.out.println("CanonicalHostName => " + host.getCanonicalHostName());
        System.out.println("HostAddress => " + host.getHostAddress());
        System.out.println("HostName => " + host.getHostName());
        byte[] code = host.getAddress();
        short[] result = new short[code.length];
        for (int i=0; i<code.length; i++) {
            result[i] = (short) (code[i] & 255);
        }
        
        System.out.println("Address => " + Arrays.toString(result));
        
        
        String cHostName = host.getCanonicalHostName();
        dislayAddresses(cHostName, InetAddress.getAllByName(cHostName));

        String hostAddress = host.getHostAddress();
        dislayAddresses(hostAddress, InetAddress.getAllByName(hostAddress));

        String hostName = host.getHostName();
        dislayAddresses(hostName, InetAddress.getAllByName(hostName));
        System.exit(0);
    }

}
