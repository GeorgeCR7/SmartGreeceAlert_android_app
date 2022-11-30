package com.example.smartgreecealert.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.smartgreecealert.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class StatisticsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    //Firebase variables.
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference dbDangers;

    //Current user ID.
    private String userID;

    //Spinner drop down lists variables.
    Spinner eventsSpinner;
    Spinner dateSpinner;

    TextView totalResults;
    TextView totalResultsNumber;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        //Initialize firebase variables.
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        
        //Get the current user ID from firebase.
        userID = mAuth.getUid();

        totalResults = findViewById(R.id.totalResults);
        totalResults.setText(getString(R.string.totalResults));
        totalResults.setVisibility(View.INVISIBLE);

        totalResultsNumber = findViewById(R.id.totalResultsNumber);

        //Drop down menu for events.
        eventsSpinner = findViewById(R.id.spinnerEventType);
        ArrayAdapter<CharSequence> events = ArrayAdapter.createFromResource(this, R.array.events, android.R.layout.simple_spinner_item);
        events.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        eventsSpinner.setAdapter(events);
        eventsSpinner.setOnItemSelectedListener(this);

        //Drop down menu for date.
        dateSpinner = findViewById(R.id.spinnerDate);
        ArrayAdapter<CharSequence> dates = ArrayAdapter.createFromResource(this, R.array.dates, android.R.layout.simple_spinner_item);
        dates.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(dates);
        dateSpinner.setOnItemSelectedListener(this);

        progressBar = findViewById(R.id.progressBarStatistics);
    }

    public void showStats (View view) {

        progressBar.setVisibility(View.VISIBLE);

        //Take the data from drop down lists: Event Type & Date.
        String event = eventsSpinner.getSelectedItem().toString().toUpperCase();
        String date = dateSpinner.getSelectedItem().toString();

        String typeAll = getString(R.string.eventsAll).toUpperCase();

        // Get the spinner timestamps to be queried against
        Map<String, Timestamp> timestamps = getTimestamps();

        // Create predicates to query firebase results
        Predicate<DataSnapshot> isAborted = s ->  s.child("aborted").getValue(Boolean.class);

        Predicate<DataSnapshot> matchType = s -> {
            String type = s.child("type").getValue(String.class);
            return event.equals(typeAll) || event.equals(type);
        };

        Predicate<DataSnapshot> matchDate = s -> {
            String timestampStr = s.child("timestamp").getValue(String.class);
            Timestamp timestamp = Timestamp.valueOf(timestampStr);
            return timestamp.after(timestamps.get(date));
        };

        //Take the reference from Firebase for events.
        dbDangers = database.getReference("notifications");

        Query query = dbDangers
                .orderByChild("userID")
                .equalTo(userID);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int counter = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (isAborted.negate().and(matchType).and(matchDate).test(snapshot)) {
                        counter++;
                    }
                }
                progressBar.setVisibility(View.GONE);
                totalResults.setVisibility(View.VISIBLE);
                totalResultsNumber.setText(String.valueOf(counter));
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Calculates spinner timestamps
     *
     * @return Map<String, Timestamp>
     */
    private Map<String, Timestamp> getTimestamps() {
        Map<String, Timestamp> map = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_WEEK, -1);
        map.put(getString(R.string.dateToday), new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.DAY_OF_WEEK, -7);
        map.put(getString(R.string.dateLastWeek), new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.DAY_OF_WEEK, 7);
        cal.add(Calendar.MONTH, -1);
        map.put(getString(R.string.dateLastMonth), new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.YEAR, -1);
        map.put(getString(R.string.dateLastYear), new Timestamp(cal.getTime().getTime()));
        return map;
    }
}