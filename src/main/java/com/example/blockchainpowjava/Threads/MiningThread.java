package com.example.blockchainpowjava.Threads;



import com.example.blockchainpowjava.ServiceData.BlockchainData;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class MiningThread extends Thread { // This Thread is constantly running. It makes sure new Block are created at continuous  interval
                                           // THis Thread will check if the Blockchain is up-to-date, if yes it will initiate
                                            // the mining of a new block at a precise interval.
                                            // Thread will loop every x second as per the code

    @Override
    public void run() {

        while (true) {

            long lastMinedBlock = LocalDateTime.parse(BlockchainData.getInstance()  //get the date of when  we mined our last block in seconds
                    .getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);

            if ((lastMinedBlock + BlockchainData.getTimeoutInterval()) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
                System.out.println("BlockChain is too old for mining! Update it from peers");
            } else if ( ((lastMinedBlock + BlockchainData.getMiningInterval()) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) > 0 ) {
                System.out.println("BlockChain is current, mining will commence in " +
                        ((lastMinedBlock + BlockchainData.getMiningInterval()) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) ) + " seconds");
            } else {
                System.out.println("MINING NEW BLOCK");
                    BlockchainData.getInstance().mineBlock();
                    System.out.println(BlockchainData.getInstance().getWalletBallanceFX());
            }

            System.out.println(LocalDateTime.parse(BlockchainData.getInstance()
                    .getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC));

            try {
                Thread.sleep(2000);
                if (BlockchainData.getInstance().isExit()) { break; }
                BlockchainData.getInstance().setMiningPoints(BlockchainData.getInstance().getMiningPoints() + 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }
}