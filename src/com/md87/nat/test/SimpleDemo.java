/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.md87.nat.test;

import com.md87.nat.CommsAgent;
import com.md87.nat.NatTraverser;
import com.md87.nat.NatType;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.util.UtilityException;
import java.io.Console;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author chris
 */
public class SimpleDemo implements CommsAgent {

    static final Console console = System.console();

    public void sendNATType(NatType type) {
        console.printf("Nat type: %s\n", type.name());
    }

    public NatType readNATType() {
        console.printf("Enter NAT type: ");
        return NatType.valueOf(console.readLine().trim());
    }

    public void sendAddress(InetAddress address) {
        console.printf("Address: %s\n", address.getHostAddress());
    }

    public void sendPort(int port) {
        console.printf("Port: %s\n", port);
    }

    public InetAddress readAddress() {
        console.printf("Enter address: ");
        try {
            return InetAddress.getByName(console.readLine().trim());
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    public int readPort() {
        console.printf("Enter port: ");
        return Integer.parseInt(console.readLine().trim());
    }

    public static void main(final String ... args) throws
            UnsupportedOperationException, IOException, SocketException,
            UtilityException, MessageHeaderParsingException, 
            MessageAttributeParsingException, InterruptedException {
        final NatTraverser traverser = new NatTraverser(new SimpleDemo());
        traverser.findBestInetAddress();
        
        final DatagramSocket sock = traverser.traverse();

        System.out.println("Traversal complete. Say hello!");

        new Thread(new Runnable() {

            public void run() {
                while (true) {
                    DatagramPacket p = new DatagramPacket(new byte[255], 255);
                try {
                    
                        sock.receive(p);
                        System.out.printf("<< %s\n", new String(p.getData()));
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                }
            }
        }).start();

        new Thread(new Runnable() {

            public void run() {
                while (true) {try {
                    DatagramPacket p = new DatagramPacket(new byte[255], 255);
                    p.setData((console.readLine() + "\n").getBytes());
                    sock.send(p);
                    System.out.printf(">> %s\n", new String(p.getData()));
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                }}
            }
        }).start();
    }

}
