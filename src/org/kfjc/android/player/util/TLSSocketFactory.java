package org.kfjc.android.player.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TLSSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory mSSLSocketFactory;

    public TLSSocketFactory() {
        SSLContext context = null;
        try {
            context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        mSSLSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLS(mSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLS(mSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLS(mSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLS(mSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLS(mSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLS(Socket socket) {
        if(socket != null && (socket instanceof SSLSocket)) {
            String[] protocols = new String[] {
                    "TLSv1.2"
            };
            ((SSLSocket) socket).setEnabledProtocols(protocols);
        }
        return socket;
    }
}