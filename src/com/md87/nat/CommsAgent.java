/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.md87.nat;

import java.net.InetAddress;

/**
 *
 * @author chris
 */
public interface CommsAgent {

    void sendNATType(final NatType type);

    NatType readNATType();

    void sendAddress(final InetAddress address);
    void sendPort(final int port);

    InetAddress readAddress();
    int readPort();

}
