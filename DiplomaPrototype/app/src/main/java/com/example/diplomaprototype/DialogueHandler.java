package com.example.diplomaprototype;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DialogueHandler {
    private static final float viewHeight = 0.83f;
    private final static String userNote = "userNote";

    public static void CreateDialogue(LayoutInflater inflater, HashMap<LocalDateTime, Pair<String, String>> data,
                                      View calendarDataView,
                                      LocalDate selectedDate, String dayText) {
        View popupView = inflater.inflate(R.layout.day_popup, null);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = (int)(calendarDataView.getHeight() * viewHeight);
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
        TextView dayView = popupWindow.getContentView().findViewById(R.id.popupDayView);
        dayView.setText(dayText);
        HashMap<LocalDateTime, String> userData = new HashMap<>();
        ArrayList<String> saveStream = new ArrayList<>();
        TableLayout tableLayout = popupWindow.getContentView().findViewById(R.id.popup_table);

        Context context = popupWindow.getContentView().getContext();
        int textColor = context.getColor(R.color.textColor);
        dayView.setTextColor(textColor);
        String scheduleNotes = context.getResources().getString(R.string.scheduleNotes);
        try {
            FileInputStream inputStream = context.openFileInput(scheduleNotes);
            LoadUserNodeTask(selectedDate, data, userData, inputStream, saveStream);
            inputStream.close();
        }
        catch (Exception e) {
            Log.e("DialogueHandler:", e.toString());
        }
        UpdateDataDialogue updateData = new UpdateDataDialogue(data);
        if (data != null) {
            for (Map.Entry<LocalDateTime, Pair<String, String>> entry : data.entrySet()) {
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                TextView rowTextTime = new TextView(context);
                rowTextTime.setTextColor(textColor);
                String time = entry.getKey().format(DateTimeFormatter.ofPattern("HH:mm")).toString();
                rowTextTime.setText(time + " ");
                rowTextTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
                row.addView(rowTextTime);
                TextView rowTextLesson = new TextView(context);
                rowTextLesson.setTextColor(textColor);
                rowTextLesson.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
                rowTextLesson.setText(entry.getValue().first + " ");
                row.addView(rowTextLesson);
                TextView note = new TextView(context);
                note.setTextColor(textColor);
                note.setText(entry.getValue().second);
                TextView placeHolder = new TextView(context);
                placeHolder.setTextColor(textColor);

                LinearLayout rowNote = new LinearLayout(context);
                rowNote.setOrientation(LinearLayout.HORIZONTAL);

                rowNote.addView(placeHolder);
                rowNote.addView(note);
                row.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        BuildTextEditDialogue(v, null, entry.getValue().first, entry.getKey(),
                                updateData, note, context);
                    }
                });
                tableLayout.addView(row);
                tableLayout.addView(rowNote);
            }
        }

        UpdateUserDataDialogue updateUserData = new UpdateUserDataDialogue(userData);
        for (Map.Entry<LocalDateTime, String> entry : userData.entrySet()) {
            LocalDate localTime = entry.getKey().toLocalDate();
            if (!selectedDate.equals(localTime)) {
                continue;
            }
            LinearLayout newRow = new LinearLayout(context);
            newRow.setOrientation(LinearLayout.HORIZONTAL);
            TextView timeText = new TextView(context);
            timeText.setTextColor(textColor);
            TextView textView = new TextView(context);
            textView.setTextColor(textColor);
            timeText.setTextSize(28);
            textView.setTextSize(28);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String noteTime =  entry.getKey().format(formatter);
            timeText.setText(noteTime + " ");
            textView.setText(entry.getValue());
            newRow.addView(timeText);
            newRow.addView(textView);
            newRow.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    BuildTextEditDialogue(newRow, tableLayout, timeText.getText().toString(),
                            entry.getKey(), updateUserData, textView, context);
                }
            });
            tableLayout.addView(newRow);
        }
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button newRowBtn = new Button(context);
        newRowBtn.setGravity(Gravity.CENTER);
        row.setGravity(Gravity.CENTER);
        newRowBtn.setText("+");
        newRowBtn.setTextColor(textColor);
        newRowBtn.setTextSize(22);
        newRowBtn.setBackgroundResource(R.drawable.roundedbutton);
        newRowBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BuildNewTextEditDialogue(row, tableLayout, selectedDate, updateUserData, context);
            }
        });
        row.addView(newRowBtn);
        tableLayout.addView(row);

        popupWindow.showAtLocation(calendarDataView, Gravity.CENTER, 0, 0);

        Button cancel = popupWindow.getContentView().findViewById(R.id.popup_cancel);
        Button save = popupWindow.getContentView().findViewById(R.id.popup_save);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    FileOutputStream outputStream = context.openFileOutput(scheduleNotes, context.MODE_PRIVATE);
                    SaveUserNodeTask(selectedDate, saveStream, data, userData, outputStream);
                    outputStream.close();
                }
                catch (Exception e) {
                    Log.e("DialogueHandler:", e.toString());
                }
                popupWindow.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }

    private static void BuildNewTextEditDialogue(View row, TableLayout table, LocalDate localDate,
                                                 UpdateDataDialogueInterface updateData, Context context) {
        AlertDialog.Builder noteDialog = new AlertDialog.Builder(context);
        int textColor = context.getColor(R.color.textColor);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView timeTextView = new TextView(context);
        timeTextView.setTextColor(textColor);
        timeTextView.setText("12:00");
        TimePickerDialog pickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                timeTextView.setText(String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute));
            }
        }, 24, 0, true);

        timeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickerDialog.show();
            }
        });


        final EditText editText = new EditText(context);
        LinearLayout timeLayout = new LinearLayout(context);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView layoutTitle = new TextView(context);
        layoutTitle.setTextColor(textColor);
        layoutTitle.setText("Час:");
        timeLayout.addView(layoutTitle);
        timeLayout.addView(timeTextView);
        layout.addView(timeLayout);
        layout.addView(editText);

        noteDialog.setView(layout);
        noteDialog.setPositiveButton("Зберегти", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(editText.getText().length() > 0) {
                    table.removeView(row);
                    String[] noteTime = timeTextView.getText().toString().split(":");
                    int hours = Integer.parseInt(noteTime[0]);
                    int minutes = Integer.parseInt(noteTime[1]);
                    LocalTime localTime = LocalTime.of(hours, minutes);
                    LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);

                    LinearLayout newRow = new LinearLayout(context);
                    newRow.setOrientation(LinearLayout.HORIZONTAL);
                    TextView timeText = new TextView(context);
                    timeText.setTextColor(textColor);
                    TextView textView = new TextView(context);
                    textView.setTextColor(textColor);
                    timeText.setTextSize(28);
                    textView.setTextSize(28);
                    timeText.setText(timeTextView.getText() + " ");
                    textView.setText(editText.getText());
                    newRow.addView(timeText);
                    newRow.addView(textView);
                    updateData.update(localDateTime, editText.getText().toString());
                    newRow.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            BuildTextEditDialogue(newRow, table, timeTextView.getText().toString(),
                                                  localDateTime, updateData, textView, context);
                        }
                    });
                    table.addView(newRow);
                    table.addView(row);
                }
            }
        });
        noteDialog.setNegativeButton("Відмінити", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //NOTE: do nothing
            }
        });
        noteDialog.show();
        pickerDialog.show();
    }

    private static void BuildTextEditDialogue(View view, TableLayout table, String title,
                                              LocalDateTime date,
                                              UpdateDataDialogueInterface updateData,
                                              TextView note, Context context) {
        AlertDialog.Builder noteDialog = new AlertDialog.Builder(context);
        int textColor = context.getColor(R.color.textColor);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        noteDialog.setTitle(title);
        final EditText edittext = new EditText(context);
        edittext.setTextColor(textColor);
        edittext.setText(note.getText());
        layout.addView(edittext);
        noteDialog.setView(layout);
        noteDialog.setPositiveButton("Зберегти", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (table != null) {
                    if(edittext.getText().length() == 0) {
                        table.removeView(view);
                    }
                }
                else {
                    note.setText(edittext.getText());
                }
                updateData.update(date, edittext.getText().toString());
            }
        });
        noteDialog.setNegativeButton("Відмінити", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //NOTE: do nothing
            }
        });
        noteDialog.show();
    }

    private static void LoadUserNodeTask(LocalDate selectedDate,
                                         HashMap<LocalDateTime, Pair<String, String>> data,
                                         HashMap<LocalDateTime, String> userData,
                                         FileInputStream inputStream,
                                         ArrayList<String> saveStream) throws IOException {
        BufferedReader page = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = page.readLine()) != null) {
            Log.i("DialogueHandler", line);
            String[] rowData = line.split(",");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(rowData[0], dateFormatter);
            if(selectedDate != date) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                if (rowData.length == 3) { //Handle user note for tasks
                    LocalDateTime dateTime = LocalDateTime.parse(rowData[1], formatter);
                    if (null == data.get(dateTime)) {

                        continue;
                    }
                    Pair<String, String> currentData = new Pair<>(data.get(dateTime).first, rowData[2]);
                    data.replace(dateTime, currentData);
                } else if (rowData.length == 4) { //Handle user note
                    LocalDateTime dateTime = LocalDateTime.parse(rowData[1], formatter);
                    userData.put(dateTime, rowData[2]);
                }
            }
            else {
                saveStream.add(line);
                saveStream.add("\n");
            }
        }
        page.close();
    }

    private static void SaveUserNodeTask(LocalDate selectedDate,
                                         ArrayList<String> saveStream,
                                         HashMap<LocalDateTime, Pair<String, String>> data,
                                         HashMap<LocalDateTime, String> userData,
                                         FileOutputStream outputStream) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (int i = 0; i < saveStream.size(); i++) {
            outputStream.write(saveStream.get(i).getBytes());
        }
        if (null != data) {
            for (HashMap.Entry<LocalDateTime, Pair<String, String>> set : data.entrySet()) {
                if (set.getValue().second != "") {
                    outputStream.write(selectedDate.toString().getBytes());
                    outputStream.write(",".getBytes());
                    outputStream.write(set.getKey().format(formatter).toString().getBytes());
                    outputStream.write(",".getBytes());
                    outputStream.write(set.getValue().second.getBytes());
                    outputStream.write("\n".getBytes());
                }
            }
        }
        if (null != userData) {
            for (HashMap.Entry<LocalDateTime, String> set : userData.entrySet()) {
                outputStream.write(selectedDate.toString().getBytes());
                outputStream.write(",".getBytes());
                outputStream.write(set.getKey().format(formatter).toString().getBytes());
                outputStream.write(",".getBytes());
                outputStream.write(set.getValue().getBytes());
                outputStream.write(",".getBytes());
                outputStream.write(userNote.getBytes());
                outputStream.write("\n".getBytes());
            }
        }
    }
}
