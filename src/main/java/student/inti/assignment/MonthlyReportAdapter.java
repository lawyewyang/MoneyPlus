package student.inti.assignment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MonthlyReportAdapter extends RecyclerView.Adapter<MonthlyReportAdapter.ViewHolder> {
    private final List<MonthlyReport> reportList;

    public MonthlyReportAdapter(List<MonthlyReport> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monthly_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthlyReport report = reportList.get(position);
        holder.tvMonth.setText(report.getMonth());
        holder.tvExpense.setText(String.format("Expense: RM%.2f", report.getTotalExpense()));
        holder.tvIncome.setText(String.format("Income: RM%.2f", report.getTotalIncome()));
        holder.tvBalance.setText(String.format("Balance: RM%.2f", report.getBalance()));

        // Set color for balance
        int color = report.getBalance() >= 0 ?
                ContextCompat.getColor(holder.itemView.getContext(), R.color.positive_balance) :
                ContextCompat.getColor(holder.itemView.getContext(), R.color.negative_balance);
        holder.tvBalance.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvExpense, tvIncome, tvBalance;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvExpense = itemView.findViewById(R.id.tvExpense);
            tvIncome = itemView.findViewById(R.id.tvIncome);
            tvBalance = itemView.findViewById(R.id.tvBalance);
        }
    }
}