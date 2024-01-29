package com.example.diplomaprototype;

import java.time.LocalDateTime;
import java.util.HashMap;

public class UpdateUserDataDialogue implements UpdateDataDialogueInterface {
    HashMap<LocalDateTime, String> userData;
    UpdateUserDataDialogue(HashMap<LocalDateTime, String> userData) {
        this.userData = userData;
    }
    @Override
    public void update(LocalDateTime localDateTime, String noteText) {
        if (noteText.isEmpty()) {
            userData.remove(localDateTime);
        }
        else {
            if(null != userData.get(localDateTime)) {
                userData.replace(localDateTime, noteText);
            }
            else {
                userData.put(localDateTime, noteText);
            }
        }
    }
}
