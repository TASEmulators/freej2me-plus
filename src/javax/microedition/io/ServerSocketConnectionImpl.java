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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class ServerSocketConnectionImpl implements ServerSocketConnection {

    private ServerSocket serverSocket;

    public ServerSocketConnectionImpl(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public String getLocalAddress() throws IOException {
        InetAddress localHost = InetAddress.getLocalHost();
        return localHost.getHostAddress();
    }

    public int getLocalPort() throws IOException {
        return serverSocket.getLocalPort();
    }

    public StreamConnection acceptAndOpen() throws IOException {
        return new SocketConnectionImpl(serverSocket.accept());
    }

    public void close() throws IOException {
        serverSocket.close();
    }

}