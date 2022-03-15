package com.example.blockchainpowjava.Threads;


import com.example.blockchainpowjava.Model.Block;
import com.example.blockchainpowjava.ServiceData.BlockchainData;
import sun.security.provider.DSAPublicKeyImpl;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;


public class PeerRequestThread extends Thread {

    private Socket socket;

    public PeerRequestThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {


            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());

            LinkedList<Block> recievedBC = (LinkedList<Block>) objectInput.readObject(); // read from the  InputStream

            System.out.println("LedgerId = " + recievedBC.getLast().getLedgerId()  +
                    " Size= " + recievedBC.getLast().getTransactionLedger().size());

           objectOutput.writeObject(BlockchainData.getInstance().getBlockchainConsensus(recievedBC)); // write to the  OutputStream

        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

    }
}