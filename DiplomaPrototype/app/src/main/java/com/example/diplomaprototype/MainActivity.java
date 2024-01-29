package com.example.diplomaprototype;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.PeriodicWorkRequest;

import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener {
    private static MainActivity activity = null;
    private TextView monthYearText;
    private RecyclerView calendarRecycleView;
    private LocalDate selectedDate;
    private LocalDate currentDate;
    private DaysDataLoader daysData;
    private boolean weekMode;
    private View calendarDataView;
    private String saveFaculty = "CurrentFaculty";
    private String saveGroup = "CurrentGroup";
    private PeriodicWorkRequest periodicWorkRequest;
    private final int daysInWeek = 7;
    private final float calendarViewHeight = 0.83f;
    private final String scheduleHNPU = "ScheduleHNPU";
    private final String urlFaculties = "UrlHNPUFaculties";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        calendarDataView = findViewById(R.id.calendarDataView);
        setSupportActionBar(toolbar);
        initWidgets();
        Context context = this.getApplicationContext();
        daysData = DaysDataLoader.getInstance();
        SharedPreferences preferences = context.getSharedPreferences(scheduleHNPU, Context.MODE_PRIVATE);
        String scheduleUrl = preferences.getString(urlFaculties, getResources().getString(R.string.default_url));
        String faculty = preferences.getString(saveFaculty, "");
        String group = preferences.getString(saveGroup, "");
        String facultyUrl = preferences.getString(getResources().getString(R.string.faculty_url), "");

        daysData.currentFaculty = faculty;
        daysData.currentGroup = group;
        daysData.setBaseLoadUrl(scheduleUrl);
        daysData.setFacultyUrl(facultyUrl);
        selectedDate = LocalDate.now();
        currentDate = selectedDate;
        initialUpdate();
        if(faculty.equals("") && group.equals("")) {
            updateViews();
            calendarDataView.post(() -> settingsPopup());
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NotificationService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        activity = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        UpdateViews updateViews = new UpdateViews();
        String scheduleSave = this.getResources().getString(R.string.scheduleSave);
        daysData.Load(updateViews, this, scheduleSave, Looper.getMainLooper());
        super.onResume();
    }

    public class UpdateViews implements DaysDataLoader.UpdateUICallback {
        PopupWindow popupWindow = null;
        UpdateViews(PopupWindow popupWindow) {
            this.popupWindow = popupWindow;
        }
        UpdateViews() {
        }
        @Override
        public void callingBack(boolean isChanged) {
            if(isChanged) {
                updateViews();
            }
            if (null != popupWindow) {
                popupWindow.dismiss();
            }
        }
    }

    public class InitialUpdateViews implements DaysDataLoader.UpdateUICallback {
        private Context context;
        private UpdateViews updateViews = new UpdateViews();
        InitialUpdateViews(Context context) {
            this.context = context;
        }
        public void callingBack(boolean isChanged) {
            updateViews.callingBack(isChanged);
        }
    }

    public class SettingsDialogue implements DaysDataLoader.UpdateUICallback {
        private View popupView;
        SettingsDialogue(View popupView) {
            this.popupView = popupView;
        }
        @Override
        public void callingBack(boolean isChange) {
            updateSettingSnippets(popupView);
            Context context = MainActivity.this.getApplicationContext();
            SharedPreferences preferences = context.getSharedPreferences(scheduleHNPU, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(urlFaculties, daysData.getBaseLoadUrl());
            editor.commit();
        }
    }

    private void initialUpdate() {
        Context context = this.getApplicationContext();
        InitialUpdateViews initialUpdateViews = new InitialUpdateViews(context);
        String scheduleSave = context.getResources().getString(R.string.scheduleSave);
        try {
            FileInputStream inputStream = context.openFileInput(scheduleSave);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            daysData.LoadSchedule(inputStreamReader);
            inputStreamReader.close();
            initialUpdateViews.callingBack(true);
        } catch (FileNotFoundException e) {
            Log.e("LoaderWorker", "Fail to open file input stream");
        }
        catch (IOException e) {
            Log.e("LoaderWorker", "Fail load schedule");
        }

        if(daysData.GetDaysCount() == 0) {
            try {
                daysData.Load(initialUpdateViews, context, scheduleSave, Looper.getMainLooper());
            } catch (Exception e) {
                Log.e("MainActivity", "Fail to open file output stream");
            }
        }
    }

    public void updateViews() {
        if (weekMode) {
            setWeekView();
        } else {
            setMonthView();
        }
    }

    private void initWidgets() {
        calendarRecycleView = findViewById(R.id.calendarView);
        monthYearText = findViewById(R.id.mothYearTextView);
    }

    private void setMonthYearTextView(LocalDate date) {
        String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        monthYearText.setText(month.substring(0, 1).toUpperCase() + month.substring(1));
    }

    private void setWeekView() {
        setMonthYearTextView(selectedDate);
        CalendarAdapter adapter = new CalendarAdapter(daysData, currentDate, selectedDate, this, false);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), daysInWeek);
        calendarRecycleView.setLayoutManager(layoutManager);
        calendarRecycleView.setAdapter(adapter);
    }

    private void setMonthView() {
        setMonthYearTextView(selectedDate);
        CalendarAdapter adapter = new CalendarAdapter(daysData, currentDate, selectedDate, this, true);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), daysInWeek);
        calendarRecycleView.setLayoutManager(layoutManager);
        calendarRecycleView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void settingsPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.settings_dialogue, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = (int)(calendarDataView.getHeight() * calendarViewHeight);
        Button update = popupView.findViewById(R.id.apply_settings);
        update.setEnabled(false);
        TextView textView = popupView.findViewById(R.id.textView10);
        textView.setText(daysData.url);
        textView.setOnClickListener(v -> {
            onUrlClick(popupView, v);
        });

        SettingsDialogue settingsDialogue = new SettingsDialogue(popupView);
        daysData.PreLoad(settingsDialogue);
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        Context context = this.getApplicationContext();
        Spinner faculties = (Spinner) popupView.findViewById(R.id.faculties);
        Spinner group = (Spinner) popupView.findViewById(R.id.groups);
        update.setOnClickListener(v -> {
            daysData.currentFaculty = (String) faculties.getSelectedItem();
            daysData.currentGroup = (String) group.getSelectedItem();
            UpdateViews updateViews = new UpdateViews(popupWindow);
            try {
                String scheduleSave = context.getResources().getString(R.string.scheduleSave);
                daysData.Load(updateViews, context, scheduleSave, Looper.getMainLooper());
            } catch (Exception e) {
                Log.e("MainActivity", "Fail to open file output stream");
            }
            SharedPreferences preferences = context.getSharedPreferences(scheduleHNPU, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            if (faculties.isSelected()) {
                editor.putString(saveFaculty, faculties.getSelectedItem().toString());
            }
            if (group.isSelected()) {
                editor.putString(saveGroup, group.getSelectedItem().toString());
            }
            editor.putString(getResources().getString(R.string.faculty_url), daysData.getFacultyUrl());
            editor.commit();
        });

        popupWindow.showAtLocation(calendarDataView, Gravity.CENTER, 0, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_week_view) {
            weekMode = true;
            setWeekView();
        }
        else if (id == R.id.action_month_view) {
            weekMode = false;
            setMonthView();
        }
        else if (id == R.id.action_settings) {
            settingsPopup();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void OnItemClick(int position, String dayText) {
        //TODO: bug on update dialoue trigger wrong update of main views
        int dayPosition = position;
        int dayNumber = Integer.valueOf(dayText);
        if(weekMode) {
            dayPosition = dayNumber;
        }
        LocalDate selectedDay = CalendarAdapter.getDayOfMonthFromPosition(selectedDate, dayPosition);
        HashMap<LocalDateTime, Pair<String, String>> data = daysData.GetCurrentsDaySchedule(selectedDay, dayNumber);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        DialogueHandler.CreateDialogue(inflater, data, calendarDataView, selectedDay, dayText);
    }

    public void previousMonthAction(View view) {
        if (weekMode) {
            selectedDate = selectedDate.minusWeeks(1);
            setWeekView();
        }
        else {
            selectedDate = selectedDate.minusMonths(1);
            setMonthView();
        }
    }

    public void nextMonthAction(View view) {
        if (weekMode) {
            selectedDate = selectedDate.plusWeeks(1);
            setWeekView();
        }
        else
        {
            selectedDate = selectedDate.plusMonths(1);
            setMonthView();
        }
    }

    private void updateSettingSnippets(View popupView) {
        int textColor = this.getColor(R.color.textColor);
        int backgroundColor =  this.getColor(R.color.white);
        List<String> groups = new ArrayList<String>();
        ArrayAdapter<String> adapterGroup = new ArrayAdapter<String>(popupView.getContext(), android.R.layout.simple_spinner_dropdown_item, groups) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(textColor);
                return v;
            }
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(backgroundColor);
                return v;
            }
        };
        Spinner faculties = (Spinner) popupView.findViewById(R.id.faculties);
        faculties.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String faculty = daysData.listOfFaculties.get(position);
                if (!faculty.isEmpty()) {
                    adapterGroup.clear();
                    adapterGroup.addAll(daysData.facultiesAndGroups.get(faculty));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<String> adapterFaculties = new ArrayAdapter<String>(popupView.getContext(), android.R.layout.simple_spinner_dropdown_item, daysData.listOfFaculties) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(textColor);
                return v;
            }
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(backgroundColor);
                return v;
            }
        };
        faculties.setAdapter(adapterFaculties);
        Spinner group = (Spinner) popupView.findViewById(R.id.groups);
        group.setAdapter(adapterGroup);
        Button update = popupView.findViewById(R.id.apply_settings);

        update.setEnabled(true);
    }

    public void onUrlClick(View popupView, View view) {
        TextView currentText = (TextView)view;
        Context context = view.getContext();
        AlertDialog.Builder newUrlDialogue = new AlertDialog.Builder(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        newUrlDialogue.setTitle("Новий адрес");
        final EditText edittext = new EditText(context);
        edittext.setText(currentText.getText());
        layout.addView(edittext);
        newUrlDialogue.setView(layout);
        newUrlDialogue.setPositiveButton("Зберегти", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                currentText.setText(edittext.getText());
                daysData.setBaseLoadUrl(edittext.getText().toString());
                SettingsDialogue settingsDialogue = new SettingsDialogue(popupView);
                daysData.PreLoad(settingsDialogue);
            }
        });
        newUrlDialogue.setNegativeButton("Відмінити", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //NOTE: do nothing
            }
        });
        newUrlDialogue.show();
    }
}