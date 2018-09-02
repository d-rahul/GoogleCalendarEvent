package com.hb.googlecalendareventssample;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class AddEventActivity extends AppCompatActivity implements
        EasyPermissions.PermissionCallbacks {

    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};
    private EditText etEventTitle, etEventDescription, etEventDate, etEventTime, etEventVenue;
    Button btnCreateEvent;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        initViews();
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    private void initViews() {

        etEventTitle = (EditText) findViewById(R.id.etEventTitle);
        etEventDescription = (EditText) findViewById(R.id.etEventDesc);
        etEventDate = (EditText) findViewById(R.id.etEventDate);
        etEventDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });
        etEventTime = (EditText) findViewById(R.id.etEventTime);
        etEventTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker();
            }
        });
        etEventVenue = (EditText) findViewById(R.id.etEventVenue);
        btnCreateEvent = (Button) findViewById(R.id.btnCreateEvent);
        btnCreateEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateEventDetails();
            }
        });
    }

    private void showTimePicker() {

        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(AddEventActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                etEventTime.setText(selectedHour + ":" + selectedMinute);
            }
        }, hour, minute, true);//Yes 24 hour time
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    Calendar myCalendar = Calendar.getInstance();

    private void showDatePicker() {

        DatePickerDialog dialog = new DatePickerDialog(AddEventActivity.this, new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDate();
            }
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();


    }

    private void updateDate() {
        String myFormat = "yyyy-MM-dd"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        etEventDate.setText(sdf.format(myCalendar.getTime()));
    }

    String eventTitle, eventDescription, eventDate, eventTime, eventVenue;

    private void validateEventDetails() {
        eventTitle = etEventTitle.getText().toString().trim();
        eventDescription = etEventDescription.getText().toString().trim();
        eventDate = etEventDate.getText().toString().trim();
        eventTime = etEventTime.getText().toString().trim();
        eventVenue = etEventVenue.getText().toString().trim();

        if (TextUtils.isEmpty(eventTitle)) {

        } else if (TextUtils.isEmpty(eventDescription)) {

        } else if (TextUtils.isEmpty(eventDate)) {

        } else if (TextUtils.isEmpty(eventTime)) {

        } else if (TextUtils.isEmpty(eventVenue)) {

        } else {
            checkGoogleCalendarPermissions();
        }
    }

    private void checkGoogleCalendarPermissions() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
//            mOutputText.setText("No network connection available.");
        } else {
            mProgress = new ProgressDialog(this);
            mProgress.setMessage("Calling Google Calendar API ...");
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                AddEventActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
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
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
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

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
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

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, Void> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Void doInBackground(Void... params) {
            try {
                createCalendarEvent();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
            }
            return null;
        }

        private void createCalendarEvent() throws IOException {

            Event event = new Event()
                    .setSummary(eventTitle)
                    .setLocation(eventVenue)
                    .setDescription(eventDescription);

            String tz = TimeZone.getDefault().getID();
            DateTime startDateTime = new DateTime(getStartDateTime(eventDate, eventTime));
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime).setTimeZone(tz);
            event.setStart(start);
            event.setEnd(start);

//            DateTime endDateTime = new DateTime("2017-11-09T17:00:00+05:30");
//            EventDateTime end = new EventDateTime()
//                    .setDateTime(endDateTime).setTimeZone("Asia/Calcutta");
//            event.setEnd(end);

//            String[] recurrence = new String[]{"RRULE:FREQ=DAILY;COUNT=1"};
//            event.setRecurrence(Arrays.asList(recurrence));

//            EventAttendee[] attendees = new EventAttendee[]{
//                    new EventAttendee().setEmail("lpage@example.com"),
//                    new EventAttendee().setEmail("sbrin@example.com"),
//            };
//            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[]{
                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                    new EventReminder().setMethod("popup").setMinutes(10),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = "primary";
            try {
                event = mService.events().insert(calendarId, event).execute();
            } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
                showGooglePlayServicesAvailabilityErrorDialog(
                        availabilityException.getConnectionStatusCode());
            } catch (UserRecoverableAuthIOException userRecoverableException) {
                startActivityForResult(
                        userRecoverableException.getIntent(), AddEventActivity.REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.printf("Event created: %s\n", event.getHtmlLink());

        }


        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mProgress.hide();
            navigateToEventsList();
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
        }
    }

    private void navigateToEventsList() {
        Intent calendarEventsIntent = new Intent(AddEventActivity.this, CalendarEventsActivity.class);
        startActivity(calendarEventsIntent);
    }

    String formattedDate;

    private String getStartDateTime(String eventDate, String eventTime) {
        DateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        DateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
        try {
            Date date = originalFormat.parse(eventDate + " " + eventTime);
            formattedDate = targetFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formattedDate;
    }
}
