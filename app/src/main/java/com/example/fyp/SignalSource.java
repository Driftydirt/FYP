package com.example.fyp;

import java.util.ArrayList;
import java.util.List;

public class SignalSource {
    String name;
    List<SignalStrengthLocation> signalStrengthLocationList;

    public SignalSource(String name) {
        this.name = name;
        this.signalStrengthLocationList = new ArrayList<>();
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
        return signalStrengthLocationList;
    }

    public void addSignalStrengthLocation(SignalStrengthLocation signalStrengthLocation) {
        signalStrengthLocationList.add(signalStrengthLocation);
    }
}
