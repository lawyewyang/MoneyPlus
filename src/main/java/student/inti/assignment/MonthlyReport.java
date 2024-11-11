package student.inti.assignment;

public class MonthlyReport {
    private String month;
    private double totalExpense;
    private double totalIncome;
    private double balance;

    public MonthlyReport(String month, double totalExpense, double totalIncome, double balance) {
        this.month = month;
        this.totalExpense = totalExpense;
        this.totalIncome = totalIncome;
        this.balance = balance;
    }

    // Getters
    public String getMonth() { return month; }
    public double getTotalExpense() { return totalExpense; }
    public double getTotalIncome() { return totalIncome; }
    public double getBalance() { return balance; }
}
