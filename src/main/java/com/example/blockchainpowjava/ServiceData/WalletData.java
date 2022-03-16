package com.example.blockchainpowjava.ServiceData;


import com.example.blockchainpowjava.Model.Wallet;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;

/***********************************************************************
 *
 *  This class is used to store our wallet while our application is running
 *   and make it available to every other  part of our application.
 *
 *   Because we want the same wallet to be available  throughout the application ,
 *   we will create a singleton class. So no duplicate wallet here :-)
 *
 *
 *****************************************************************/
public class WalletData {

    private Wallet wallet;
    //singleton class
    private static WalletData instance;

    static {
        instance = new WalletData();
    }

    public static WalletData getInstance() {
        return instance;
    }

    //This will load your wallet from the database.
    public void loadWallet() throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        Connection walletConnection = DriverManager.getConnection(
                "jdbc:sqlite:G:\\demo\\BlockchainPowJava\\src\\main\\db\\wallet.db");
        Statement walletStatment = walletConnection.createStatement();
        ResultSet resultSet;
        resultSet = walletStatment.executeQuery(" SELECT * FROM WALLET ");

        KeyFactory keyFactory = KeyFactory.getInstance("DSA"); // this is Class from java.security package. It will help us to generate a  PublicKey/ PrivateKey Object

        PublicKey pub2 = null;
        PrivateKey prv2 = null;
        while (resultSet.next()) {
            pub2 = keyFactory.generatePublic(   // generatePublic takes only KeySpec as param  -> generatePublic( KeySpec ks)
                    new X509EncodedKeySpec(resultSet.getBytes("PUBLIC_KEY")));  // we create a KeySpec object thanks to  X509EncodedKeySpec
            prv2 = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(resultSet.getBytes("PRIVATE_KEY"))); // we create a KeySpec object thanks to  X509EncodedKeySpec
        }
        this.wallet = new Wallet(pub2, prv2);  // Here we create a wallet using our  public + private key
    }

    public Wallet getWallet() {
        return wallet;
    }
}
