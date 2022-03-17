package com.example.blockchainpowjava.ServiceData;


import com.example.blockchainpowjava.Model.Block;
import com.example.blockchainpowjava.Model.Transaction;
import com.example.blockchainpowjava.Model.Wallet;
import com.example.blockchainpowjava.configuration.Config;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import sun.security.provider.DSAPublicKeyImpl;

import java.security.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;


/***************************************************************************
 *
 *  This is the biggest and most complex class in our application. Here we can find all  the functionality regarding
 *  our Blockchain. This Class is Singleton class.
 *
 *
 * ****************************************************************************/

@Slf4j
public class BlockchainData {
                                                        //ObservableList is a list that allows listeners to track changes when they occur.
    private ObservableList<Transaction> newTransactionListFX;  // We define a Transaction as a ObservableList (FXCollections - javafx.collections ) . ObservableList adds a way to listen for changes on a list
    private ObservableList<Transaction> newTransactionList;  // The List will hold the transaction of the Blockchain. it represents the current ledger of our Blockchain

    private LinkedList<Block> currentBlockChain = new LinkedList<>(); // This is our current Blockchain. It' s a LinkedList<Block>

    private Block latestBlock; // represent the latest Block we are trying to add to the Blockchain
    private boolean exit = false; // exit Boolean that helps the exit command for our front-end-work
    private int miningPoints; // help us to track the mining point  that we use in our consensus Algorithms
    private static final int TIMEOUT_INTERVAL = 65;    // timeout Interval used to define if the Blockchain is too old
    private static final int MINING_INTERVAL = 60;     //another time-out

    //helper class.
    private Signature signing = Signature.getInstance("SHA256withDSA"); //Java Signature that helps us  to create a Signature

    //singleton class
    private static BlockchainData instance;

    static {   // Static object that helps to make this class a Singleton class
        try {
            instance = new BlockchainData();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public BlockchainData() throws NoSuchAlgorithmException {
        newTransactionList = FXCollections.observableArrayList();
        newTransactionListFX = FXCollections.observableArrayList();
    }

    public static BlockchainData getInstance() {
        return instance;
    }

    Comparator<Transaction> transactionComparator = Comparator.comparing(Transaction::getTimestamp); // comparator used to  sort Transaction at time of creation


    public ObservableList<Transaction> getTransactionListFX() {  // display the current Transaction ledger
        newTransactionListFX.clear();                         // transfer the transaction from the current ledger to an ObservableList that we can display to UI
        newTransactionList.sort(transactionComparator);
        newTransactionListFX.addAll(newTransactionList);
        return FXCollections.observableArrayList(newTransactionListFX);
    }


    /*******************
     *  This Method is used to retrieve our own current balance when we need it throughout the application
     *
     * @return
     *************/
    public String getWalletBallanceFX() {
        return getBalance(currentBlockChain, newTransactionList,
                            WalletData.getInstance().getWallet().getPublicKey()).toString();
    }

    /**************************************
    //  getBalance() :: this Method go through the given Blockchain and a current ledger with potential transactions
    // for the new block and finds the balance for the given public address
    // but also it verifies if certain address has  enough coins before sending them
     *********************************************/
    private Integer getBalance(LinkedList<Block> blockChain,
                               ObservableList<Transaction> currentTransactionList, PublicKey walletAddress) {
        Integer balance = 0;
        for (Block block : blockChain) { // we got through each Block of the Blockchain.
            for (Transaction transaction : block.getTransactionList()) {
                if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) { // check if we are sending fund
                    balance -= transaction.getValue();             // if so, we debit the balance
                }
                if (Arrays.equals(transaction.getTo(), walletAddress.getEncoded())) { // check if we are receiving fund
                    balance += transaction.getValue(); // if so, we credit the balance
                }
            }
        }
        for (Transaction transaction : currentTransactionList) { // to prevent double spending, we also need to subtract
            if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) { //any fund we are already trying to send
                balance -= transaction.getValue();                               // that exist in the current transaction ledger
            }     // !! remember, double spending is a situation where user sends total fund 2 times. So effectively creating fund out of thin air
        }
        return balance; // we return the fund after calculation
    }


    /********************************
     *   This method validate our own Blockchain as well as the Blockchain received by other peer (miners) on the network
     *   This method that actually checks the validity  of each transaction of/and each blocks
     *
     * @param currentBlockChain
     * @throws GeneralSecurityException
     **************************/
    private void verifyBlockChain(LinkedList<Block> currentBlockChain) throws GeneralSecurityException {
        for (Block block : currentBlockChain) { // go through each Block

            if (!block.isVerified(signing)) {
                throw new GeneralSecurityException("Block validation failed");
            }

            ArrayList<Transaction> transactionList = block.getTransactionList();  // then go through the Transaction of the particular Block
            for (Transaction transaction : transactionList) {
                if (!transaction.isVerified(signing)) {
                    throw new GeneralSecurityException("Transaction validation failed");
                }
            }


        }
    }


