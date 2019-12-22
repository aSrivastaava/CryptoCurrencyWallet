package subchain;
import java.security.Security;
import java.util.*;

public class Subchain {
	
	public static ArrayList<Block> blockchain = new ArrayList<Block>();
	public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();
	
	public static int difficulty = 3;
	public static float minimumTransaction = 0.1f;
	public static Wallet walletA;
	public static Wallet walletB;
	public static Transaction genesisTransaction;

	public static void main(String[] args) {	

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Setup Bouncey castle as a Security Provider
		
		Scanner scn = new Scanner (System.in);
		
		//Create wallets:
		walletA = new Wallet();
		walletB = new Wallet();		
		Wallet coinbase = new Wallet();
	
		float amount,t1,t2;
		int choice = 0;
		
		
		
		// menu display 
		try{
			do{
			System.out.println("Enter the choice number you wnat to do");
			System.out.println("1. To Set the amount of WalletA \n 2. Transfer funds to WalletA to WalletB \n 3. Transfer funds to WalletB to WalletA \n ");
			choice = scn.nextInt();
			
			
				if(choice == 1){ 
					System.out.println("Enter the initial amaount in WalletA: ");
					amount = scn.nextFloat();
					
					//create genesis transaction, which sends 100 subCoin to walletA: 
					
					genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, amount, null);
					genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction	
					genesisTransaction.transactionId = "0"; //manually set the transaction id
					genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //manually add the Transactions Output
					UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.
					
					System.out.println("Creating and Mining Genesis block... ");
					Block genesis = new Block("0");
					genesis.addTransaction(genesisTransaction);
					addBlock(genesis);
					}
				else if(choice==2){
					Block genesis = new Block(null);
					
					Block block1 = new Block(genesis.hash);
					System.out.println("\nWalletA's balance is: " + walletA.getBalance());
					System.out.println("Enter the amount you want to transfer");
					t1 = scn.nextFloat();
					System.out.println("\nWalletA is Attempting to send funds  to WalletB...");
					block1.addTransaction(walletA.sendFunds(walletB.publicKey, t1));
					addBlock(block1);
					System.out.println("\nWalletA's balance is: " + walletA.getBalance());
					System.out.println("WalletB's balance is: " + walletB.getBalance());
					
				}
				else if(choice==3){
					Block block1 = new Block(null);
					
					Block block2 = new Block(block1.hash);
					System.out.println("Enter the amount you want to transfer");
					t2 = scn.nextFloat();
					System.out.println("\nWalletB is Attempting to send funds  to WalletA...\n\n");
					block2.addTransaction(walletB.sendFunds( walletA.publicKey, t2));
					System.out.println("\n\nWalletA's balance is: " + walletA.getBalance());
					System.out.println("WalletB's balance is: " + walletB.getBalance());
					
				}
				else{
					System.out.println("Invalid input..");
					continue;
				}
				
				isChainValid();

		}while(true);
		}
		catch(InputMismatchException e) {
			System.out.println("Input should be INT datatype only");
			System.out.println("The program will terminate due to some security issue\nKindly Try again..\n\n");
		}
		scn.close();
	}
	
	public static Boolean isChainValid() {
		Block currentBlock; 
		Block previousBlock;
		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		//loop through blockchain to check hashes:
		for(int i=1; i < blockchain.size(); i++) {
			
			currentBlock = blockchain.get(i);
			previousBlock = blockchain.get(i-1);
			//compare registered hash and calculated hash:
			if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
				System.out.println("#Current Hashes not equal");
				return false;
			}
			
			//check if hash is solved
			if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
				System.out.println("#This block hasn't been mined");
				return false;
			}
			
			//loop thru blockchains transactions:
			TransactionOutput tempOutput;
			for(int t=0; t <currentBlock.transactions.size(); t++) {
				Transaction currentTransaction = currentBlock.transactions.get(t);
				
				if(!currentTransaction.verifySignature()) {
					System.out.println("#Signature on Transaction(" + t + ") is Invalid");
					return false; 
				}
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
					return false; 
				}
				
				for(TransactionInput input: currentTransaction.inputs) {	
					tempOutput = tempUTXOs.get(input.transactionOutputId);
					
					if(tempOutput == null) {
						System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
						return false;
					}
					
					if(input.UTXO.value != tempOutput.value) {
						System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
						return false;
					}
					
					tempUTXOs.remove(input.transactionOutputId);
				}
				
				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}
				
				if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
					System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
					return false;
				}
				if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
					System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
					return false;
				}
				
			}
			
		}
		System.out.println("Blockchain is valid");
		return true;
	}
	
	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty);
		blockchain.add(newBlock);
	}
}
