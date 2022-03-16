package com.example.blockchainpowjava.ServiceData;


import com.example.blockchainpowjava.Model.Block;
import com.example.blockchainpowjava.Model.Transaction;
import com.example.blockchainpowjava.Model.Wallet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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


public class BlockchainData {
                                                        //ObservableList is a list that allows listeners to track changes when they occur.
    private ObservableList<Transaction> newBlockTransactionsFX;  // We define a Transaction as a ObservableList (FXCollections - javafx.collections ) . ObservableList adds a way to listen for changes on a list
    private ObservableList<Transaction> newBlockTransactions;  // The List will hold the transaction of the Blockchain. it represents the current ledger of our Blockchain

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
        newBlockTransactions = FXCollections.observableArrayList();
        newBlockTransactionsFX = FXCollections.observableArrayList();
    }

    public static BlockchainData getInstance() {
        return instance;
    }

    Comparator<Transaction> transactionComparator = Comparator.comparing(Transaction::getTimestamp); // comparator used to  sort Transaction at time of creation


    public ObservableList<Transaction> getTransactionLedgerFX() {  // display the current Transaction ledger
        newBlockTransactionsFX.clear();                         // transfer the transaction from the current ledger to an ObservableList that we can display to UI
        newBlockTransactions.sort(transactionComparator);
        newBlockTransactionsFX.addAll(newBlockTransactions);
        return FXCollections.observableArrayList(newBlockTransactionsFX);
    }


    /*******************
     *  This Method is used to retrieve our own current balance when we need it throughout the application
     *
     * @return
     *************/
    public String getWalletBallanceFX() {
        return getBalance(currentBlockChain, newBlockTransactions,
                            WalletData.getInstance().getWallet().getPublicKey()).toString();
    }

    /**************************************
    //  getBalance() :: this Method go through the given Blockchain and a current ledger with potential transactions
    // for the new block and finds the balance for the given public address
    // but also it verifies if certain address has  enough coins before sending them
     *********************************************/
    private Integer getBalance(LinkedList<Block> blockChain,
                               ObservableList<Transaction> currentLedger, PublicKey walletAddress) {
        Integer balance = 0;
        for (Block block : blockChain) { // we got through each Block of the Blockchain.
            for (Transaction transaction : block.getTransactionLedger()) {
                if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) { // check if we are sending fund
                    balance -= transaction.getValue();             // if so, we debit the balance
                }
                if (Arrays.equals(transaction.getTo(), walletAddress.getEncoded())) { // check if we are receiving fund
                    balance += transaction.getValue(); // if so, we credit the balance
                }
            }
        }
        for (Transaction transaction : currentLedger) { // to prevent double spending, we also need to subtract
            if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) { //any fund we are already trying to send
                balance -= transaction.getValue();                               // that exist in the current transaction ledger
            }     // !! remember, double spending is a situation where user sends total fund 2 times. So effectively creating fund out of thin air
        }
        return balance; // we return the fund after calculation
    }


    /********************************
     *   This method validate our own Blockchain as well as the Blockchain received by other peer (miners) on the network
     *   This method that actually checks the validity  of each transaction and each blocks
     *
     * @param currentBlockChain
     * @throws GeneralSecurityException
     **************************/
    private void verifyBlockChain(LinkedList<Block> currentBlockChain) throws GeneralSecurityException {
        for (Block block : currentBlockChain) { // go through each Block

            if (!block.isVerified(signing)) {
                throw new GeneralSecurityException("Block validation failed");
            }

            ArrayList<Transaction> transactions = block.getTransactionLedger();  // then go through the Transaction of the particular Block
            for (Transaction transaction : transactions) {
                if (!transaction.isVerified(signing)) {
                    throw new GeneralSecurityException("Transaction validation failed");
                }
            }


        }
    }


    public void addTransactionState(Transaction transaction) {
        newBlockTransactions.add(transaction);
        newBlockTransactions.sort(transactionComparator);
    }



    public void addTransaction(Transaction transaction, boolean blockReward) throws GeneralSecurityException {
        try {
            if (getBalance(currentBlockChain, newBlockTransactions,
                    new DSAPublicKeyImpl(transaction.getFrom())) < transaction.getValue() && !blockReward) {
                throw new GeneralSecurityException("Not enough funds by sender to record transaction");
            } else {
                Connection connection = DriverManager.getConnection
                        ("jdbc:sqlite:G:\\demo\\BlockchainPowJava\\src\\main\\db\\blockchain.db");

                PreparedStatement pstmt;
                pstmt = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", LEDGER_ID, VALUE, SIGNATURE, CREATED_ON) " +
                        " VALUES (?,?,?,?,?,?) ");
                pstmt.setBytes(1, transaction.getFrom());
                pstmt.setBytes(2, transaction.getTo());
                pstmt.setInt(3, transaction.getLedgerId());
                pstmt.setInt(4, transaction.getValue());
                pstmt.setBytes(5, transaction.getSignature());
                pstmt.setString(6, transaction.getTimestamp());
                pstmt.executeUpdate();

                pstmt.close();
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void loadBlockChain() {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:G:\\demo\\BlockchainPowJava\\src\\main\\db\\blockchain.db");
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            while (resultSet.next()) {
                this.currentBlockChain.add(new Block(
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        loadTransactionLedger(resultSet.getInt("LEDGER_ID"))
                ));
            }

            latestBlock = currentBlockChain.getLast();
            Transaction transaction = new Transaction(new Wallet(),
                    WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100, latestBlock.getLedgerId() + 1, signing);
            newBlockTransactions.clear();
            newBlockTransactions.add(transaction);
            verifyBlockChain(currentBlockChain);
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException | NoSuchAlgorithmException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Transaction> loadTransactionLedger(Integer ledgerID) throws SQLException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:G:\\demo\\BlockchainPowJava\\src\\main\\db\\blockchain.db");
            PreparedStatement stmt = connection.prepareStatement
                    (" SELECT  * FROM TRANSACTIONS WHERE LEDGER_ID = ?");
            stmt.setInt(1, ledgerID);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getInt("VALUE"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getInt("LEDGER_ID"),
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

    public void mineBlock() {
        try {
            finalizeBlock(WalletData.getInstance().getWallet());
            addBlock(latestBlock);
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void finalizeBlock(Wallet minersWallet) throws GeneralSecurityException, SQLException {
        latestBlock = new Block(BlockchainData.getInstance().currentBlockChain);
        latestBlock.setTransactionLedger(new ArrayList<>(newBlockTransactions));
        latestBlock.setTimeStamp(LocalDateTime.now().toString());
        latestBlock.setMinedBy(minersWallet.getPublicKey().getEncoded());
        latestBlock.setMiningPoints(miningPoints);
        signing.initSign(minersWallet.getPrivateKey());
        signing.update(latestBlock.toString().getBytes());
        latestBlock.setCurrHash(signing.sign());
        currentBlockChain.add(latestBlock);
        miningPoints = 0;
        //Reward transaction
        latestBlock.getTransactionLedger().sort(transactionComparator);
        addTransaction(latestBlock.getTransactionLedger().get(0), true);
        Transaction transaction = new Transaction(new Wallet(), minersWallet.getPublicKey().getEncoded(),
                100, latestBlock.getLedgerId() + 1, signing);
        newBlockTransactions.clear();
        newBlockTransactions.add(transaction);
    }

    private void addBlock(Block block) {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:G:\\demo\\BlockchainPowJava\\src\\main\\db\\blockchain.db");
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement
                    ("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH, LEDGER_ID, CREATED_ON," +
                            " CREATED_BY, MINING_POINTS, LUCK) VALUES (?,?,?,?,?,?,?) ");
            pstmt.setBytes(1, block.getPrevHash());
            pstmt.setBytes(2, block.getCurrHash());
            pstmt.setInt(3, block.getLedgerId());
            pstmt.setString(4, block.getTimeStamp());
            pstmt.setBytes(5, block.getMinedBy());
            pstmt.setInt(6, block.getMiningPoints());
            pstmt.setDouble(7, block.getLuck());
            pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBC) {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:G:\\demo\\BlockchainPowJava\\src\\main\\db\\blockchain.db");
            Statement clearDBStatement = connection.createStatement();
            clearDBStatement.executeUpdate(" DELETE FROM BLOCKCHAIN ");
            clearDBStatement.executeUpdate(" DELETE FROM TRANSACTIONS ");
            clearDBStatement.close();
            connection.close();
            for (Block block : receivedBC) {
                addBlock(block);
                boolean rewardTransaction = true;
                block.getTransactionLedger().sort(transactionComparator);
                for (Transaction transaction : block.getTransactionLedger()) {
                    addTransaction(transaction, rewardTransaction);
                    rewardTransaction = false;
                }
            }
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public LinkedList<Block> getBlockchainConsensus(LinkedList<Block> receivedBC) {
        try {
            //Verify the validity of the received blockchain.
            verifyBlockChain(receivedBC);
            //Check if we have received an identical blockchain.
            if (!Arrays.equals(receivedBC.getLast().getCurrHash(), getCurrentBlockChain().getLast().getCurrHash())) {
                if (checkIfOutdated(receivedBC) != null) {
                    return getCurrentBlockChain();
                } else {
                    if (checkWhichIsCreatedFirst(receivedBC) != null) {
                        return getCurrentBlockChain();
                    } else {
                        if (compareMiningPointsAndLuck(receivedBC) != null) {
                            return getCurrentBlockChain();
                        }
                    }
                }
                // if only the transaction ledgers are different then combine them.
            } else if (!receivedBC.getLast().getTransactionLedger().equals(getCurrentBlockChain()
                    .getLast().getTransactionLedger())) {
                updateTransactionLedgers(receivedBC);
                System.out.println("Transaction ledgers updated");
                return receivedBC;
            } else {
                System.out.println("blockchains are identical");
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return receivedBC;
    }

    private void updateTransactionLedgers(LinkedList<Block> receivedBC) throws GeneralSecurityException {
        for (Transaction transaction : receivedBC.getLast().getTransactionLedger()) {
            if (!getCurrentBlockChain().getLast().getTransactionLedger().contains(transaction) ) {
                getCurrentBlockChain().getLast().getTransactionLedger().add(transaction);
                System.out.println("current ledger id = " + getCurrentBlockChain().getLast().getLedgerId() + " transaction id = " + transaction.getLedgerId());
                addTransaction(transaction, false);
            }
        }
        getCurrentBlockChain().getLast().getTransactionLedger().sort(transactionComparator);
        for (Transaction transaction : getCurrentBlockChain().getLast().getTransactionLedger()) {
            if (!receivedBC.getLast().getTransactionLedger().contains(transaction) ) {
                receivedBC.getLast().getTransactionLedger().add(transaction);
            }
        }
        receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
    }

    private LinkedList<Block> checkIfOutdated(LinkedList<Block> receivedBC) {
        //Check how old the blockchains are.
        long lastMinedLocalBlock = LocalDateTime.parse
                (getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long lastMinedRcvdBlock = LocalDateTime.parse
                (receivedBC.getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        //if both are old just do nothing
        if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            System.out.println("both are old check other peers");
            //If your blockchain is old but the received one is new use the received one
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            //we reset the mining points since we weren't contributing until now.
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockChain(new LinkedList<>());
            loadBlockChain();
            System.out.println("received blockchain won!, local BC was old");
            //If received one is old but local is new send ours to them
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) &&
                (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {

            return getCurrentBlockChain();
        }
        return null;
    }

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
            System.out.println("PeerClient blockchain won!, PeerServer's BC was old");
        } else if (initLocalBlockTIme < initRcvBlockTime) {
            return getCurrentBlockChain();
        }
        return null;
    }

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
                getCurrentBlockChain().getLast().getTransactionLedger().remove(0);
                for (Transaction transaction : getCurrentBlockChain().getLast().getTransactionLedger()) {
                    if (!receivedBC.getLast().getTransactionLedger().contains(transaction)) {
                        receivedBC.getLast().getTransactionLedger().add(transaction);
                    }
                }
                receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
                //we are returning the mining points since our local block lost.
                setMiningPoints(BlockchainData.getInstance().getMiningPoints() +
                        getCurrentBlockChain().getLast().getMiningPoints());
                replaceBlockchainInDatabase(receivedBC);
                setCurrentBlockChain(new LinkedList<>());
                loadBlockChain();
                System.out.println("Received blockchain won!");
            } else {
                // remove the reward transaction from their losing block and transfer
                // the transactions to our winning block
                receivedBC.getLast().getTransactionLedger().remove(0);
                for (Transaction transaction : receivedBC.getLast().getTransactionLedger()) {
                    if (!getCurrentBlockChain().getLast().getTransactionLedger().contains(transaction)) {
                        getCurrentBlockChain().getLast().getTransactionLedger().add(transaction);
                        addTransaction(transaction, false);
                    }
                }
                getCurrentBlockChain().getLast().getTransactionLedger().sort(transactionComparator);
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
