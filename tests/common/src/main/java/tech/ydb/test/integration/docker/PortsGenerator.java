package tech.ydb.test.integration.docker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

import javax.net.ServerSocketFactory;

/**
 *
 * @author Alexandr Gorshenin
 */
public class PortsGenerator {
    private static final int PORT_RANGE_MIN = 51000;
    private static final int PORT_RANGE_MAX = 59000;
    private static final Random RANDOM = new Random(System.nanoTime());

    private int nextPort;

    public PortsGenerator() {
        this.nextPort = randomPort();
    }

    public int findAvailablePort() {
        while (true) {
            nextPort++;
            if (nextPort > PORT_RANGE_MAX) {
                nextPort = randomPort();
            }

            try {
                ServerSocket serverSocket = ServerSocketFactory.getDefault()
                        .createServerSocket(nextPort, 1, InetAddress.getByName("localhost"));
                serverSocket.close();
                return nextPort;
            } catch (IOException ex) { } // port is used
        }
    }

    private static int randomPort() {
        return PORT_RANGE_MIN + RANDOM.nextInt(PORT_RANGE_MAX - PORT_RANGE_MIN + 1);
    }
}
