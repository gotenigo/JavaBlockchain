package com.example.blockchainpowjava.Controller;


import com.example.blockchainpowjava.Model.Transaction;
import com.example.blockchainpowjava.ServiceData.BlockchainData;
import com.example.blockchainpowjava.ServiceData.WalletData;
import com.example.blockchainpowjava.Threads.UI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class MainWindowController {

    @FXML
    public TableView<Transaction> tableview = new TableView<>(); //this is read-only UI table
    @FXML
    private TableColumn<Transaction, String> from;
    @FXML
    private TableColumn<Transaction, String> to;
    @FXML
    private TableColumn<Transaction, Integer> value;
    @FXML
    private TableColumn<Transaction, String> timestamp;
    @FXML
    private TableColumn<Transaction, String> signature;
    @FXML
    private BorderPane borderPane;
    @FXML
    private TextField eCoins;
    @FXML
    private TextArea publicKey;

    public void initialize() {
        Base64.Encoder encoder = Base64.getEncoder();
        from.setCellValueFactory(
                new PropertyValueFactory<>("fromFX"));
        to.setCellValueFactory(
                new PropertyValueFactory<>("toFX"));
        value.setCellValueFactory(
                new PropertyValueFactory<>("value"));
        signature.setCellValueFactory(
                new PropertyValueFactory<>("signatureFX"));
        timestamp.setCellValueFactory(
                new PropertyValueFactory<>("timestamp"));
        eCoins.setText(BlockchainData.getInstance().getWalletBallanceFX());
        publicKey.setText(encoder.encodeToString(WalletData.getInstance().getWallet().getPublicKey().getEncoded()));
        tableview.setItems(BlockchainData.getInstance().getTransactionLedgerFX());
        tableview.getSelectionModel().select(0);
    }

    @FXML
    public void toNewTransactionController() {
        Dialog<ButtonType> newTransactionController = new Dialog<>();
        newTransactionController.initOwner(borderPane.getScene().getWindow());
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(UI.class.getResource("AddNewTransactionWindow.fxml"));
        try {
            newTransactionController.getDialogPane().setContent(fxmlLoader.load());
        } catch (IOException e) {
            log.info("Cant load dialog");
            e.printStackTrace();
            return;
        }
        newTransactionController.getDialogPane().getButtonTypes().add(ButtonType.FINISH);
        Optional<ButtonType> result = newTransactionController.showAndWait();
        if (result.isPresent() ) {
            tableview.setItems(BlockchainData.getInstance().getTransactionLedgerFX());
            eCoins.setText(BlockchainData.getInstance().getWalletBallanceFX());
        }
    }

    @FXML
    public void refresh() {
        tableview.setItems(BlockchainData.getInstance().getTransactionLedgerFX());
        tableview.getSelectionModel().select(0);
        eCoins.setText(BlockchainData.getInstance().getWalletBallanceFX());
    }

    @FXML
    public void handleExit() {
        BlockchainData.getInstance().setExit(true);
        Platform.exit();
    }
}
