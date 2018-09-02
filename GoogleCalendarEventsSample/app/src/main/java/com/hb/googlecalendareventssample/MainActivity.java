package com.hb.googlecalendareventssample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    Button btnAddEvent,btnGetEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnAddEvent = (Button) findViewById(R.id.btnAddEvent);
        btnAddEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToAddEvent();
            }
        });

        btnGetEvents = (Button) findViewById(R.id.btnGetEvents);
        btnGetEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToGetEvents();
            }
        });
    }

    private void navigateToGetEvents() {
        Intent calendarEventsIntent = new Intent(MainActivity.this, CalendarEventsActivity.class);
        startActivity(calendarEventsIntent);
    }

    private void navigateToAddEvent() {
        Intent addEventIntent = new Intent(MainActivity.this, AddEventActivity.class);
        startActivity(addEventIntent);
    }
}
