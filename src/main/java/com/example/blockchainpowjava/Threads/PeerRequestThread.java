package com.example.blockchainpowjava.Threads;


import com.example.blockchainpowjava.Model.Block;
import com.example.blockchainpowjava.ServiceData.BlockchainData;
import sun.security.provider.DSAPublicKeyImpl;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;

/*********************************************
 *
 *  This Thread handles each specific request that arrives at this peer
 *
 *
 *************************************/
public class PeerRequestThread extends Thread {

    private Socket socket;

    public PeerRequestThread(Socket socket) {
        this.socket = socket; //socket present the link to the peer that sent the request
    }

    @Override
    public void run() {

        try {


            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());

            LinkedList<Block> recievedBC = (LinkedList<Block>) objectInput.readObject(); // read from the  InputStream. We should ge a LinkedList<Block>

            System.out.println("LedgerId = " + recievedBC.getLast().getLedgerId()  +
                    " Size= " + recievedBC.getLast().getTransactionLedger().size());

           objectOutput.writeObject(BlockchainData.getInstance().getBlockchainConsensus(recievedBC)); //  we send back the Blockchain after running it through our consensus validation

        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

    }
}