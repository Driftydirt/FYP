package com.example.fyp;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class User {
    private FirebaseUser firebaseUser;
    private DatabaseInterface dbInterface;
    private List<SignalSource> signalSourcesList;

    public User(FirebaseUser firebaseUser, DatabaseInterface dbInterface) {
        this.firebaseUser = firebaseUser;
        this.dbInterface = dbInterface;
        this.dbInterface.getSignalSourceDB(this.firebaseUser.getUid(), this.dbInterface);
        this.signalSourcesList = dbInterface.getSignalSourcesList();
        dbInterface.addUserDB(this);
    }

    public FirebaseUser getFirebaseUser() {
        return firebaseUser;
    }

    public void setFirebaseUser(FirebaseUser firebaseUser) {
        this.firebaseUser = firebaseUser;
    }

    public void unsetFirebaseUser() {
        this.firebaseUser = null;
    }

    public String getDisplayName() {
        return firebaseUser.getDisplayName();
    }

    public String getEmail() {
        return firebaseUser.getEmail();
    }

    public String getUid() {
        return firebaseUser.getUid();
    }

    public void addSignalSource(SignalSource signalSource) {
        if (!this.getSignalSourcesListNames().contains(signalSource.getName())) {
            signalSourcesList.add(signalSource);
            this.dbInterface.addSourceToUser(this);
        }

    }

    public void addSignalSource(List<SignalSource> signalSources) {
        for (SignalSource signalSource : signalSources) {
            if (!this.getSignalSourcesListNames().contains(signalSource.getName())) {
                signalSourcesList.add(signalSource);

            }
        }
        this.dbInterface.addSourceToUser(this);

    }

    public List<SignalSource> getSignalSourcesList() {
        this.addSignalSource(dbInterface.getSignalSourcesList());
        return signalSourcesList;
    }

    public List<String> getSignalSourcesListNames() {
        List<String> names = new ArrayList<>();
        for (SignalSource source: signalSourcesList) {
            names.add(source.getName());
        }
        return names;
    }

}
