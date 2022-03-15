package com.example.blockchainpowjava.Threads;



import com.example.blockchainpowjava.Model.Block;
import com.example.blockchainpowjava.ServiceData.BlockchainData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/******************************************
 *
 *  Our PeerClient Thread  will cycle through a predetermined list of peer
 *  and try to share our blockchain  with them. Since we intend to constantly contact other peers
 *  and share our blockchain, we loop this thread  in while(true) loop. *
 *
 ****************************************/


public class PeerClient extends Thread {

    private Queue<Integer> queue = new ConcurrentLinkedQueue<>();

    public PeerClient() {
        this.queue.add(6001);
        this.queue.add(6002);
    }

    @Override
    public void run() {

        while (true) {

            //Creates a stream socket and connects it to the specified port number on the named host.
            try (Socket socket = new Socket("127.0.0.1", queue.peek())) {  // try-with-resources (introduced in Java 7)
                   // allows us to declare resources to be used in a try block with the assurance that the resources will be closed after the execution of that block.

                System.out.println("Sending blockchain object on port: " + queue.peek());
                queue.add(queue.poll()); // rotate the port order // so  we rotate at each iteration while(true)
                socket.setSoTimeout(5000);

                ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());

                LinkedList<Block> blockChain = BlockchainData.getInstance().getCurrentBlockChain(); // we use the service layer to get the Blockchain object which is a LinkedList<Block>
                objectOutput.writeObject(blockChain); // Send the Blockchain via the port
                                                                                                    // Object should be a LinkedList<Block>
                LinkedList<Block> returnedBlockchain = (LinkedList<Block>) objectInput.readObject(); // Read the Blockchain from port (the blockchain send by other Miner on the Network)

                System.out.println(" RETURNED BC LedgerId = " + returnedBlockchain.getLast().getLedgerId()  +
                        " Size= " + returnedBlockchain.getLast().getTransactionLedger().size());

                BlockchainData.getInstance().getBlockchainConsensus(returnedBlockchain); // we check the Blockchain is valid
                Thread.sleep(2000);

            } catch (SocketTimeoutException e) {
                System.out.println("The socket timed out");
                queue.add(queue.poll()); // rotate the port and carry on with the next one
            } catch (IOException e) {
                System.out.println("Client Error: " + e.getMessage() + " -- Error on port: "+ queue.peek());
                queue.add(queue.poll()); // rotate the port and carry on with the next one
            } catch (InterruptedException | ClassNotFoundException e) {
                e.printStackTrace();
                queue.add(queue.poll()); // rotate the port and carry on with the next one
            }

        }



    }
}
