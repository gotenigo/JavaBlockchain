package com.example.blockchainpowjava.Threads;



import com.example.blockchainpowjava.ServiceData.BlockchainData;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/*********************************
//  This Thread is constantly running as long the app is running.
//  It makes sure new Block are created at continuous  interval
//  THis Thread will check if the Blockchain is up-to-date, if yes it will initiate
//  the mining of a new block at a precise interval.
//  Thread will loop every x second as per the code. Since we are using Mining Point,
 it will also keep a track of the Mining point

 It uses the service BlockchainData to feed with the date when the last Block was mined


 Please note - all Node (peer) will all create local blocks and try to push theirs to stick,
 the one that sticks will be the one with the most mining points written on it,
 each client gets 1 point for each second spent runing. I used a simpler alternative to hashcash
 so that I can explain the consensus algorithm better, hence you get this one you should be able to replace it with any other.

**************************************/

@Slf4j
public class MiningThread extends Thread {

    @Override
    public void run() {

        while (true) {

            long timeOfLastMinedBlock = LocalDateTime.parse(BlockchainData.getInstance()  //get the date of when  we mined our last block in seconds
                    .getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);

                                                                                                                                    // A Block is being produced every x seconds (it's a continuous real time  process), so any Blockchain that was made in the last  x second is too old.
            if ((timeOfLastMinedBlock + BlockchainData.getTimeoutInterval()) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {  // TIMEOUT_INTERVAL = 65;  //  BlockchainData.getTimeoutInterval() is set to 65s. So anything above that will be considered too old. This is time out is set under BlockchainData
                log.info("BlockChain is too old for mining! Update it from peers"); // no point to mine anything until we get new version
            } else if ( ((timeOfLastMinedBlock + BlockchainData.getMiningInterval()) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) > 0 ) {  // MINING_INTERVAL = 60;  // check if less than 60s has passed since the last mined Block.
                log.info("BlockChain is current, mining will commence in " +     // if so then we print the remaining time (until 60s timeframe)
                        ((timeOfLastMinedBlock + BlockchainData.getMiningInterval()) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) ) + " seconds");
            } else {
                log.info("MINING NEW BLOCK"); //  Create a new Block.we are between 60s - 65s (5s) time windows where a new Block needs to be created. So it's time to create a new Block
                    BlockchainData.getInstance().mineBlock();   // !!!! uses BlockchainData to Mine a new Block !!!!! finalizeBlock + save it into the Database

                    log.info("The Wallet balance is now ="+BlockchainData.getInstance().getWalletBallanceFX());   // Print our new wallet Balance by using  service layer BlockchainData
            }

            log.info("Time of the latest Block mined "+LocalDateTime.parse(BlockchainData.getInstance()   // we print the time of our last mined Block
                    .getCurrentBlockChain().getLast().getTimeStamp()).toLocalTime());

            try {
                Thread.sleep(2000);
                if (BlockchainData.getInstance().isExit()) { break; }
                BlockchainData.getInstance().setMiningPoints(BlockchainData.getInstance().getMiningPoints() + 2); // 2 mining points are added
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }
}