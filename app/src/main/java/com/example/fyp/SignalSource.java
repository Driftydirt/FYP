package com.example.fyp;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SignalSource {
    String name;
    List<SignalStrengthLocation> signalStrengthLocationList;
    DatabaseInterface db;

    public SignalSource(String name, DatabaseInterface db) {
        this.name = name;
        List<SignalStrengthLocation> list = new ArrayList<>();

        this.db = db;
        this.signalStrengthLocationList = db.getSignalStrengthLocationByName(name);
    }

    public SignalSource(String name, List<SignalStrengthLocation> signalStrengthLocationList) {
        this.name = name;
        this.signalStrengthLocationList = signalStrengthLocationList;
    }

    public SignalSource(String name, SignalStrengthLocation signalStrengthLocation) {
        this.name = name;
        List<SignalStrengthLocation> signalStrengthLocationListTemp = new ArrayList<>();
        signalStrengthLocationListTemp.add(signalStrengthLocation);
        this.signalStrengthLocationList = signalStrengthLocationListTemp;

    }

    public String getName() {
        return name;
    }

    public List<SignalStrengthLocation> getSignalStrengthLocationList() {
        this.signalStrengthLocationList = this.db.getSignalStrengthLocationByName(this.name);
        return signalStrengthLocationList;
    }

    public void addSignalStrengthLocation(SignalStrengthLocation signalStrengthLocation) {
        signalStrengthLocationList.add(signalStrengthLocation);
    }

    public void addMultipleSignalStrengthLocation(List<SignalStrengthLocation> newLocations) {
        for (SignalStrengthLocation newLocation: newLocations) {
            signalStrengthLocationList.add(newLocation);
        }
    }
}
