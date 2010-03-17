/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.md87.nat;

import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.attribute.MessageAttributeException;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;
import de.javawi.jstun.util.UtilityException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 *
 * @author chris
 */
public class NatTraverser {

    protected final CommsAgent agent;

    protected InetAddress address;
    protected DiscoveryInfo result;

    protected NatType theirType;
    protected NatType ourType;

    public NatTraverser(final CommsAgent agent) {
        this.agent = agent;
    }

    public void setInetAddress(final InetAddress address) {
        this.address = address;
        this.result = getDiscoveryInfo(address);
        addressUpdated();
    }

    public void findBestInetAddress() {
        final Semaphore threadSem = new Semaphore(0);

        NatType bestType = null;
        Map.Entry<InetAddress, DiscoveryInfo> best = null;

        for (final Map.Entry<InetAddress, DiscoveryInfo> info : getDiscoveryInfos().entrySet()) {
            if (info.getValue() == null) {
                return;
            }

            final NatType thisType = getTypeFromDI(info.getValue());
            if (thisType != null &&
                    (bestType == null || thisType.ordinal() < bestType.ordinal())) {
                bestType = thisType;
                best = info;
            }
        }

        if (best != null) {
            this.address = best.getKey();
            this.result = best.getValue();
            addressUpdated();
        }
    }

    protected void addressUpdated() {
        try {
            ourType = getTypeFromDI(result);

            System.out.println("Using local IP: " + result.getLocalIP().getHostAddress()
                    + " (remote: " + result.getPublicIP().getHostAddress() + "; iface: "
                    + NetworkInterface.getByInetAddress(result.getLocalIP()).getName()
                    + "; type: " + ourType + ")");

            agent.sendNATType(ourType);

            theirType = agent.readNATType();
        } catch (SocketException ex) {
            // Meh
        }
    }

    public DatagramSocket traverse() throws UnsupportedOperationException,
            SocketException, UtilityException, IOException,
            MessageHeaderParsingException, MessageAttributeParsingException, InterruptedException {
        if (!getTypeFromDI(result).canTraverseWith(theirType)) {
            throw new UnsupportedOperationException("Cannot traverse "
                    + "between a " + ourType + " network and a "
                    + theirType + " network.");
        }

        final DatagramSocket sock
                = new DatagramSocket(new InetSocketAddress(address, 0));
        sock.connect(InetAddress.getByName("stun.dmdirc.com"), 3478);

        MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
        sendMH.generateTransactionID();

        byte[] data = sendMH.getBytes();
        DatagramPacket send = new DatagramPacket(data, data.length);
        sock.send(send);

        MessageHeader receiveMH = new MessageHeader();
        while (!(receiveMH.equalTransactionID(sendMH))) {
            DatagramPacket receive = new DatagramPacket(new byte[200], 200);
            sock.receive(receive);
            receiveMH = MessageHeader.parseHeader(receive.getData());
            receiveMH.parseAttributes(receive.getData());
        }

        final MappedAddress ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);

        System.out.println("I'm listening on " + address.getHostAddress() + ":"
                + sock.getLocalPort() + ". STUN server says I'm sending packets from "
                + ma.getAddress().getInetAddress().getHostAddress() + ":"
                + ma.getPort());

        agent.sendAddress(ma.getAddress().getInetAddress());
        agent.sendPort(ma.getPort());

        final InetAddress raddr = agent.readAddress();
        final int rport = agent.readPort();

        sock.disconnect();
        sock.connect(raddr, rport);

        boolean cont;

        do {
            cont = false;
            DatagramPacket p = new DatagramPacket("NatTraverser!".getBytes(), 13);
            
            try {
                sock.send(p);

                p = new DatagramPacket(new byte[13], 13);

                sock.receive(p);
            } catch (IOException ex) {
                ex.printStackTrace();

                cont = true;
                Thread.sleep(500);
            }
        } while (cont);

        return sock;
    }

    public static NatType getTypeFromDI(final DiscoveryInfo info) {
        if (info.isFullCone()) {
            return NatType.FULL_CONE;
        } else if (info.isOpenAccess()) {
            return NatType.OPEN;
        } else if (info.isPortRestrictedCone()) {
            return NatType.PORT_RESTRICTED_CONE;
        } else if (info.isRestrictedCone()) {
            return NatType.RESTRICTED_CONE;
        } else if (info.isSymmetric()) {
            return NatType.SYMMETRIC;
        } else {
            return null;
        }
    }

    protected Map<InetAddress, DiscoveryInfo> getDiscoveryInfos() {
        final Map<InetAddress, DiscoveryInfo> res
                = new HashMap<InetAddress, DiscoveryInfo>();

        try {
            final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                final Enumeration<InetAddress> inen = en.nextElement().getInetAddresses();

                while (inen.hasMoreElements()) {
                    final InetAddress ia = inen.nextElement();
                    res.put(ia, getDiscoveryInfo(ia));
                }
            }
        } catch (SocketException ex) {
            // Meh
        }

        return res;
    }

    protected DiscoveryInfo getDiscoveryInfo(final InetAddress iaddress) {
        try {
            return new DiscoveryTest(iaddress, "stun.dmdirc.com", 3478).test();
        } catch (IOException ex) {
            // Meh
        } catch (UtilityException ex) {
            // Meh
        } catch (MessageAttributeException ex) {
            // Meh
        } catch (MessageHeaderParsingException ex) {
            // Meh
        }

        return null;
    }

}
