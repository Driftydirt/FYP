package com.example.fyp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DatabaseInterface {
    FirebaseFirestore db;
    List<SignalStrengthLocation> signalStrengthLocationList;
    ListenerRegistration registration;
    public DatabaseInterface() {
        db = FirebaseFirestore.getInstance();

    }

    public void addSignalStrengthLocationDB(SignalStrengthLocation signalStrengthLocation) {
        db.collection("signal_strength_location").document(signalStrengthLocation.getCreationTime().toString() + signalStrengthLocation.getName()).set(signalStrengthLocation);
        Log.d("DatabaseInterface", "Saved " + signalStrengthLocation.getCreationTime() + " " + signalStrengthLocation.getName());

    }

    public void getSignalStrengthLocationDB() {
        signalStrengthLocationList = new ArrayList<>();
        Query signalStrengthLocationsQuery = db.collection("signal_strength_location");
        registration = signalStrengthLocationsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null) {
                    Log.w("DatabaseInterface", "listen:error", error);
                    return;
                }

                for (DocumentChange dc: value.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            signalStrengthLocationList.add(dc.getDocument().toObject(SignalStrengthLocation.class));
                            Log.d("DatabaseInterface", "Added " + dc.getDocument().getId() + " => " + dc.getDocument().getData());
                            break;
                        case MODIFIED:
                            SignalStrengthLocation signalStrengthLocation = dc.getDocument().toObject(SignalStrengthLocation.class);
                            for (int i = 0; i < signalStrengthLocationList.size(); i++) {
                                if(signalStrengthLocationList.get(i).getCreationTime() == signalStrengthLocation.getCreationTime() & signalStrengthLocationList.get(i).getName() == signalStrengthLocation.getName()) {
                                    signalStrengthLocationList.set(i, signalStrengthLocation);
                                }
                            }
                            Log.d("DatabaseInterface", "Changed " + dc.getDocument().getId() + " => " + dc.getDocument().getData());
                            break;
                        case REMOVED:
                            SignalStrengthLocation signalStrengthLocationRemoved = dc.getDocument().toObject(SignalStrengthLocation.class);

                            for (int i = 0; i < signalStrengthLocationList.size(); i++) {
                                if(signalStrengthLocationList.get(i).getCreationTime() == signalStrengthLocationRemoved.getCreationTime()) {
                                    signalStrengthLocationList.remove(i);
                                }
                            }
                            Log.d("DatabaseInterface", "Removed " + dc.getDocument().getId() + " => " + dc.getDocument().getData());
                            break;


                    }
                }
            }
        });
    }

    public void stopDatabaseListener() {
        registration.remove();
    }

    public List<SignalStrengthLocation> getSignalStrengthLocationByName(String name) {
        List<SignalStrengthLocation> locations = new ArrayList<>();
        for (SignalStrengthLocation location: signalStrengthLocationList) {
            if(location.getName().equals(name)) {
                locations.add(location);
            }
        }
        return locations;
    }
}