    /******
     *
     * Simply   add a transaction into our current transaction ledger and sort it
     *
     * @param transaction
     */
    public void addTransactionState(Transaction transaction) {
        newTransactionList.add(transaction);
        newTransactionList.sort(transactionComparator);
    }


    /**********************************
     *
     *  Add a new Transaction into Database
     *
     * @param transaction   Transaction Object
     * @param blockReward boolean -  its ita reward Transaction for the miner ? Or is it regular Transaction
     *                              Flag this transaction  in order to skip the coin balance check performed by getBalance()
     * @throws GeneralSecurityException
     ****************************************************/
    public void addTransaction(Transaction transaction, boolean blockReward) throws GeneralSecurityException {
        try {
            if (getBalance(currentBlockChain, newTransactionList,
                    new DSAPublicKeyImpl(transaction.getFrom())) < transaction.getValue() && !blockReward) {
                throw new GeneralSecurityException("Not enough funds by sender to record transaction");
            } else {
                Connection connection = DriverManager.getConnection(Config.getInstance().getDB_BLOCKCHAIN_URL());

                PreparedStatement pstmt;
                pstmt = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", BLOCK_ID, VALUE, SIGNATURE, CREATED_ON) " +
                        " VALUES (?,?,?,?,?,?) ");
                pstmt.setBytes(1, transaction.getFrom());
                pstmt.setBytes(2, transaction.getTo());
                pstmt.setInt(3, transaction.getblockId());
                pstmt.setInt(4, transaction.getValue());
                pstmt.setBytes(5, transaction.getSignature());
                pstmt.setString(6, transaction.getTimestamp());
                pstmt.executeUpdate();

                pstmt.close();
                connection.close();
            }
        } catch (SQLException e) {
            log.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }

    }


    /***************************************************************
     *
     *    loadBlockChain()  ::  this method is used whenever we want to load the entire Blockchain
     *    from the database. For example, if we want to re-set the state of the app, then we will use that method
     *
     *   Logic : Read all the Block from the database (table Blockchain), then iterate and add each Block into the in-memory Blockchain (LinkedList<Block>)
     *
     ***************************************************************/
    public void loadBlockChain() {
        try {
            Connection connection = DriverManager.getConnection(Config.getInstance().getDB_BLOCKCHAIN_URL());
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            while (resultSet.next()) {
                // we add each Block into the Blockchain (LinkedList<Block>) => currentBlockChain
                this.currentBlockChain.add(new Block(        // each Block holds a List<Transaction> . And Each Transaction comes under an Id called blockId (Integer)
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getInt("BLOCK_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        loadTransactionList(resultSet.getInt("BLOCK_ID"))  // we are in new Block, so we need an ArrayList<Transaction>. As such we  resolve the  ArrayList<Transaction> from transactionList with the blockId
                                                                    // the List of Transaction is in the Table Transaction
                                                                        //" SELECT  * FROM TRANSACTIONS WHERE BLOCK_ID = ?"
                                                                            // So we pull from the database all transaction belonging to the same Block (BLOCK_ID)
                ));
            }

            latestBlock = currentBlockChain.getLast();   // we set the latestBlock with the last Block from the current Blockchain (LinkedList<Block> currentBlockChain)

            Transaction transaction = new Transaction(new Wallet(),  // we create a new reward Transaction. This is reward Transaction for our future Block
                    WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100 /*reward*/ , latestBlock.getblockId() + 1, signing);

            newTransactionList.clear();    // Remember !!! ObservableList<Transaction> newTransactionList
            newTransactionList.add(transaction);
            verifyBlockChain(currentBlockChain); //  we call Verify Blockchain method  on the current Blockchain we just loaded from the Database. This needed as we might import Blockchain from someone else Database
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException | NoSuchAlgorithmException e) {
            log.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }





    /*********************************************
     *
     *  This method load the transaction ledger for each block from the database to the application
     *
     *
     * @param blockId
     * @return
     * @throws SQLException
     *****************************************/
    private ArrayList<Transaction> loadTransactionList(Integer blockId) throws SQLException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection(Config.getInstance().getDB_BLOCKCHAIN_URL());
            PreparedStatement stmt = connection.prepareStatement
                    (" SELECT  * FROM TRANSACTIONS WHERE BLOCK_ID = ?");
            stmt.setInt(1, blockId);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getInt("VALUE"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getInt("BLOCK_ID"),
                        resultSet.getString("CREATED_ON")
                ));
            }
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }


    /****************************************************************
     *
     *   This method is called by the MiningThread class when it's time to mine new block
     *
     ****************************************************************/
    public void mineBlock() {
        try {

            finalizeBlock(WalletData.getInstance().getWallet()); // performs the necessary steps to finish up the finish  up the latest block
                                                                // and adds it to our current Blockchain (LinkedList<Block> currentBlockChain)
            addBlock(latestBlock); // we add the latestBlock to the Database

        } catch (SQLException | GeneralSecurityException e) {
            log.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /***************************************
     *
     * This method performs the necessary steps to finish up the finish  up the latest block
     *   and adds it to our current Blockchain (LinkedList<Block> currentBlockChain)
     *
     *
     * @param minersWallet
     * @throws GeneralSecurityException
     * @throws SQLException
     ***********************************/
    private void finalizeBlock(Wallet minersWallet) throws GeneralSecurityException, SQLException {

        // 1.Create Block
        latestBlock = new Block(BlockchainData.getInstance().currentBlockChain); // we prepare/finalize the latest Block
        latestBlock.setTransactionList(new ArrayList<>(newTransactionList)); // Lets go, we add the Transaction . ( Here the Transaction reward is already included as it was the 1st transaction within the list of Transaction)
        latestBlock.setTimeStamp(LocalDateTime.now().toString());   // we set the timestamp to the current time
        latestBlock.setMinedBy(minersWallet.getPublicKey().getEncoded()); // we set our own wallet address since we are  trying to mine this Block
        latestBlock.setMiningPoints(miningPoints); // we set the current mining point we have accumulated

        // 2. Sign the Block
        signing.initSign(minersWallet.getPrivateKey());
        signing.update(latestBlock.toString().getBytes());
        latestBlock.setCurrHash(signing.sign()); // we sign the Block
        currentBlockChain.add(latestBlock);
        miningPoints = 0; // we reset our mining point to 0

        // 3. Reward transaction
        latestBlock.getTransactionList().sort(transactionComparator); // we sort them in asc to make sure we pick up the 1st Transaction (which is the BlockReward transaction)
        addTransaction(latestBlock.getTransactionList().get(0), true); // add to the Database :  we add the reward transaction of the  Block we have just finalized to the Database.
                                                                    // Until now, we kept it in newTransactionList list (ObservableList<Transaction>), which
                                                                    // we copied in our latestBlock

        Transaction transaction = new Transaction(new Wallet(), minersWallet.getPublicKey().getEncoded(),  // we create a new  Block reward for the next mining cycle. it will be the 1st Transaction of the Block. So we created a new Transaction of value x (100 here) to be added in the Transaction list
                100, latestBlock.getblockId() + 1, signing); // the reward is set at 100
        newTransactionList.clear(); // newTransactionList contains now an old transaction  of the block we have finalized. So we clear newTransactionList out
        newTransactionList.add(transaction); // !!! newTransactionList is now loaded with the next reward transaction for the next mining cycle
                                                // This reward Transaction is what the miner gets for  successfully mining the next block
    }


    /*********************
     *
     *  Add the Block into the Database
     *
     * @param block
     *****************/
    private void addBlock(Block block) {
        try {
            Connection connection = DriverManager.getConnection(Config.getInstance().getDB_BLOCKCHAIN_URL());
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement
                    ("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH, BLOCK_ID, CREATED_ON," +
                            " CREATED_BY, MINING_POINTS, LUCK) VALUES (?,?,?,?,?,?,?) ");
            pstmt.setBytes(1, block.getPrevHash());
            pstmt.setBytes(2, block.getCurrHash());
            pstmt.setInt(3, block.getblockId());
            pstmt.setString(4, block.getTimeStamp());
            pstmt.setBytes(5, block.getMinedBy());
            pstmt.setInt(6, block.getMiningPoints());
            pstmt.setDouble(7, block.getLuck());
            pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException e) {
            log.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /*****************************************************
     *
     *
     *
     * @param receivedBC
     ***************************************************/
    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBC) {
        try {
            Connection connection = DriverManager.getConnection(Config.getInstance().getDB_BLOCKCHAIN_URL());
            Statement clearDBStatement = connection.createStatement();
            clearDBStatement.executeUpdate(" DELETE FROM BLOCKCHAIN ");
            clearDBStatement.executeUpdate(" DELETE FROM TRANSACTIONS ");
            clearDBStatement.close();
            connection.close();
            for (Block block : receivedBC) {

                //                    ("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH, BLOCK_ID, CREATED_ON," +
                //                            " CREATED_BY, MINING_POINTS, LUCK) VALUES (?,?,?,?,?,?,?) ");
                addBlock(block);             //  !!! we add the  Block into the database

                boolean rewardTransaction = true;
                block.getTransactionList().sort(transactionComparator);
                for (Transaction transaction : block.getTransactionList()) {

                    //"INSERT INTO TRANSACTIONS" + "(\"FROM\", \"TO\", BLOCK_ID, VALUE, SIGNATURE, CREATED_ON) " +" VALUES (?,?,?,?,?,?) ");
                    addTransaction(transaction, rewardTransaction);  // !!! We add the reward transaction into the database   //

                    rewardTransaction = false;
                }
            }
        } catch (SQLException | GeneralSecurityException e) {
            log.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /*************************************************************************
     *
     *  This Method executes the consensus Algorithms. This is the heart of the technology.
     *  This consensus  resolves the so-called Byzantine problem.
     *
     * we will be performing validation checks and several comparisons between our Blockchain
     *   and the one we receive here from other miner/peer on the Network. -> in parameter LinkedList<Block> receivedBC
     *
     *   we constantly try to share our Blockchain with other peers/miners. So getBlockchainConsensus()  gets to run  repeatedly
     *
     * @param receivedBC   -LinkedList<Block>  represents the Blockchain we have received from peer / miner
     * @return
     ******************************************************************************/
    public LinkedList<Block> getBlockchainConsensus(LinkedList<Block> receivedBC) {
        try {
            //Verify the validity of the received blockchain.
            verifyBlockChain(receivedBC); // we need to very each Blockchain we get from peer

            //Check if we have received an identical blockchain.
            if (!Arrays.equals(receivedBC.getLast().getCurrHash(), getCurrentBlockChain().getLast().getCurrHash())) { // this is the 1st comparison. We check both Blockchain's Block via the getLast() method

                // from here, it means both Blockchain have different miner on the last Block. Then we have to perform our consensus.
                // We need to work out which miner gets to mine the last block

                if (checkIfOutdated(receivedBC) != null) {  // if the Blockchain  is outdated (older than the mining interval), then the Blockchain wont be selected
                              // if out outdated, then we keep the Received blockchain. If both are outdated, then we dont do anything and we wait for the next UP-to-date Blockchain

                    return getCurrentBlockChain();

                } else { // both Blockchains are up-to-date

                    if (checkWhichIsCreatedFirst(receivedBC) != null) { // checkWhichIsCreatedFirst checks  which one was created 1st (look at the 1st Block for that) and both dont start with the same block
                        return getCurrentBlockChain();
                    } else {  // !!!! If we are here, it means both Blockchain are valid, and they are identical until the final Block
                        if (compareMiningPointsAndLuck(receivedBC) != null) {  // compare Mining Points And Luck . The selected Blockchain will be rule out by a luck (random number)
                            return getCurrentBlockChain();
                        }
                    }

                }
             // if both blockchain have the same last Block ( both Hash are the same )
             // if only the transaction ledgers are different then combine them.
            } else if (!receivedBC.getLast().getTransactionList().equals(getCurrentBlockChain() // Here we compare the Transaction ledger
                    .getLast().getTransactionList())) {
                updatetransactionLists(receivedBC);   // we merge both transaction
                log.info("Transaction ledgers updated");
                return receivedBC;
            } else {
                log.info("blockchains are identical");
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return receivedBC;
    }





    private void updatetransactionLists(LinkedList<Block> receivedBC) throws GeneralSecurityException {
        for (Transaction transaction : receivedBC.getLast().getTransactionList()) {
            if (!getCurrentBlockChain().getLast().getTransactionList().contains(transaction) ) {
                getCurrentBlockChain().getLast().getTransactionList().add(transaction);
                log.info("current blockId id = " + getCurrentBlockChain().getLast().getblockId() + " transaction id = " + transaction.getblockId());
                addTransaction(transaction, false);
            }
        }
        getCurrentBlockChain().getLast().getTransactionList().sort(transactionComparator);
        for (Transaction transaction : getCurrentBlockChain().getLast().getTransactionList()) {
            if (!receivedBC.getLast().getTransactionList().contains(transaction) ) {
                receivedBC.getLast().getTransactionList().add(transaction);
            }
        }
        receivedBC.getLast().getTransactionList().sort(transactionComparator);
    }


    /*************************************************
     *
     * @param receivedBC
     * @return
     ************************************************/
    private LinkedList<Block> checkIfOutdated(LinkedList<Block> receivedBC) {
        //Check how old the blockchains are.
        long lastMinedLocalBlock = LocalDateTime.parse
                (getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long lastMinedRcvdBlock = LocalDateTime.parse
                (receivedBC.getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        //if both are old just do nothing
        if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            log.info("both are old check other peers");
            //If your blockchain is old but the received one is new use the received one
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            //we reset the mining points since we weren't contributing until now.
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockChain(new LinkedList<>());
            loadBlockChain();
            log.info("received blockchain won!, local BC was old");
            //If received one is old but local is new send ours to them
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {

            return getCurrentBlockChain();
        }
        return null; // both are old   ,  the received one is newer or both are up-to-date
    }


    /****************************************
     *
     * @param receivedBC
     * @return
     ******************************************/
    private LinkedList<Block> checkWhichIsCreatedFirst(LinkedList<Block> receivedBC) {
        //Compare timestamps to see which one is created first.
        long initRcvBlockTime = LocalDateTime.parse(receivedBC.getFirst().getTimeStamp())
                .toEpochSecond(ZoneOffset.UTC);
        long initLocalBlockTIme = LocalDateTime.parse(getCurrentBlockChain().getFirst()
                .getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        if (initRcvBlockTime < initLocalBlockTIme) {
            //we reset the mining points since we weren't contributing until now.
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockChain(new LinkedList<>());
            loadBlockChain();
            log.info("PeerClient blockchain won!, PeerServer's BC was old");
        } else if (initLocalBlockTIme < initRcvBlockTime) {
            return getCurrentBlockChain();
        }
        return null;
    }


    /********************************************
     *
     *
     * @param receivedBC
     * @return
     * @throws GeneralSecurityException
     *****************************************************/
    private LinkedList<Block> compareMiningPointsAndLuck(LinkedList<Block> receivedBC)
            throws GeneralSecurityException {
        //check if both blockchains have the same prevHashes to confirm they are both
        //contending to mine the last block
        //if they are the same compare the mining points and luck in case of equal mining points
        //of last block to see who wins
        if (receivedBC.equals(getCurrentBlockChain())) {
            //If received block has more mining points points or luck in case of tie
            // transfer all transactions to the winning block and add them in DB.
            if (receivedBC.getLast().getMiningPoints() > getCurrentBlockChain()
                    .getLast().getMiningPoints() || receivedBC.getLast().getMiningPoints()
                    .equals(getCurrentBlockChain().getLast().getMiningPoints()) &&
                    receivedBC.getLast().getLuck() > getCurrentBlockChain().getLast().getLuck()) {
                //remove the reward transaction from our losing block and
                // transfer the transactions to the winning block
                getCurrentBlockChain().getLast().getTransactionList().remove(0);
                for (Transaction transaction : getCurrentBlockChain().getLast().getTransactionList()) {
                    if (!receivedBC.getLast().getTransactionList().contains(transaction)) {
                        receivedBC.getLast().getTransactionList().add(transaction);
                    }
                }
                receivedBC.getLast().getTransactionList().sort(transactionComparator);
                //we are returning the mining points since our local block lost.
                setMiningPoints(BlockchainData.getInstance().getMiningPoints() +
                        getCurrentBlockChain().getLast().getMiningPoints());
                replaceBlockchainInDatabase(receivedBC);
                setCurrentBlockChain(new LinkedList<>());
                loadBlockChain();
                log.info("Received blockchain won!");
            } else {
                // remove the reward transaction from their losing block and transfer
                // the transactions to our winning block
                receivedBC.getLast().getTransactionList().remove(0);
                for (Transaction transaction : receivedBC.getLast().getTransactionList()) {
                    if (!getCurrentBlockChain().getLast().getTransactionList().contains(transaction)) {
                        getCurrentBlockChain().getLast().getTransactionList().add(transaction);
                        addTransaction(transaction, false);
                    }
                }
                getCurrentBlockChain().getLast().getTransactionList().sort(transactionComparator);
                return getCurrentBlockChain();
            }
        }
        return null;
    }






    public LinkedList<Block> getCurrentBlockChain() {
        return currentBlockChain;
    }

    public void setCurrentBlockChain(LinkedList<Block> currentBlockChain) {
        this.currentBlockChain = currentBlockChain;
    }

    public static int getTimeoutInterval() { return TIMEOUT_INTERVAL; }

    public static int getMiningInterval() { return MINING_INTERVAL; }

    public int getMiningPoints() {
        return miningPoints;
    }

    public void setMiningPoints(int miningPoints) {
        this.miningPoints = miningPoints;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }
}
