package Accounts;

import Bank.Bank;

/**
 * SavingsAccount class representing a standard savings account with balance tracking.
 * It allows deposits, withdrawals, and fund transfers while enforcing banking rules.
 */
public class SavingsAccount extends Account implements Withdrawal, Deposit, FundTransfer {
    // The current balance of the savings account
    private double balance;

    /**
     * Constructor for SavingsAccount.
     *
     * @param bank - The bank associated with this savings account.
     * @param accountNumber - The unique account number.
     * @param ownerFname - Owner's first name.
     * @param ownerLname - Owner's last name.
     * @param ownerEmail - Owner's email address.
     * @param pin - Security PIN for authentication.
     * @param balance - The initial deposit amount.
     * @throws IllegalArgumentException If the initial deposit is below 0.
     */
    public SavingsAccount(Bank bank, String accountNumber, String pin, String ownerFname,
                          String ownerLname, String ownerEmail, double balance) {
        super(bank, accountNumber, pin, ownerFname, ownerLname, ownerEmail);
        if (balance < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }
        this.balance = balance;
    }

    /**
     * Get the account balance statement of this savings account.
     * 
     * @return - String balance statement.
     */
    public String getAccountBalanceStatement() {
        return String.format("SavingsAccount{Account Number: %s, Owner: %s, Balance: Php %.2f}", 
                                    this.accountNumber, getOwnerFullName(), this.balance);
    }

    /**
     * Validates whether this savings account has enough balance to proceed with such a transaction
     * based on the amount that is to be adjusted.
     * 
     * @param amount - Amount of money to be supposedly adjusted from this account’s balance.
     * @return - Flag if transaction can proceed by adjusting the account balance by the amount to be
     * changed.
     */
    public boolean hasEnoughBalance(double amount) {
        return balance >= amount;
    }

    /**
     * Warns the account owner that their balance is not enough for the transaction to proceed
     * successfully.
     */
    public void insufficientBalance() {
        System.out.println("Warning: Insufficient balance to complete the transaction.");
    }

    /**
     * Adjust the account balance of this savings account based on the amount to be adjusted. If it
     * results to the account balance going less than 0.0, then it is forcibly reset to 0.0.
     * 
     * @param amount - Amount to be added or subtracted from the account balance.
     */
    public void adjustAccountBalance(double amount) {
        this.balance += amount;
        if (this.balance < 0) {
            this.balance = 0.0;
        }
    }

    /**
     * Deposit some cash into this account. Cannot be greater than the bank’s deposit limit.
     * 
     * @param amount – Amount of money to be deposited.
     * @return Flag if transaction is successful or not.
     */
    @Override
    public boolean cashDeposit(double amount) {
        if (amount > bank.getDepositLimit()) {
            System.out.println("Deposit amount exceeds the bank's limit.");
            return false;
        }
        this.adjustAccountBalance(amount);

        // Ensure a transaction log is added for the deposit
        this.addNewTransaction(this.getAccountNumber(), Transaction.Transactions.DEPOSIT,
                "Deposited Php " + amount);

        return true;
    }

    /**
     * Withdraw an amount of money from this savings account. Cannot proceed if account does not
     * have sufficient balance.
     * 
     * @param amount – Amount of money to be withdrawn.
     */
    @Override
    public boolean withdrawal(double amount) {
        if (amount <= 0 || amount > balance || amount > bank.getWithdrawLimit()) {
            insufficientBalance();
            return false; // Cannot withdraw more than available balance or withdrawal limit
        }

        // Adjust balance and log transaction
        adjustAccountBalance(-amount);
        addNewTransaction(accountNumber, Transaction.Transactions.WITHDRAWAL,
                String.format("Withdraw Php %.2f", amount));

        return true;
    }

    /**
     * Transfers an amount of money from this account to another savings account. Is extensively used
     * by the other transfer() method
     * <br><br>
     * Cannot proceed if one of the following is true:
     * <ul>
     *     <li>Insufficient balance from source account.</li>
     *     <li>Recepient account does not exist.</li>
     *     <li>Recepient account is from another bank.</li>
     * </ul>
     * @param account – Account number of recipient
     * @param amount – Amount of money to be supposedly adjusted from this account’s balance.
     * @throws IllegalAccountType Cannot fund transfer when the other account is of type
     * CreditAccount.
     * @return Flag if fund transfer transaction is successful or not.
     */
    @Override
    public boolean transfer(Account recipient, double amount) throws IllegalAccountType {
        if (!(recipient instanceof SavingsAccount)) {
            throw new IllegalAccountType("Cannot transfer funds to a CreditAccount.");
        }

        if (!hasEnoughBalance(amount) || amount <= 0 || amount > bank.getWithdrawLimit()) {
            insufficientBalance();
            return false;
        }

        // Deduct from sender and add to recipient
        adjustAccountBalance(-amount);
        ((SavingsAccount) recipient).adjustAccountBalance(amount);

        // Log transactions for both accounts
        addNewTransaction(recipient.getAccountNumber(), Transaction.Transactions.FUNDTRANSFER,
                String.format("Transferred Php %.2f to %s", amount, recipient.getAccountNumber()));
        recipient.addNewTransaction(accountNumber, Transaction.Transactions.RECEIVE_TRANSFER,
                String.format("Received Php %.2f from %s", amount, accountNumber));

        return true;
    }

    /**
     * Transfers an amount of money from this account to another savings account.
     * Should be used when transferring to other banks.
     *
     * @param bank Bank object of the recipient
     * @param account Account number of recipient
     * @param amount Amount of money to be supposedly adjusted from this account's balance
     * @return Flag if fund transfer transaction is successful or not
     * @throws IllegalAccountType Cannot fund transfer when the other account is of type CreditAccount
     */
    @Override
    public boolean transfer(Bank recipientBank, Account recipient, double amount) throws IllegalAccountType {
        if (!(recipient instanceof SavingsAccount)) {
            throw new IllegalAccountType("Cannot transfer funds to a CreditAccount.");
        }

        double totalAmount = amount + bank.getProcessingFee();

        if (!hasEnoughBalance(totalAmount) || amount <= 0 || totalAmount > bank.getWithdrawLimit()) {
            insufficientBalance();
            return false; // Insufficient funds or exceeding withdrawal limit
        }

        // Deduct full amount from sender including processing fee
        adjustAccountBalance(-totalAmount);

        // Credit only the transferred amount (not including fee) to recipient
        ((SavingsAccount) recipient).adjustAccountBalance(amount);

        // Log transactions for both accounts
        addNewTransaction(recipient.getAccountNumber(), Transaction.Transactions.EXTERNAL_TRANSFER,
                String.format("Transferred Php %.2f to %s at %s (Fee: Php %.2f)", 
                                    amount, recipient.getAccountNumber(), recipientBank.getName(), bank.getProcessingFee()));

        recipient.addNewTransaction(accountNumber, Transaction.Transactions.RECEIVE_TRANSFER,
                String.format("Received Php %.2f from %s at %s", amount, this.accountNumber, bank.getName()));

        return true;
    }

    public double getAccountBalance() {
        return this.balance;
    }
}
