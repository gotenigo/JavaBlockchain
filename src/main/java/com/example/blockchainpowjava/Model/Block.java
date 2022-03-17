package com.example.blockchainpowjava.Model;



import sun.security.provider.DSAPublicKeyImpl;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;


// !!! Block  = List<Transaction> !!!
//Represent a single Block of the Blockchain
public class Block implements Serializable {

    private byte[] prevHash; //Hash of the previous block, an important part to build the chain
    private byte[] currHash;  //the hash of this block. The Hash here is calculated with the private key of miner who got to mine this Block
    private String timeStamp; // The timestamp of the creation of this block (when it was mined)
    private byte[] minedBy; // will hold the public key of the miner who managed to mine that block. this value is used in the Verification process to check the currHash is valid
    private Integer ledgerId = 1; // this is just the TransactionId (Block -> Transaction is a oneToMany relationship) .  this field helps us to identity the correct Ledger for this block. helpful for Database lookup against table Block and Transaction
    private Integer miningPoints = 0;
    private Double luck = 0.0;

    // this is simpy an ArrayList of all transaction containing in this Block
    private ArrayList<Transaction> transactionLedger = new ArrayList<>(); //The actual data, any information having value, like a contract , Transaction


    //This constructor is used when we retrieve it from the db
    // Here we retrieve all the blocks completely finalized
    //this constructor helps us to instantiate the block with all the fields properly
    public Block(byte[] prevHash, byte[] currHash, String timeStamp, byte[] minedBy,Integer ledgerId,
                 Integer miningPoints, Double luck, ArrayList<Transaction> transactionLedger) {
        this.prevHash = prevHash;
        this.currHash = currHash;
        this.timeStamp = timeStamp;
        this.minedBy = minedBy;
        this.ledgerId = ledgerId;
        this.transactionLedger = transactionLedger;
        this.miningPoints = miningPoints;
        this.luck = luck;
    }


    //This constructor is used when we initiate it after retrieve.
    // This constructor is used while the application is running
    // it's used to create a completely new Block (in other world, the head of the Blockchain)
    public Block(LinkedList<Block> currentBlockChain) {
        Block lastBlock = currentBlockChain.getLast();
        prevHash = lastBlock.getCurrHash();
        ledgerId = lastBlock.getLedgerId() + 1;
        luck = Math.random() * 1000000;
    }


    //This constructor is only used by init() method to create the 1st Block
    //This constructor is used only for creating the first block in the blockchain.
    public Block() {
        prevHash = new byte[]{0};
    } //1st Block is always set with prevHash=0




    //Signature is a Class from java security package (java.security.Signature)
    //it can create a digital signature for binary data.
    //A digital signature is a message digest encrypted with a private key of a private / public key pair.
    // Anyone in possession of the public key can verify the digital signature.
    //Signature is a helper singleton class that allows us to encrypt/decrypt data using different Algorithms
    public Boolean isVerified(Signature signature)
            throws InvalidKeyException, SignatureException {

        signature.initVerify(new DSAPublicKeyImpl(this.minedBy));// We initiate the signature parameter  by using public Key  from minedBy
        signature.update(this.toString().getBytes()); // we insert the Data we want to verify (data in clear).
                                            // in this case, its the content of the String Method : prevHash + timeStamp + minedBy + ledgerId + miningPoints + luck

        return signature.verify(this.currHash); // Return Boolean  after verifying the dara contained in this class against currHash

    }




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;
        Block block = (Block) o;
        return Arrays.equals(getPrevHash(), block.getPrevHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getPrevHash());
    }


    public byte[] getPrevHash() { return prevHash; }
    public byte[] getCurrHash() { return currHash; }

    public void setPrevHash(byte[] prevHash) { this.prevHash = prevHash; }
    public void setCurrHash(byte[] currHash) { this.currHash = currHash; }

    public ArrayList<Transaction> getTransactionLedger() { return transactionLedger; }
    public void setTransactionLedger(ArrayList<Transaction> transactionLedger) {
        this.transactionLedger = transactionLedger;
    }




    public String getTimeStamp() { return timeStamp; }
    public void setMinedBy(byte[] minedBy) { this.minedBy = minedBy; }

    public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }

    public byte[] getMinedBy() { return minedBy; }

    public Integer getMiningPoints() { return miningPoints; }
    public void setMiningPoints(Integer miningPoints) { this.miningPoints = miningPoints; }
    public Double getLuck() { return luck; }
    public void setLuck(Double luck) { this.luck = luck; }

    public Integer getLedgerId() { return ledgerId; }
    public void setLedgerId(Integer ledgerId) { this.ledgerId = ledgerId; }





    @Override
    public String toString() {
        return "Block{" +
                "prevHash=" + Arrays.toString(prevHash) +
                ", timeStamp='" + timeStamp + '\'' +
                ", minedBy=" + Arrays.toString(minedBy) +
                ", ledgerId=" + ledgerId +
                ", miningPoints=" + miningPoints +
                ", luck=" + luck +
                '}';
    }



}
