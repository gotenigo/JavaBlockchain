package com.example.blockchainpowjava.Controller;


import com.example.blockchainpowjava.Model.Transaction;
import com.example.blockchainpowjava.ServiceData.BlockchainData;
import com.example.blockchainpowjava.ServiceData.WalletData;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.Base64;

public class AddNewTransactionController {

    @FXML
    private TextField toAddress;
    @FXML
    private TextField value;

    @FXML
    public void createNewTransaction() throws GeneralSecurityException {
        Base64.Decoder decoder = Base64.getDecoder();
        Signature signing = Signature.getInstance("SHA256withDSA");
        Integer blockId = BlockchainData.getInstance().getTransactionListFX().get(0).getblockId();
        byte[] sendB = decoder.decode(toAddress.getText());
        Transaction transaction = new Transaction(WalletData.getInstance()
                .getWallet(),sendB ,Integer.parseInt(value.getText()), blockId, signing);
        BlockchainData.getInstance().addTransaction(transaction,false);
        BlockchainData.getInstance().addTransactionState(transaction);
    }
}