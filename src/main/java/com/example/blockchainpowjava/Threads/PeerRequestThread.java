package com.example.blockchainpowjava.Threads;


import com.example.blockchainpowjava.Model.Block;
import com.example.blockchainpowjava.ServiceData.BlockchainData;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

            LinkedList<Block> receivedBC = (LinkedList<Block>) objectInput.readObject(); // read from the  InputStream. We should ge a LinkedList<Block>

            log.info("blockId = " + receivedBC.getLast().getblockId()  +
                    " Size= " + receivedBC.getLast().getTransactionList().size());

            // The same service layer BlockchainData is used to validate the Blockchain
            // The nodes in the network use the same protocol to detect malicious branch of the chain.
           objectOutput.writeObject(BlockchainData.getInstance().getBlockchainConsensus(receivedBC)); //  we send back the Blockchain after running it through our consensus validation

        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

    }
}