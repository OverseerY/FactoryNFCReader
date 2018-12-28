package com.yaroslav.factorynfcreader;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //#region Variables

    /** Ссылка на базу данных Firestore */
    FirebaseFirestore mFirestore;
    /** Управление службой NFC */
    NfcManager nfcManager;
    /** Адаптер NFC */
    NfcAdapter nfcAdapter;
    /** Значок для отображения изменения статуса NFC-адаптера */
    ImageView imageNfc;
    /** Значок для отображения изменения статуса подключения к интернету */
    ImageView imageInternet;
    /** Подпись значка статуса NFC */
    TextView labelNfc;
    /** Подпись значка статуса подключения к интернету */
    TextView labelInternet;

    /** Свойство - флаг, доступен ли NFC */
    private boolean isNfcEnabled;

    private boolean isDarkTheme = false;

    /** Меню навигации в нижней части окна приложения */
    BottomNavigationView navigation;

    /** Свойство - значение для сравнения с результатом запроса разрешения на доступ к местоположению */
    private static final int PERMISSION_REQUEST_LOCATION = 0;

    private Map<String, String> current_tags = new HashMap<>();
    private Map<String, String[]> coordinates = new HashMap<>();

    /** Свойство - массив технологий NFC потенциальных меток */
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

    /** Реализация метода меню навигации */
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

    //#region Activity Methods

    @Override
    public Resources.Theme getTheme() {
        isDarkTheme = getIntent().getBooleanExtra("theme_state", false);
        Resources.Theme theme = super.getTheme();
        if(isDarkTheme){
            theme.applyStyle(R.style.DarkTheme, true);
        } else {
            theme.applyStyle(R.style.AppTheme, true);
        }
        return theme;
    }

    /** Метод жизненного цикла активности - вызывается при её создании */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirestore = FirebaseFirestore.getInstance();

        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        imageNfc = findViewById(R.id.amNfcImg);
        imageInternet = findViewById(R.id.amInternetImg);

        labelNfc = findViewById(R.id.amNfcText);
        labelInternet = findViewById(R.id.amInternetText);

        testNfcState();
        testInternetState();

        delayBeforeInitState();
        new UpdateListOfTags().execute();

        if (isNfcEnabled) {
            listenForNfc();
        }
    }

    /** Метод жизненного цикла активности - вызывается при её создании */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /** Реализация меню в правом верхнем углу */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_dialog_window:
                showSortDialog();
                return true;
            case R.id.exit:
                finishAffinity();
            case R.id.switch_darkTheme:
                if (isDarkTheme) {
                    isDarkTheme = false;
                } else {
                    isDarkTheme = true;
                }
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("theme_state", isDarkTheme);
                startActivity(intent);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Обработка нажатия системной кнопки Назад */
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /** Метод жизненного цикла активности - вызывается после её создания сразу за методом onCreate */
    @Override
    protected void onStart() {
        super.onStart();

        displayMainFragment(2, 0);

        new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("OverseerY", "FactoryNFCReader")
                .setDisplay(Display.DIALOG)
                .setCancelable(false)
                .start();
    }

    /** Метод жизненного цикла активности - вызывается при продолжении после паузы */
    @Override
    protected void onResume() {
        super.onResume();

        delayBeforeInitState();

        if (isNfcEnabled) {
            listenForNfc();
        }
    }

    /** Запрос разрешения на доступ к местоположению */
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

    /** инициализация службы NFC - возвращает логическое значение состояния подключения адаптера */
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

    //#endregion

    //#region Check State

    /** Метод - проверка состояния подключения NFC-адаптера с определённым интервалом */
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

    /** Метод - проверка состояния подключения к сети интернет с определённым интервалом */
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
                    } else {
                        textColor = getResources().getColor(R.color.colorRed);
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

    /** Проверка доступности определённого URL, передаваемого в качестве параметра.
     * Возвращает логическое состояние доступности. */
    static public boolean isURLReachable(String url) {
        if (get(url) != null) {
            return true;
        }
        return false;
    }

    /** Метод - реализация проверки доступности URL.
     * Примечание: может вызвать сбой приложения при долгом ожидании ответа.
     * Не реализован таймаут */
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

    /** Метод - ключевой в данном приложении. Получение данных счтываемой метки от системы, обработка и сохранение*/
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
            if (checkTag(tag_id, tag_data)) {
                createNewTicket(tag_id, tag_data, getLatitude(tag_id), getLongitude(tag_id), getCurTime());
                autoCloseDialog(getString(R.string.success), tag_data + "\n" + getString(R.string.success_message), 1);
            } else {
                autoCloseDialog(getString(R.string.unknown), getString(R.string.unknown_message), 3);
            }
        } else {
            autoCloseDialog(getString(R.string.fail), getString(R.string.fail_message), 2);
        }
    }

    /** Метод - слушатель обнаружения метки при считывании через Отложенное намерение */
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

    private boolean checkTag(String id, String value) {
        if (!current_tags.isEmpty()) {
            return current_tags.containsKey(id) && current_tags.containsValue(value);
        }
        return false;
    }

    private String getLatitude(String tagId) {
        if (!coordinates.isEmpty() && coordinates.containsKey(tagId)) {
            for (Map.Entry entry : coordinates.entrySet()) {
                if (entry.getKey().equals(tagId)) {
                    String[] arr = (String[]) entry.getValue();
                    return arr[0];
                }
            }
        }
        return "";
    }

    private String getLongitude(String tagId) {
        if (!coordinates.isEmpty() && coordinates.containsKey(tagId)) {
            for (Map.Entry entry : coordinates.entrySet()) {
                if (entry.getKey().equals(tagId)) {
                    String[] arr = (String[]) entry.getValue();
                    return arr[1];
                }
            }
        }
        return "";
    }

    /** Метод - обработка данных, считанных с метки; получение уникального идентификатора метки */
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

    /** Метод - создание объекта и его упаковка для сохранения и отправки на сервер */
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

    /** Метод - получение системного времени в строковом формате в текущий момент времени */
    private String getCurTime() {
        long value = System.currentTimeMillis();
        return String.valueOf(value);
    }

    /** Метод - создание информационного диалогового окна, закрывающегося автоматически через определённый промежуток времени */
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
            case 3:
                builder.setIcon(R.drawable.ic_warning);
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

    /** Метод - запуск и отображение фрагмента Меню сортировки */
    public void showSortDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        final TagsSortDialogFragment sortDialogFragment = new TagsSortDialogFragment();
        sortDialogFragment.show(fragmentManager, "dialog");
    }

    /** Метод - запуск фрагмента с параметрами, от которых зависит какой фрагмент должен отобразиться */
    public void displayMainFragment(int id, int menuNumber) {
        MainFragment fragment = new MainFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        fragment.setFragId(id);
        fragment.setMenuNumber(menuNumber);
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    /** Метод - задерка с определённым интервалом */
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

    /** Get coordinates of tags */
    private void getCoordinatesOfTags() {
        Query queryRef = mFirestore.collection("coordinates");

        ListenerRegistration firestoreListener = queryRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.save_fail) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("CURRENT_TAGS", e.getMessage());
                    return;
                }

                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String[] lat_lon = new String[2];
                    lat_lon[0] = doc.getString("lat");
                    lat_lon[1] = doc.getString("lon");
                    coordinates.put(doc.getString("tag_id"),lat_lon);
                }
            }
        });
    }

    /** Get list of existing tags (factory ID and given name) */
    private void getTags() {
        Query queryRef = mFirestore.collection("tags");

        ListenerRegistration firestoreListener = queryRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.save_fail) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("CURRENT_TAGS", e.getMessage());
                    return;
                }

                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    current_tags.put(doc.getString("tag_id"),doc.getString("tag_name"));
                }
            }
        });
    }

    private class UpdateListOfTags extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.update_tags_title) + "\n" + getString(R.string.update_tags));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (progressDialog.isShowing()) progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            getTags();
            getCoordinatesOfTags();

            return null;
        }
    }
}



































