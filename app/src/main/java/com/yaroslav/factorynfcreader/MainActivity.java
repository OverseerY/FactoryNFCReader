package com.yaroslav.factorynfcreader;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcV;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //#region Variables

    FirebaseFirestore mFirestore;

    LocationManager locationManager;
    NfcManager nfcManager;
    NfcAdapter nfcAdapter;

    ImageView imageNfc;
    ImageView imageLocation;
    ImageView imageInternet;
    TextView labelNfc;
    TextView labelLocation;
    TextView labelInternet;

    private String curLatitude;
    private String curLongitude;

    private boolean isNfcEnabled;
    private boolean isGpsEnabled;
    private boolean isInternetEnabled;

    BottomNavigationView navigation;

    private static final int LOCATION_INTERVAL = 1000; // 1 sec
    private static final float LOCATION_DISTANCE = 1f; // 1 meter

    private static final int PERMISSION_REQUEST_LOCATION = 0;

    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(),
                    Ndef.class.getName()
            }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_main:
                    displayMainFragment(2, 0);
                    return true;
                case R.id.navigation_history:
                    displayMainFragment(1, 1);
                    return true;
            }
            return false;
        }
    };

    //#endregion

    //#region Get / Set

    public void setCurLatitude(String curLatitude) {
        this.curLatitude = curLatitude;
    }

    public void setCurLongitude(String curLongitude) {
        this.curLongitude = curLongitude;
    }

    public String getCurLatitude() {
        return curLatitude;
    }

    public String getCurLongitude() {
        return curLongitude;
    }

    //#endregion

    //#region Activity Methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppCustomTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirestore = FirebaseFirestore.getInstance();

        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        imageNfc = findViewById(R.id.amNfcImg);
        imageLocation = findViewById(R.id.amLocationImg);
        imageInternet = findViewById(R.id.amInternetImg);

        labelNfc = findViewById(R.id.amNfcText);
        labelLocation = findViewById(R.id.amLocationText);
        labelInternet = findViewById(R.id.amInternetText);

        testNfcState();
        testGpsState();
        testInternetState();

        delayBeforeInitState();

        if (isNfcEnabled) {
            listenForNfc();
        }

        if (isGpsEnabled) {
            initLocationProvider();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_dialog_window:
                showSortDialog();
                return true;
            case R.id.exit:
                finishAffinity();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        displayMainFragment(2, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        delayBeforeInitState();

        if (isNfcEnabled) {
            listenForNfc();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.permission_granted), Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.permission_denied), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    //#endregion

    //#region Other Methods

    //#region Initialization

    private boolean initNFC() {
        nfcManager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        boolean service_enabled = false;
        try {
            nfcAdapter = nfcManager.getDefaultAdapter();
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                service_enabled = true;
            }
        } catch (NullPointerException e) {
            Log.e("initNFC", e.getLocalizedMessage());
        }
        return service_enabled;
    }

    private boolean initGPS() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean service_enabled = false;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                service_enabled = true;
            }
        } catch (NullPointerException e) {
            Log.e("initLocationManager", e.getLocalizedMessage());
        }
        return service_enabled;
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            setCurLatitude(location.convert(location.getLatitude(), location.FORMAT_DEGREES));
            setCurLongitude(location.convert(location.getLongitude(), location.FORMAT_DEGREES));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void initLocationProvider() {
        Log.i("INITLP", "initializeLocationProvider");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
            } catch (java.lang.SecurityException ex) {
                Log.e("INITLP", "Fail to request location update, ignore:", ex);
            } catch (IllegalArgumentException ex) {
                Log.e("INITLP", "Network provider does not exist: " + ex.getMessage());
            } catch (NullPointerException ex) {
                Log.e("INITLP", "Fuck you, NETWORK PROVIDER: " + ex.getMessage());
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
            } catch (java.lang.SecurityException ex) {
                Log.e("INITLP", "Fail to request location update, ignore: ", ex);
            } catch (IllegalArgumentException ex) {
                Log.e("INITLP", "Gps provider does not exist: " + ex.getMessage());
            } catch (NullPointerException ex) {
                Log.e("INITLP", "Fuck you, GPS PROVIDER: " + ex.getMessage());
            }
        } else {
            requestLocationPermissions();
        }
    }

    //Ask user for geolocation permissions
    private void requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(findViewById(android.R.id.content), "Permission granted", Snackbar.LENGTH_INDEFINITE).setAction("Ok", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Permission denied", Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        }
    }

    //#endregion

    //#region Check State

    public void testNfcState() {
        Timer nfcTimer = new Timer();
        final Handler nfcHandler = new Handler();
        nfcTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final int statusColor;
                if (initNFC()) {
                    statusColor = getResources().getColor(R.color.colorGreen);
                    isNfcEnabled = true;
                } else {
                    statusColor = getResources().getColor(R.color.colorRed);
                    isNfcEnabled = false;
                }
                nfcHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        labelNfc.setTextColor(statusColor);
                        imageNfc.setColorFilter(statusColor);
                    }
                });
            }
        }, 0L, 5L * 1000);
    }

    public void testGpsState() {
        Timer gpsTimer = new Timer();
        final Handler gpsHandler = new Handler();
        gpsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final int textColor;
                if (initGPS()) {
                    textColor = getResources().getColor(R.color.colorGreen);
                    isGpsEnabled = true;
                } else {
                    textColor = getResources().getColor(R.color.colorRed);
                    isGpsEnabled = false;
                }
                gpsHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        labelLocation.setTextColor(textColor);
                        imageLocation.setColorFilter(textColor);
                    }
                });
            }
        }, 0L, 2L * 1000);
    }

    public void testInternetState() {
        Timer netTimer = new Timer();
        final Handler netHandler = new Handler();
        netTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final int textColor;
                try {
                    if (isURLReachable("http://google.com")) {
                        textColor = getResources().getColor(R.color.colorGreen);
                        isInternetEnabled = true;
                    } else {
                        textColor = getResources().getColor(R.color.colorRed);
                        isInternetEnabled = false;
                    }
                    netHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            labelInternet.setTextColor(textColor);
                            imageInternet.setColorFilter(textColor);
                        }
                    });
                } catch (Exception e) {
                    Log.e("TIS", e.getMessage());
                }
            }
        }, 0L, 2L * 1000);
    }

    static public boolean isURLReachable(String url) {
        if (get(url) != null) {
            return true;
        }
        return false;
    }

    static String get(String url) {
        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url(url).get().build();
        try {
            Response resp = client.newCall(req).execute();
            ResponseBody body = resp.body();
            if (resp.isSuccessful()) {
                return body.string(); // Closes automatically.
            } else {
                body.close();
                return null;
            }
        } catch (IOException e) {
            Log.e("get_HTTP_Response", e.getLocalizedMessage());
            return null;
        }
    }

    //#endregion

    //#region NFC Listener

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String tag_data = "";
        String tag_id = "";

        Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (data != null) {
            try {
                for (int i = 0; i < data.length; i++) {
                    NdefRecord[] recs = ((NdefMessage) data[i]).getRecords();
                    for (int j = 0; j < recs.length; j++) {
                        if (recs[j].getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(recs[j].getType(), NdefRecord.RTD_TEXT)) {
                            byte[] payload = recs[j].getPayload();
                            String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
                            int langCodeLen = payload[0] & 0077;
                            tag_data += (new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1, textEncoding));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("TagDispatch", e.getLocalizedMessage());
            }
        }
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            tag_id = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
        }
        //Check if tag name is not empty
        if (!tag_data.equals("") && !tag_id.equals("")) {
            createNewTicket(tag_id, tag_data, getCurLatitude(), getCurLongitude(), getCurTime());
            autoCloseDialog(getString(R.string.success), tag_data + "\n" + getString(R.string.success_message), 1);
        } else {
            autoCloseDialog(getString(R.string.fail), getString(R.string.fail_message), 2);
        }
    }

    private void listenForNfc() {
        try {
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter filter = new IntentFilter();
            filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
            filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
            nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
        } catch (Exception e) {
            Log.e("listenForNfc", e.getLocalizedMessage());
        }
    }

    private String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";
        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    private void createNewTicket(String id, String name, String lat, String lon, String time) {
        Map<String, Object> tagMap = new Ticket(id, name, lat, lon, time).toMap();

        mFirestore.collection("tickets")
                .add(tagMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(getApplicationContext(), getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.save_fail) + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    //#endregion

    //#region Common Methods

    private String getCurTime() {
        long value = System.currentTimeMillis();
        return String.valueOf(value);
    }

    public void autoCloseDialog(String title, String message, int iconType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        switch (iconType) {
            case 1:
                builder.setIcon(R.drawable.ic_checked);
                break;
            case 2:
                builder.setIcon(R.drawable.ic_alert);
                break;
        }
        builder.setCancelable(true);

        final AlertDialog closedialog = builder.create();

        closedialog.show();

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closedialog.dismiss();
                timer.cancel();
            }
        }, 3000);

    }

    public void showSortDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        final TagsSortDialogFragment sortDialogFragment = new TagsSortDialogFragment();
        sortDialogFragment.show(fragmentManager, "dialog");
    }

    public void displayMainFragment(int id, int menuNumber) {
        MainFragment fragment = new MainFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        fragment.setFragId(id);
        fragment.setMenuNumber(menuNumber);
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void delayBeforeInitState() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 2000);
    }

    //#endregion

    //#endregion
}



































