package com.example.blockchainpowjava;

import com.example.blockchainpowjava.Model.Block;
import com.example.blockchainpowjava.Model.Transaction;
import com.example.blockchainpowjava.Model.Wallet;
import com.example.blockchainpowjava.ServiceData.BlockchainData;
import com.example.blockchainpowjava.ServiceData.WalletData;
import com.example.blockchainpowjava.Threads.MiningThread;
import com.example.blockchainpowjava.Threads.PeerClient;
import com.example.blockchainpowjava.Threads.PeerServer;
import com.example.blockchainpowjava.Threads.UI;
import com.example.blockchainpowjava.configuration.Config;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.sql.*;
import java.time.LocalDateTime;

/******************************************
 Life-cycle

 The entry point for JavaFX applications is the Application class. The JavaFX runtime does the following, in order, whenever an application is launched:

 1=> Constructs an instance of the specified Application class
 2=> Calls the init method
 3=> Calls the start method
 4=> Waits for the application to finish, which happens when either of the following occur:
    -the application calls Platform.exit
    -the last window has been closed and the implicitExit attribute on Platform is true
 5=> Calls the stop method

*****************************************/

@Slf4j
public class ECoin extends Application {

    private Config config;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        new UI().start(primaryStage);
        new PeerClient().start();  // we start PeerClient Thread  to send nd receive Blockchain from other Node operator
        new PeerServer( config.getSERVER_PORT() ).start(); // we start PeerServer Thread to keep listening Network request from Node operator
        new MiningThread().start(); // We need to keep on mining. So this Thread run all the time

    }

    @Override
    public void init() {
        try {

            //String dbWalletPath = "G:\\demo\\BlockchainPowJava\\src\\main\\db\\wallet.db";
            //String dbBlockchainPath = "G:\\demo\\BlockchainPowJava\\src\\main\\db\\blockchain.db";
            //this.config = Config.getInstance( dbWalletPath, dbBlockchainPath,null,null );
            this.config = Config.getInstance();


            //This creates your wallet if there is none and gives you a KeyPair.
            //We will create it in separate db for better security and ease of portability.
            Connection walletConnection = DriverManager
                    .getConnection( config.getDB_WALLET_URL()  );
            Statement walletStatment = walletConnection.createStatement();
            walletStatment.executeUpdate("CREATE TABLE IF NOT EXISTS WALLET ( " +
                    " PRIVATE_KEY BLOB NOT NULL UNIQUE, " +
                    " PUBLIC_KEY BLOB NOT NULL UNIQUE, " +
                    " PRIMARY KEY (PRIVATE_KEY, PUBLIC_KEY)" +
                    ") "
            );
            ResultSet resultSet = walletStatment.executeQuery(" SELECT * FROM WALLET ");
            if (!resultSet.next()) { // !!! If database does not have any result, then we create a new Wallet
                Wallet newWallet = new Wallet(); // !!!!! new Wallet created
                byte[] pubBlob = newWallet.getPublicKey().getEncoded();
                byte[] prvBlob = newWallet.getPrivateKey().getEncoded();
                PreparedStatement pstmt = walletConnection // !! we then save the new Wallet key pair, freshly generated, into the database
                        .prepareStatement("INSERT INTO WALLET(PRIVATE_KEY, PUBLIC_KEY) " +
                        " VALUES (?,?) ");
                pstmt.setBytes(1, prvBlob);
                pstmt.setBytes(2, pubBlob);
                pstmt.executeUpdate();
            }
            resultSet.close();
            walletStatment.close();
            walletConnection.close();

            WalletData.getInstance().loadWallet();   //!!! We load the Wallet from the Database //  Here we load the Wallet as Singleton Object

//          This will create the db tables with columns for the Blockchain.
            Connection blockchainConnection = DriverManager
                    .getConnection(config.getDB_BLOCKCHAIN_URL());
            Statement blockchainStmt = blockchainConnection.createStatement();
            blockchainStmt.executeUpdate("CREATE TABLE IF NOT EXISTS BLOCKCHAIN ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " PREVIOUS_HASH BLOB UNIQUE, " +
                    " CURRENT_HASH BLOB UNIQUE, " +
                    " BLOCK_ID INTEGER NOT NULL UNIQUE, " +
                    " CREATED_ON  TEXT, " +
                    " CREATED_BY  BLOB, " +
                    " MINING_POINTS  TEXT, " +
                    " LUCK  NUMERIC, " +
                    " PRIMARY KEY( ID AUTOINCREMENT) " +
                    ")"
            );
            ResultSet resultSetBlockchain = blockchainStmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            Transaction initBlockRewardTransaction = null;
            if (!resultSetBlockchain.next()) {    // !!!  if there isnt any Existing Blockchain (as per the SELECT Query) , then we create a new Block

                Block firstBlock = new Block();
                firstBlock.setMinedBy(WalletData.getInstance().getWallet().getPublicKey().getEncoded()); // set the Block with our  public key
                firstBlock.setTimeStamp(LocalDateTime.now().toString());  // set the current TimeStamp

                //helper class.
                Signature signing = Signature.getInstance("SHA256withDSA");
                signing.initSign(WalletData.getInstance().getWallet().getPrivateKey()); // we sign the Block with our private key
                signing.update(firstBlock.toString().getBytes());  // we provide the data we are looking to sign
                firstBlock.setCurrHash(signing.sign());  // we update the Block object with the cryptographic signature ( update field field  "Block.CurrHash")


                PreparedStatement pstmt = blockchainConnection
                        .prepareStatement("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH , BLOCK_ID," + // we save that Block into the database
                                " CREATED_ON, CREATED_BY,MINING_POINTS,LUCK ) " +
                        " VALUES (?,?,?,?,?,?,?) ");
                pstmt.setBytes(1, firstBlock.getPrevHash());
                pstmt.setBytes(2, firstBlock.getCurrHash());
                pstmt.setInt(3, firstBlock.getblockId());
                pstmt.setString(4, firstBlock.getTimeStamp());
                pstmt.setBytes(5, WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                pstmt.setInt(6, firstBlock.getMiningPoints());
                pstmt.setDouble(7, firstBlock.getLuck());
                pstmt.executeUpdate(); // !!!! execute the SQL statement

                Signature transSignature = Signature.getInstance("SHA256withDSA"); //!!! We prepare  the initial block reward transaction for our block.
                initBlockRewardTransaction = new Transaction(WalletData.getInstance().getWallet(),      // we will insert it into the database later as we are not sure if it exists  in the database
                        WalletData.getInstance().getWallet().getPublicKey().getEncoded(),100,1,transSignature);
            }
            resultSetBlockchain.close();

            blockchainStmt.executeUpdate("CREATE TABLE IF NOT EXISTS TRANSACTIONS ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " \"FROM\" BLOB, " +
                    " \"TO\" BLOB, " +
                    " BLOCK_ID INTEGER, " +
                    " VALUE INTEGER, " +
                    " SIGNATURE BLOB UNIQUE, " +
                    " CREATED_ON TEXT, " +
                    " PRIMARY KEY(ID AUTOINCREMENT) " +
                    ")"
            );
            if (initBlockRewardTransaction != null) { // if the initial BlockRewardTransaction exists, then we proceed
                BlockchainData.getInstance().addTransaction(initBlockRewardTransaction,true); // we add the Transaction reward into the Database
                BlockchainData.getInstance().addTransactionState(initBlockRewardTransaction);  // !!! Add the tx State (in memory only) :   Simply   add a transaction into our current transaction ledger
            }
            blockchainStmt.close();
            blockchainConnection.close();
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.info("db failed: " + e.getMessage());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        BlockchainData.getInstance().loadBlockChain();   // !!!!! We reload the entire Blockchain from the Database
                                                        // this will be our current Blockchain  as part of the initialisation
    }
}

