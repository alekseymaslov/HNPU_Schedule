package com.example.diplomaprototype;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DaysDataLoader {
    private AtomicBoolean updating = new AtomicBoolean(false);
    public String url;
    public String currentGroup;
    public String currentFaculty;
    private static DaysDataLoader Instance;
    public List<String> listOfFaculties = new ArrayList<String>();
    private LinkedHashMap<String, String> facultiesAndUrl = new LinkedHashMap<String, String>();
    public LinkedHashMap<String, ArrayList<String> > facultiesAndGroups = new LinkedHashMap<String, ArrayList<String>>();
    private HashMap<LocalDate, LinkedHashMap<LocalDateTime, Pair<String, String>>> daysData = new HashMap<LocalDate, LinkedHashMap<LocalDateTime, Pair<String, String> >>();
    private final int timeout = 30000;
    private DaysDataLoader() {}

    public static DaysDataLoader getInstance() {
        if (Instance == null) {
            Instance = new DaysDataLoader();
        }
        return Instance;
    }

    interface UpdateUICallback{
        void callingBack(boolean isChange);
    }

    public void setBaseLoadUrl(String urlString) {
        url = urlString;
    }

    public String getBaseLoadUrl() {
        return url;
    }

    public void setFacultyUrl(String urlString) {
        if (currentFaculty != null && currentFaculty != "") {
            facultiesAndUrl.putIfAbsent(currentFaculty, urlString);
        }
    }

    public String getFacultyUrl() {
        if (currentFaculty != null && currentFaculty != "") {
            return facultiesAndUrl.get(currentFaculty);
        }
        return null;
    }

    private void LoadFacultiesSchedule(String faculty, String stringUrl) throws IOException {
        ArrayList<String> groups = new ArrayList<String>();
        URL localUrl = new URL(stringUrl);
        HttpURLConnection connection = (HttpURLConnection) localUrl.openConnection();
        connection.setConnectTimeout(timeout);

        BufferedReader page = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = page.readLine()) != null) {
            Log.i("DaysDataLoader", line);
            String[] rowData = line.split(",");
            if (rowData.length > 1) {
                // NOTE: Sets groups
                for(int i = 2; i < rowData.length; i++) {
                    groups.add(rowData[i]);
                }
                break;
            }
        }
        page.close();
        facultiesAndGroups.putIfAbsent(faculty, groups);
    }

    public void PreLoad(UpdateUICallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL localUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) localUrl.openConnection();
                connection.setConnectTimeout(timeout);

                BufferedReader basePage = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                listOfFaculties.clear();
                while ((line = basePage.readLine()) != null) {
                    Log.i("DaysDataLoader", line);
                    String[] rowData = line.split(",");
                    if (rowData.length == 2) {
                        String data = rowData[0];
                        String value = rowData[1];
                        listOfFaculties.add(data);
                        facultiesAndUrl.putIfAbsent(data, value);
                        LoadFacultiesSchedule(data, value);
                    }
                }
                basePage.close();
            } catch (Exception e) {
                Log.d("DaysDataLoader",e.toString());
            }
            handler.post(() -> {
                callback.callingBack(false);
            });
        });

    }

    private void LoadScheduleReader(BufferedReader page,
                                    HashMap<LocalDate, LinkedHashMap<LocalDateTime, Pair<String, String>>> inDaysData,
                                    ArrayList<String> copyStream) throws IOException {
        String line;
        boolean header = true;
        int groupIndex = 0;
        int year = 0;
        int month = 0;
        int day = 0;
        LinkedHashMap<LocalDateTime, Pair<String, String>> currentDay = null;
        while ((line = page.readLine()) != null) {
            if (null != copyStream) {
                copyStream.add(line);
                copyStream.add("\n");
            }
            String dayData[] = line.split(",");
            if (header) {
                header = false;
                for (int i = 0; i < dayData.length; i++) {
                    if (currentGroup.equals(dayData[i])) {
                        groupIndex = i;
                        break;
                    }
                }
                continue;
            }
            if (!dayData[0].isEmpty()) {
                if (null != currentDay) {
                    inDaysData.put(LocalDate.of(year, month, day), currentDay);
                }
                currentDay = new LinkedHashMap<LocalDateTime, Pair<String, String>>();
                String currentDate[] = dayData[0].split("\\.");
                year = Integer.parseInt(currentDate[2].trim());
                month = Integer.parseInt(currentDate[1].trim());
                day = Integer.parseInt(currentDate[0].trim());
            }

            String timeInterval[] = dayData[1].split("-");
            String timeStart[] = timeInterval[0].split(":");
            int hour = Integer.parseInt(timeStart[0].trim());
            int minutes = Integer.parseInt(timeStart[1].trim());
            if(dayData.length > groupIndex && !dayData[groupIndex].isEmpty()) {
                Pair<String, String> task = new Pair(dayData[groupIndex], "");
                currentDay.put(LocalDateTime.of(year, month, day, hour, minutes), task);
            }
        }
        if (null != currentDay) {
            inDaysData.put(LocalDate.of(year, month, day), currentDay);
        }
    }

    public void LoadSchedule(InputStreamReader streamReader) throws IOException {
        BufferedReader page = new BufferedReader(streamReader);
        LoadScheduleReader(page, daysData, null);
        page.close();
    }

    private void ApplySaveOutputStream(FileOutputStream outputStream, ArrayList<String> arrayStream) throws IOException {
        for(int i = 0; i < arrayStream.size(); i++) {
            outputStream.write(arrayStream.get(i).getBytes());
        }
    }

    public void Load(UpdateUICallback callback, Context context, String outputStreamName, Looper looper) {
        if (null != currentGroup && null != currentFaculty && facultiesAndUrl.size() > 0 && !updating.get()) {
            updating.set(true);
            AtomicBoolean isChanged = new AtomicBoolean(false);
            String localStringUrl = facultiesAndUrl.get(currentFaculty);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(looper);
            executor.execute(() -> {
                try {
                    URL localUrl = new URL(localStringUrl);
                    HttpURLConnection connection = (HttpURLConnection) localUrl.openConnection();
                    connection.setConnectTimeout(timeout);

                    InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader page = new BufferedReader(streamReader);
                    HashMap<LocalDate, LinkedHashMap<LocalDateTime, Pair<String, String>>> currentDaysData = new HashMap<LocalDate, LinkedHashMap<LocalDateTime, Pair<String, String> >>();
                    ArrayList<String> arrayStream = new ArrayList<String>();
                    LoadScheduleReader(page, currentDaysData, arrayStream);
                    if (!daysData.equals(currentDaysData)) {
                        daysData = currentDaysData;
                        FileOutputStream outputStream = context.openFileOutput(outputStreamName, context.MODE_PRIVATE);
                        ApplySaveOutputStream(outputStream, arrayStream);
                        outputStream.close();
                        isChanged.set(true);
                    }
                    page.close();
                } catch (Exception e) {
                    Log.e("DaysDataLoader",e.toString());
                }
                handler.post(() -> {
                    updating.set(false);
                    callback.callingBack(isChanged.get());
                });
            });
        }
    }

    public HashMap<LocalDateTime, Pair<String, String>> GetCurrentsDaySchedule(LocalDate date, int dayMouth)
    {
        LocalDate dayDate = date.withDayOfMonth(dayMouth);
        HashMap<LocalDateTime, Pair<String, String>> data = daysData.get(dayDate);
        return data;
    }

    public int GetDaysCount() {
        return daysData.size();
    }
}
