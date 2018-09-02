package com.hb.googlecalendareventssample;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;



public class CalendarEventsActivity extends AppCompatActivity implements
        EasyPermissions.PermissionCallbacks, EventClicked {

    RecyclerView rvEvents;
    EventsAdapter eventsAdapter;

    ProgressDialog mProgress;
    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_events);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        initComponents();

    }

    private void initComponents() {
        rvEvents = (RecyclerView) findViewById(R.id.rvEvents);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvEvents.getContext(), LinearLayoutManager.VERTICAL);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setHasFixedSize(true);
        rvEvents.addItemDecoration(dividerItemDecoration);
        getAllCalendarEvents();

    }

    private void getAllCalendarEvents() {

        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                CalendarEventsActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {

            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);

            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                mProgress = new ProgressDialog(this);
                mProgress.setMessage("Calling Google Calendar API ...");
                new MakeRequestTask(mCredential).execute();
            } else {
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {

                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        mProgress = new ProgressDialog(this);
                        mProgress.setMessage("Calling Google Calendar API ...");
                        new MakeRequestTask(mCredential).execute();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == REQUEST_PERMISSION_GET_ACCOUNTS) {
            chooseAccount();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    public void onEventClickListener(int position, CalendarEvents events) {

        Bundle mBundle = new Bundle();
        mBundle.putSerializable("Event", events);
        Intent editEventIntent = new Intent(CalendarEventsActivity.this,
                EditEventActivity.class);
        editEventIntent.putExtra("EventData", mBundle);
        startActivity(editEventIntent);
        finish();
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, ArrayList<CalendarEvents>> {
        private com.google.api.services.calendar.Calendar mService = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        @Override
        protected ArrayList<CalendarEvents> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        private ArrayList<CalendarEvents> getDataFromApi() throws IOException {
            DateTime now = new DateTime(System.currentTimeMillis());
            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();
            ArrayList<CalendarEvents> calendarEventsList = new ArrayList<>();

            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                CalendarEvents calendarEvents = new CalendarEvents();
                calendarEvents.title = event.getSummary();
                calendarEvents.description = event.getDescription();
                calendarEvents.venue = event.getLocation();
                calendarEvents.id = event.getId();
                if (start == null) {
                    start = event.getStart().getDate();
                    calendarEvents.dateOnly = true;
                } else {
                    calendarEvents.dateOnly = false;
                }
                calendarEvents.datetime = start;
                calendarEventsList.add(calendarEvents);
                eventStrings.add(
                        String.format("%s (%s)", event.getSummary(), start));
            }
            return calendarEventsList;
        }


        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(ArrayList<CalendarEvents> output) {
            mProgress.hide();
            setData(output);
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
        }
    }

    private void setData(ArrayList<CalendarEvents> calendarEventsList) {
        eventsAdapter = new EventsAdapter(CalendarEventsActivity.this, calendarEventsList, this);
        rvEvents.setAdapter(eventsAdapter);
    }
}
