package com.example.diplomaprototype;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.text.LineBreaker;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public final TasksArrayView tasks = new TasksArrayView();
    public final TextView dayOfMonth;
    private final CalendarAdapter.OnItemListener listener;
    private View thisView;

    public CalendarViewHolder(@NonNull View itemView, CalendarAdapter.OnItemListener InListener) {
        super(itemView);
        dayOfMonth = itemView.findViewById(R.id.cellDayTextView);

        listener = InListener;
        thisView = itemView;
        itemView.setOnClickListener(this);
    }

    class TasksArrayView extends ArrayList<TextView> {
        private LinearLayout linearLayout;
        public TasksArrayView() {
            linearLayout = itemView.findViewById(R.id.textsLayout);
        }

        @Override
        public TextView get(int index) {
            int size = index + 1;
            if(size > this.size()) {
                int lackOf = size - this.size();
                int textColor = thisView.getContext().getColor(R.color.textColor);
                for(int i = 0; i < lackOf; i++) {
                    TextView view = new TextView(thisView.getContext());
                    view.setSingleLine();
                    view.setTextSize(14);
                    view.setTextColor(textColor);
                    view.setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED);
                    view.setFreezesText(true);
                    linearLayout.addView(view);
                    this.add(view);
                }
            }
            return super.get(index);
        }

        public void reset() {
            if(null != linearLayout) {
                linearLayout.removeAllViews();
            }
            this.clear();
        }
    }

    @Override
    public void onClick(View v) {
        listener.OnItemClick(getAdapterPosition(), (String) dayOfMonth.getText());
    }

    public void setBackgroundColor(int color) {
        Drawable drawable = thisView.getBackground();
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }

    public void reset() {
        tasks.reset();
    }
}
