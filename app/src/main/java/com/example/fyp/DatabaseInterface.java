package com.example.fyp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
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
    String TAG = "DatabaseInterface";
    List<SignalSource> signalSourcesList;
    public DatabaseInterface() {
        db = FirebaseFirestore.getInstance();
        signalSourcesList = new ArrayList<>();

    }

    public void addUserDB(User user) {
        Query userQuery = db.collection("user").whereEqualTo("uid", user.getUid());
        userQuery.limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if(task.getResult().isEmpty()) {
                        UserDB userDB = new UserDB(user.getUid(), user.getSignalSourcesListNames());
                        db.collection("user").document(userDB.getUid()).set(userDB);
                        Log.d(TAG, "Saved User " + user.getDisplayName());
                    }
                } else {
                    Log.d(TAG, "Failed with: ", task.getException());
                }
            }
        });

    }

    public void addSourceToUser(User user) {
        Query userQuery = db.collection("user").whereEqualTo("uid", user.getUid());
        userQuery.limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, document.getId() + " => " + document.getData());

                        UserDB userDB = document.toObject(UserDB.class);
                        userDB.addSignalSourcesNames(user.getSignalSourcesListNames());
                        db.collection("user").document(userDB.getUid()).set(userDB);
                        Log.d(TAG, "Updated User " + user.getDisplayName());
                    }
                } else {
                    Log.d(TAG, "Failed with: ", task.getException());
                }
            }
        });
    }
    public void addSignalStrengthLocationDB(SignalStrengthLocation signalStrengthLocation) {
        db.collection("signal_strength_location").document(signalStrengthLocation.getCreationTime().toString() + signalStrengthLocation.getName()).set(signalStrengthLocation);
        Log.d(TAG, "Saved " + signalStrengthLocation.getCreationTime() + " " + signalStrengthLocation.getName());

    }

    public void getSignalStrengthLocationDB() {
        signalStrengthLocationList = new ArrayList<>();
        Query signalStrengthLocationsQuery = db.collection("signal_strength_location");
        registration = signalStrengthLocationsQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error != null) {
                    Log.w(TAG, "listen:error", error);
                    return;
                }

                for (DocumentChange dc: value.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            signalStrengthLocationList.add(dc.getDocument().toObject(SignalStrengthLocation.class));
                            Log.d(TAG, "Added " + dc.getDocument().getId() + " => " + dc.getDocument().getData());
                            break;
                        case MODIFIED:
                            SignalStrengthLocation signalStrengthLocation = dc.getDocument().toObject(SignalStrengthLocation.class);
                            for (int i = 0; i < signalStrengthLocationList.size(); i++) {
                                if(signalStrengthLocationList.get(i).getCreationTime() == signalStrengthLocation.getCreationTime() & signalStrengthLocationList.get(i).getName() == signalStrengthLocation.getName()) {
                                    signalStrengthLocationList.set(i, signalStrengthLocation);
                                }
                            }
                            Log.d(TAG, "Changed " + dc.getDocument().getId() + " => " + dc.getDocument().getData());
                            break;
                        case REMOVED:
                            SignalStrengthLocation signalStrengthLocationRemoved = dc.getDocument().toObject(SignalStrengthLocation.class);

                            for (int i = 0; i < signalStrengthLocationList.size(); i++) {
                                if(signalStrengthLocationList.get(i).getCreationTime() == signalStrengthLocationRemoved.getCreationTime()) {
                                    signalStrengthLocationList.remove(i);
                                }
                            }
                            Log.d(TAG, "Removed " + dc.getDocument().getId() + " => " + dc.getDocument().getData());
                            break;


                    }
                }
            }
        });
    }

    public void stopDatabaseListener() {
        registration.remove();
    }

    public void getSignalSourceDB(String uid, DatabaseInterface dbInterface) {

        Query signalStrengthLocationsQuery = db.collection("user");
        signalStrengthLocationsQuery.whereEqualTo("uid", uid).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d(TAG, document.getId() + " => " + document.getData());


                        for (String signalSource : document.toObject(UserDB.class).getSignalSourcesListNames()) {
                            if (!signalSourcesList.contains(signalSource)) {
                                signalSourcesList.add(new SignalSource(signalSource, dbInterface));
                            }
                        }
                    }
                }
            }
        });
    }

    public List<SignalSource> getSignalSourcesList() {
        return signalSourcesList;
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
