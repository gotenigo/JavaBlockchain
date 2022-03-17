package com.example.blockchainpowjava.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//singleton class
public final class Config {

    private static Config instance = null;

    private final String DB_WALLET_URL;
    private final String DB_BLOCKCHAIN_URL;
    private final Integer SERVER_PORT;
    private final List<Integer> CLIENT_PORT_LIST;


    public static Config getInstance()
    {
        if (instance==null){
            instance= new Config(null, null,null,null);
        }
        return instance;
    }


    public static Config getInstance(String dbWalletPath, String dbBlockchainPath,Integer serverPort, List<Integer> clientPortList)
    {
        if (instance==null){

            List<Integer> cloneList = new ArrayList<>(clientPortList); // clone list as object immutable
            instance= new Config(dbWalletPath, dbBlockchainPath, serverPort, cloneList);
        }
        return instance;
    }


    private Config(String dbWalletPath, String dbBlockchainPath, Integer serverPort, List<Integer> clientPortList) {

        if (dbWalletPath != null){
            DB_WALLET_URL = "jdbc:sqlite:" + dbWalletPath;
        }else{
            DB_WALLET_URL = "jdbc:sqlite:" + "G:\\demo\\BlockchainPowJava\\src\\main\\db\\wallet.db";
        }

        if (dbBlockchainPath != null){
            DB_BLOCKCHAIN_URL = "jdbc:sqlite:" + dbBlockchainPath;
        }else{
            DB_BLOCKCHAIN_URL = "jdbc:sqlite:" + "G:\\demo\\BlockchainPowJava\\src\\main\\db\\blockchain.db";
        }

        if (serverPort != null){
            SERVER_PORT = serverPort;
        }else{
            SERVER_PORT = 6000;
        }

        if (serverPort != null){
            CLIENT_PORT_LIST = clientPortList;
        }else{
            CLIENT_PORT_LIST = Arrays.asList(6001,6002);
        }

    }


    public String getDB_WALLET_URL() {
        return DB_WALLET_URL;
    }

    public String getDB_BLOCKCHAIN_URL() {
        return DB_BLOCKCHAIN_URL;
    }

    public Integer getSERVER_PORT() {
        return SERVER_PORT;
    }

    public List<Integer> getCLIENT_PORT_LIST() {
        return new ArrayList<>(CLIENT_PORT_LIST); //return a copy
    }
}
