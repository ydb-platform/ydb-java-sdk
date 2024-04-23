package tech.ydb.test.integration.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.Random;

import javax.net.ServerSocketFactory;

/**
 * @author Alexandr Gorshenin
 */
public class PortsGenerator {
    public int findAvailablePort() {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket();
            serverSocket.close();
            return serverSocket.getLocalPort();
        } catch (IOException ex) {
            throw new RuntimeException("Can't find available port", ex);
        }
    }
}
