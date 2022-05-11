package com.example.fyp;

import java.util.ArrayList;
import java.util.List;

public class UserDB {
    private String uid;
    private List<String> signalSourcesList;

    public UserDB(String uid, List<String> signalSourcesList) {
        this.uid = uid;
        this.signalSourcesList = signalSourcesList;
    }

    public UserDB() {
        this.uid = "";
        this.signalSourcesList = new ArrayList();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<String> getSignalSourcesListNames() {
        return this.signalSourcesList;
    }

    public void setSignalSourcesListNames(List<String> signalSourcesList) {
        this.signalSourcesList = signalSourcesList;
    }

    public void addSignalSourcesNames(List<String> signalSourcesList) {
        for (String signalSource : signalSourcesList) {
            if (!this.signalSourcesList.contains(signalSource)) {
                this.signalSourcesList.add(signalSource);
            }
        }
    }
}
