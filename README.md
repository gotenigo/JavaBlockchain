# BlockChain Implementation with Java.
In this project, we will demonstrate the power of the Blockchain by implementing simple PoW Blockchain in Java.
There are many consensus algorithms that could be used to build a Blockchain. Here, we will be using Proof of work (PoW)
that is the most famous one. Proof of work is also used by famous Cryptocurrency BlockChain like Bitcoin and Ethereum.

## What Is A Blockchain ?
A blockchain is a distributed database that is shared among the nodes of a computer network. 
As a database, a blockchain stores information electronically in digital format.

![img_1.png](img_1.png)


## What Is Proof of Work (PoW)?
It's a Hash that provide proof that the transaction is valid. Proof of work (PoW) is a form of cryptographic proof in 
which one party (the prover) proves to others (the verifiers) that a certain amount of a specific computational effort 
has been expended. Verifiers can subsequently confirm this expenditure with minimal effort on their part. 
The concept was invented by Cynthia Dwork and Moni Naor in 1993 as a way to deter denial-of-service attacks and other 
service abuses such as spam on a network by requiring some work from a service requester, usually meaning processing 
time by a computer.

Variant :

=>Challenge–response

![img_2.png](img_2.png)



=>Solution–verification

![img_3.png](img_3.png)



## How to use this code ? 
1. Open the folder as project in InteliJ.
2. Change all database connections filepaths to your local machine.
3. Run.
If you want to run multiple peers: 
1. Copy Paste the same folder and open the multiple folders in Intelij in paralel
2. Make sure they don't share database connection filepaths.
3. Change local peer port and peer client ports for each copy accordingly. (They are hard coded in the code)

