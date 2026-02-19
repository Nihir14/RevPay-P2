package com.revpay.util;

import java.math.BigDecimal;

public class NotificationUtil {

    public static String welcome(){
        return "Welcome to RevPay  Your wallet is ready.";
    }

    public static String walletCredited(BigDecimal amount){
        return "₹" + amount + " added to your wallet";
    }

    public static String walletDebited(BigDecimal amount){
        return "₹" + amount + " debited from your wallet";
    }

    public static String moneySent(BigDecimal amount){
        return "₹" + amount + " sent successfully";
    }

    public static String moneyReceived(BigDecimal amount){
        return "₹" + amount + " received";
    }

    public static String requestCreated(BigDecimal amount){
        return "Money request of ₹" + amount + " created";
    }

    public static String requestAccepted(BigDecimal amount){
        return "Your request of ₹" + amount + " was accepted";
    }

    public static String requestDeclined(){
        return "Your money request was declined";
    }

    public static String loanApplied(BigDecimal amount){
        return "Loan application submitted for ₹" + amount;
    }

    public static String loanApproved(){
        return "Your loan has been approved";
    }

    public static String loanRejected(){
        return "Your loan was rejected";
    }

    public static String loanRepayment(BigDecimal amount){
        return "Loan repayment of ₹" + amount + " successful";
    }

    public static String invoiceSent(BigDecimal amount){
        return "Invoice of ₹" + amount + " sent";
    }

    public static String invoicePaid(BigDecimal amount){
        return "Invoice of ₹" + amount + " paid";
    }

    public static String passwordChanged(){
        return "Your password was updated successfully";
    }

    public static String pinChanged(){
        return "Your transaction PIN was updated";
    }

    public static String businessVerified(){
        return "Your business account is verified ✅";
    }

    public static String cardAdded(){
        return "New card added successfully";
    }

    public static String cardDeleted(){
        return "Card removed successfully";
    }
}
