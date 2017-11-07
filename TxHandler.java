import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        UTXOPool uniqueUtxoPool = new UTXOPool();
        double sumInput = 0;
        double sumOutput = 0;

        //Looping tro all inputs
        for(int i = 0; i < tx.numInputs();i++){
            Transaction.Input txIn = tx.getInput(i); //Getting the input
            UTXO utxo = new UTXO(txIn.prevTxHash, txIn.outputIndex); // Creating new UTXO based on previous transaction
            Transaction.Output txOut = utxoPool.getTxOutput(utxo); // Getting the output of the previous transaction

            if(!utxoPool.contains(utxo)) return false; // Output is not in the pool.
            if(!Crypto.verifySignature(txOut.address,tx.getRawDataToSign(i),txIn.signature)) return false; // Verify the signature
            if(uniqueUtxoPool.contains(utxo)) return false; // Check if the unspend transaction already is done in the current pool.

            uniqueUtxoPool.addUTXO(utxo, txOut); // Add unspend transaction to list of current unspend transaction
            sumInput += txOut.value;

        }

        //Making sure that no output value is a negative number (4)
        for (Transaction.Output txOut : tx.getOutputs()) {
            if (txOut.value < 0) return false;
            sumOutput += txOut.value; //Calculating the total output value.
        }

        //Making sure the input is greater or equal then the output
        if(sumOutput <= sumInput) return true;
        else return false;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        Set<Transaction> validTx = new HashSet<>();

        //Going tro all transactions to validate them
        for(Transaction tx :possibleTxs){
            if (isValidTx(tx)) {
                validTx.add(tx); //Add to set of valid transactions

                //Removing the transactions input from the current pool
                for (Transaction.Input in : tx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }

                //Adding transaction outputs to current pool
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output out = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }

        //Return fixed size array of transactions
        Transaction[] validTxArray = new Transaction[validTx.size()];
        return validTx.toArray(validTxArray);
    }
}
