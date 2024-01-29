package com.example.diplomaprototype;

import android.util.Pair;

import java.time.LocalDateTime;
import java.util.HashMap;

public class UpdateDataDialogue implements UpdateDataDialogueInterface {
    HashMap<LocalDateTime, Pair<String, String>> data;
    UpdateDataDialogue(HashMap<LocalDateTime, Pair<String, String>> data) {
        this.data = data;
    }
    @Override
    public void update(LocalDateTime localDateTime, String noteText) {
        if(data != null) {
            if (data.containsKey(localDateTime)) {
                String task = data.get(localDateTime).first;
                Pair<String, String> taskNote = new Pair<>(task, noteText);
                data.replace(localDateTime, taskNote);
            }
        }
    }
}
