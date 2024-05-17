package org.cloud.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
@Slf4j
public class ClientNetwork {
    private static ClientNetwork instance;

    private final Socket socket;

    private final String host = "127.0.0.1";
    private final int port = 8189;

    private final ObjectDecoderInputStream inputStream;
    private final ObjectEncoderOutputStream outputStream;

    private ClientNetwork() throws IOException {
        socket = new Socket(host, port);
        log.debug("Client socket connected");
        inputStream = new ObjectDecoderInputStream(socket.getInputStream());
        outputStream = new ObjectEncoderOutputStream(socket.getOutputStream());
        log.debug("Client socket ready");
    }

    public static ClientNetwork getNetwork() throws IOException {
        if (instance == null) {
            instance = new ClientNetwork();
        }
        return instance;
    }

    public static void disconnect() {
        instance = null;
    }

    public ObjectDecoderInputStream getInputStream() {
        return inputStream;
    }

    public ObjectEncoderOutputStream getOutputStream() {
        return outputStream;
    }
}
