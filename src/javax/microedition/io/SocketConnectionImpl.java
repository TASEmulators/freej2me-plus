/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketConnectionImpl implements SocketConnection {

    protected Socket socket;

    public SocketConnectionImpl() {
    }

    public SocketConnectionImpl(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
    }

    public SocketConnectionImpl(Socket socket) {
        this.socket = socket;
    }

    public String getAddress() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getInetAddress().toString();
    }

    public String getLocalAddress() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getLocalAddress().toString();
    }

    public int getLocalPort() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getLocalPort();
    }

    public int getPort() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException();
        }

        return socket.getPort();
    }

    public int getSocketOption(byte option) throws IllegalArgumentException,
            IOException {
        if (socket != null && socket.isClosed()) {
            throw new IOException();
        }
        switch (option) {
            case DELAY:
                if (socket.getTcpNoDelay()) {
                    return 1;
                } else {
                    return 0;
                }
            case LINGER:
                int value = socket.getSoLinger();
                if (value == -1) {
                    return 0;
                } else {
                    return value;
                }
            case KEEPALIVE:
                if (socket.getKeepAlive()) {
                    return 1;
                } else {
                    return 0;
                }
            case RCVBUF:
                return socket.getReceiveBufferSize();
            case SNDBUF:
                return socket.getSendBufferSize();
            default:
                throw new IllegalArgumentException();
        }
    }

    public void setSocketOption(byte option, int value)
            throws IllegalArgumentException, IOException {
        if (socket.isClosed()) {
            throw new IOException();
        }
        switch (option) {
            case DELAY:
                int delay;
                if (value == 0) {
                    delay = 0;
                } else {
                    delay = 1;
                }
                socket.setTcpNoDelay(delay == 0 ? false : true);
                break;
            case LINGER:
                if (value < 0) {
                    throw new IllegalArgumentException();
                }
                socket.setSoLinger(value == 0 ? false : true, value);
                break;
            case KEEPALIVE:
                int keepalive;
                if (value == 0) {
                    keepalive = 0;
                } else {
                    keepalive = 1;
                }
                socket.setKeepAlive(keepalive == 0 ? false : true);
                break;
            case RCVBUF:
                if (value <= 0) {
                    throw new IllegalArgumentException();
                }
                socket.setReceiveBufferSize(value);
                break;
            case SNDBUF:
                if (value <= 0) {
                    throw new IllegalArgumentException();
                }
                socket.setSendBufferSize(value);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void close() throws IOException {
        // TODO fix differences between Java ME and Java SE

        socket.close();
    }

    public InputStream openInputStream() throws IOException {
        return socket.getInputStream();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public OutputStream openOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

}

