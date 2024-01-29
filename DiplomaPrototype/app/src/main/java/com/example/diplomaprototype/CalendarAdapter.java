package com.example.diplomaprototype;

import android.graphics.Color;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


public class CalendarAdapter extends RecyclerView.Adapter<CalendarViewHolder> {
    private final CalendarAdapter.OnItemListener listener;
    private boolean modeMonth;
    private DaysDataLoader daysData;
    private LocalDate selectedDate;
    private LocalDate currentDate;
    private final int numberDay = 1;
    private final int daysInMonth = 42;
    private final int daysInWeek = 7;
    private final double viewHeight = 0.1666666;
    public CalendarAdapter(DaysDataLoader daysData,
                           LocalDate currentDate,
                           LocalDate selectedDate,
                           CalendarAdapter.OnItemListener inListener,
                           boolean modeMonth) {
        this.selectedDate = selectedDate;
        this.currentDate = currentDate;
        this.daysData = daysData;
        listener = inListener;
        this.modeMonth = modeMonth;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (modeMonth) {
            params.height = (int) (parent.getHeight() * viewHeight);
        }
        return new CalendarViewHolder(view, listener);
    }

    private void handleMonth(@NonNull CalendarViewHolder holder, int position) {
        LocalDate firstDayOfMonth = selectedDate.withDayOfMonth(numberDay);
        int dayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() - numberDay;
        int daysInMonth = selectedDate.lengthOfMonth();
        int dayOfMouth = position - dayOfWeek + numberDay;
        int upperDayRange = daysInMonth + dayOfWeek;
        int lowerDayRange = dayOfWeek;
        if(position < lowerDayRange) {
            holder.setBackgroundColor(Color.GRAY);
            LocalDate localDate = firstDayOfMonth.minusDays(lowerDayRange - position);
            int localDay = localDate.getDayOfMonth();
            holder.dayOfMonth.setText(Integer.toString(localDay));
        }
        else if (position >= upperDayRange) {
            int afterDay = position - upperDayRange + numberDay;
            holder.setBackgroundColor(Color.GRAY);
            holder.dayOfMonth.setText(Integer.toString(afterDay));
        }
        else
        {
            if(currentDate.isEqual(selectedDate.withDayOfMonth(dayOfMouth))) {
                holder.setBackgroundColor(Color.BLUE);
            }
            HashMap<LocalDateTime, Pair<String, String>> data = daysData.GetCurrentsDaySchedule(selectedDate, dayOfMouth);
            String dayOfMouthString = Integer.valueOf(dayOfMouth).toString();

            holder.dayOfMonth.setText(dayOfMouthString);
            int currentTaskIndex = 0;
            if (data != null) {
                for (Map.Entry<LocalDateTime, Pair<String, String>> entry : data.entrySet()) {
                    TextView view = holder.tasks.get(currentTaskIndex);
                    currentTaskIndex++;
                    view.setVisibility(View.VISIBLE);
                    view.setText(entry.getValue().first);
                }
            }
        }
    }

    private void handleWeek(@NonNull CalendarViewHolder holder, int position) {
        int dayOfWeek = selectedDate.getDayOfWeek().getValue();
        int dayShift = position - dayOfWeek + 1;
        int dayOfMonth = selectedDate.getDayOfMonth() + dayShift;
        int monthLength = selectedDate.getMonth().length(false);
        if (dayOfMonth > monthLength) {
            dayOfMonth = dayOfMonth - monthLength;
            selectedDate.plusMonths(1);
        }
        LocalDate currentMonth = selectedDate;
        if (dayOfMonth <= 0) {
            currentMonth = selectedDate.minusMonths(1);
            dayOfMonth = currentMonth.getMonth().length(false) + dayShift + numberDay;
        }
        HashMap<LocalDateTime, Pair<String, String>> data = daysData.GetCurrentsDaySchedule(selectedDate, dayOfMonth);
        holder.dayOfMonth.setText(Integer.valueOf(dayOfMonth).toString());

        if (currentDate.isEqual(currentMonth.withDayOfMonth(dayOfMonth))) {
            holder.setBackgroundColor(Color.BLUE);
        }

        if (null != data) {
            int currentTaskIndex = 0;
            for (Map.Entry<LocalDateTime, Pair<String, String>> entry : data.entrySet()) {
                TextView timeView = holder.tasks.get(currentTaskIndex);
                currentTaskIndex++;
                timeView.setVisibility(View.VISIBLE);
                String time = entry.getKey().format(DateTimeFormatter.ofPattern("HH:mm")).toString();
                timeView.setText(time);
                TextView view = holder.tasks.get(currentTaskIndex);
                view.setVisibility(View.VISIBLE);
                view.setText(entry.getValue().first);
                currentTaskIndex++;
            }
        }
    }

    public static LocalDate getDayOfMonthFromPosition(LocalDate selectedDate, int position) {
        LocalDate date = null;
        LocalDate firstDayOfMonth = selectedDate.withDayOfMonth(1);
        int dayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() - 1;
        int daysInMonth = selectedDate.lengthOfMonth();
        int dayOfMouth = position - dayOfWeek + 1;
        int upperDayRange = daysInMonth + dayOfWeek;
        int lowerDayRange = dayOfWeek;
        if(position < lowerDayRange) {
            date = firstDayOfMonth.minusDays(lowerDayRange - position);
        }
        else if (position >= upperDayRange) {
            int day = upperDayRange - position + 1;
            date = selectedDate.plusMonths(1).withDayOfMonth(day);
        }
        else {
            date = selectedDate.withDayOfMonth(dayOfMouth);
        }
        return date;
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        holder.reset();
        if(modeMonth) {
            handleMonth(holder, position);
        }
        else {
            handleWeek(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        if(modeMonth) {
            return daysInMonth;
        }
        return daysInWeek;
    }

    public interface OnItemListener {
        void OnItemClick(int position, String dayText);
    }
}
