package student.inti.assignment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class IncomeAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Income> incomes;

    public IncomeAdapter(Context context, ArrayList<Income> incomes) {
        this.context = context;
        this.incomes = incomes;
    }

    @Override
    public int getCount() {
        return incomes.size();
    }

    @Override
    public Object getItem(int position) {
        return incomes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.income_item, parent, false);
        }

        Income income = (Income) getItem(position);

        TextView tvDescription = convertView.findViewById(R.id.tvDescription);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);
        ImageView ivCategoryIcon = convertView.findViewById(R.id.ivCategoryIcon);

        tvDescription.setText(income.getDescription());
        tvDate.setText(income.getDate());
        tvAmount.setText(String.format("RM%.2f", income.getAmount()));

        // Set the appropriate icon based on category
        switch (income.getCategory().toLowerCase()) {
            case "salary":
                ivCategoryIcon.setImageResource(R.drawable.ic_salary);
                break;
            case "pocket_money":
                ivCategoryIcon.setImageResource(R.drawable.ic_pocketmoney);
                break;
            case "part_time":
                ivCategoryIcon.setImageResource(R.drawable.ic_parttime);
                break;
            default:
                ivCategoryIcon.setImageResource(R.drawable.ic_default); // Default icon
                break;
        }

        return convertView;
    }
}
