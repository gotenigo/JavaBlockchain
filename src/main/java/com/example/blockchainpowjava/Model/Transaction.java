package com.example.blockchainpowjava.Model;

import lombok.extern.slf4j.Slf4j;
import sun.security.provider.DSAPublicKeyImpl;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;


//Represent a Blockchain Transaction. one Block will have a List<Transaction>
@Slf4j
public class Transaction implements Serializable { // Needed to go through the Network

   private byte[] from; //public key (address) of the sender
   private String fromFX;
   private byte[] to; //public key (address) of the receiver
   private String toFX;
   private Integer value; // the amount of coins that will be sent
   private String timestamp; // timestamp at which the transaction has occurred
   private byte[] signature;   // encrypted (hash) of all the field. it will be used to validate the transaction
   private  String signatureFX; // a copy of signature in String (not byte)
   private Integer blockId; // the Id of the Ledger (helpful for database reconciliation )


   //Constructor for loading with existing signature
   //When re retrieved Data from the database
   public Transaction(byte[] from, byte[] to, Integer value, byte[] signature, Integer blockId,
                      String timeStamp) {
      Base64.Encoder encoder = Base64.getEncoder();
      this.from = from;
      this.fromFX = encoder.encodeToString(from);
      this.to = to;
      this.toFX = encoder.encodeToString(to);
      this.value = value;
      this.signature = signature;
      this.signatureFX = encoder.encodeToString(signature);
      this.blockId = blockId;
      this.timestamp = timeStamp;
   }


   //Constructor for creating a new transaction and signing it.
   //Used BAU to create a new Transaction
   // Object Wallet hold both public and private key
   public Transaction (Wallet fromWallet, byte[] toAddress, Integer value, Integer blockId,
                       Signature signing) throws InvalidKeyException, SignatureException {

      Base64.Encoder encoder = Base64.getEncoder();
      this.from = fromWallet.getPublicKey().getEncoded();
      this.fromFX = encoder.encodeToString(fromWallet.getPublicKey().getEncoded());
      this.to = toAddress;
      this.toFX = encoder.encodeToString(toAddress);
      this.value = value;
      this.blockId = blockId;
      this.timestamp = LocalDateTime.now().toString();
      signing.initSign(fromWallet.getPrivateKey());
      String sr = this.toString();
      signing.update(sr.getBytes()); // Here we provide data we want to Sign
      this.signature = signing.sign();  // Here we sign all the data defined in update().For more details look at http://tutorials.jenkov.com/java-cryptography/signature.html
      this.signatureFX = encoder.encodeToString(this.signature);

   }


   // We can verify the signature here via signing object
   //this method will be used by other peers to verify that each transaction is valid
   public Boolean isVerified(Signature signing)
           throws InvalidKeyException, SignatureException {

      signing.initVerify(new DSAPublicKeyImpl(this.getFrom())); // init by with DSA algorithms SHA256
      signing.update(this.toString().getBytes());
      return signing.verify(this.signature);

   }



   @Override
   public String toString() {
      return "Transaction{" +
              "from=" + Arrays.toString(from) +
              ", to=" + Arrays.toString(to) +
              ", value=" + value +
              ", timeStamp= " + timestamp +
              ", blockId=" + blockId +
              '}';
   }




   public byte[] getFrom() { return from; }
   public void setFrom(byte[] from) { this.from = from; }

   public byte[] getTo() { return to; }
   public void setTo(byte[] to) { this.to = to; }

   public Integer getValue() { return value; }
   public void setValue(Integer value) { this.value = value; }
   public byte[] getSignature() { return signature; }

   public Integer getblockId() { return blockId; }
   public void setblockId(Integer blockId) { this.blockId = blockId; }

   public String getTimestamp() { return timestamp; }

   public String getFromFX() { return fromFX; }
   public String getToFX() { return toFX; }
   public String getSignatureFX() { return signatureFX; }





   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Transaction)) return false;
      Transaction that = (Transaction) o;
      return Arrays.equals(getSignature(), that.getSignature());
   }



   @Override
   public int hashCode() {
      return Arrays.hashCode(getSignature());
   }



}
