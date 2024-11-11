package student.inti.assignment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ExpenseAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Expense> expenses;

    public ExpenseAdapter(Context context, ArrayList<Expense> expenses) {
        this.context = context;
        this.expenses = expenses;
    }

    @Override
    public int getCount() {
        return expenses.size();
    }

    @Override
    public Object getItem(int position) {
        return expenses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.expense_item, parent, false);
        }

        Expense expense = (Expense) getItem(position);

        TextView tvDescription = convertView.findViewById(R.id.tvDescription);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);
        ImageView ivCategoryIcon = convertView.findViewById(R.id.ivCategoryIcon);

        tvDescription.setText(expense.getDescription());
        tvDate.setText(expense.getDate());  // Set the date
        tvAmount.setText(String.format("RM%.2f", expense.getAmount()));

        // Set the appropriate icon based on category
        switch (expense.getCategory().toLowerCase()) {
            case "food":
                ivCategoryIcon.setImageResource(R.drawable.ic_food);
                break;
            case "transport":
                ivCategoryIcon.setImageResource(R.drawable.ic_transport);
                break;
            case "education":
                ivCategoryIcon.setImageResource(R.drawable.ic_education);
                break;
            case "parking":
                ivCategoryIcon.setImageResource(R.drawable.ic_parking);
                break;
            case "travel":
                ivCategoryIcon.setImageResource(R.drawable.ic_travel);
                break;
            case "shopping":
                ivCategoryIcon.setImageResource(R.drawable.ic_shopping);
                break;
            case "dessert":
                ivCategoryIcon.setImageResource(R.drawable.ic_dessert);
                break;
            case "entertainment":
                ivCategoryIcon.setImageResource(R.drawable.ic_entertainment);
                break;
            default:
                ivCategoryIcon.setImageResource(R.drawable.ic_default); // Default icon
                break;
        }

        return convertView;
    }
}