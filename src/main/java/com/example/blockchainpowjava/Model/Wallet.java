package com.example.blockchainpowjava.Model;

import java.io.Serializable;
import java.security.*;


//Represent a Blockchain Wallet
public class Wallet implements Serializable {

    private KeyPair keyPair;



    //Constructors for generating new KeyPair
    public Wallet() throws NoSuchAlgorithmException {
        this(2048, KeyPairGenerator.getInstance("DSA")); // recall the 2nd Constructor with default param
    }


    public Wallet(Integer keySize, KeyPairGenerator keyPairGen) {
       keyPairGen.initialize(keySize);   // we generate a new key Pair
       this.keyPair = keyPairGen.generateKeyPair();
    }

    //Constructor for importing Keys only
    public Wallet(PublicKey publicKey, PrivateKey privateKey) {
        this.keyPair = new KeyPair(publicKey,privateKey);
    }

    public KeyPair getKeyPair() { return keyPair; }

    public PublicKey getPublicKey() { return keyPair.getPublic(); }
    public PrivateKey getPrivateKey() { return keyPair.getPrivate(); }


    @Override
    public String toString() {
        return "Wallet{" +
                "keyPair=" + keyPair +
                '}';
    }


}