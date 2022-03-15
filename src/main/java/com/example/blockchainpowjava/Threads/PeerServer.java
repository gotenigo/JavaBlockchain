package com.example.blockchainpowjava.Threads;


import java.io.IOException;
import java.net.ServerSocket;


public class PeerServer extends Thread {

    private ServerSocket serverSocket;

    public PeerServer(Integer socketPort) throws IOException {
        this.serverSocket = new ServerSocket(socketPort);    // Define a new Server Socket based on the port passed into parameter
    }

    @Override
    public void run() {


        while (true) {

            try {

                new PeerRequestThread(serverSocket.accept()).start();  //Listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made.
                                  //A new Socket s is created and, if there is a security manager, the security manager's checkAccept method is called with s.getInetAddress().getHostAddress() and s.getPort()
                                // as its arguments to ensure the operation is allowed. This could result in a SecurityException.

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }


    }
}