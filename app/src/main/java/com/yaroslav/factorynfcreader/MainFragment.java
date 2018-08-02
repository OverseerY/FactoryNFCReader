package com.yaroslav.factorynfcreader;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

public class MainFragment extends Fragment {
    FirebaseFirestore mFirestore;

    RecyclerView historyRecyclerView;
    ProgressBar historyProgressBar;
    TicketAdapter ticketAdapter;

    private int fragId;
    private String fragName = "";
    private int menuNumber;
    private int orderNumber;
    private boolean orderType;
    private String beginSort;
    private String endSort;

    private static final String coll_ref = "tickets";

    private static final long dateCorrection = 1000 * 60 * 60 * 24;
    private static long todayDate = new Date().getTime();

    public MainFragment() {

    }

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirestore = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView;

        switch (fragId) {
            case 1:
                rootView = inflater.inflate(R.layout.history_list, container, false);
                initHistory(rootView, orderNumber, orderType, beginSort, endSort);
                fragName = getString(R.string.title_history);
                setHasOptionsMenu(true);
                break;
            case 2:
                rootView = inflater.inflate(R.layout.default_layout, container, false);
                fragName = getString(R.string.app_name);
                break;
            default:
                rootView = inflater.inflate(R.layout.default_layout, container, false);
                fragName = getString(R.string.app_name);
                break;
        }

        ((MainActivity) getActivity()).getSupportActionBar().setTitle(fragName);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menuNumber == 1) {
            inflater.inflate(R.menu.sort_menu, menu);
        }
    }

    public void setFragName(String fragName) {
        this.fragName = fragName;
    }

    public void setFragId(int fragId) {
        this.fragId = fragId;
    }

    public void setMenuNumber(int menuNumber) {
        this.menuNumber = menuNumber;
    }

    public void setBeginSort(String beginSort) {
        this.beginSort = beginSort;
    }

    public void setEndSort(String endSort) {
        this.endSort = endSort;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderType(boolean orderType) {
        this.orderType = orderType;
    }

    private String converteTime(String value) {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long temp = Long.parseLong(value);
        Date date = new Date(temp);
        return format.format(date);
    }

    private void initHistory(View view, int order, boolean orderType, String begin, String end) {
        String orderQuery;
        Query.Direction direction;
        Query queryRef;

        historyRecyclerView = view.findViewById(R.id.hlRecyclerView);
        historyProgressBar = view.findViewById(R.id.hlProgressBar);
        final List<Ticket> tagList = new ArrayList<>();
        ticketAdapter = new TicketAdapter(tagList, view.getContext(), mFirestore);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        historyRecyclerView.setLayoutManager(layoutManager);
        historyRecyclerView.setItemAnimator(new DefaultItemAnimator());
        historyRecyclerView.setAdapter(ticketAdapter);

        if (orderType) {
            direction = Query.Direction.ASCENDING;
        } else {
            direction = Query.Direction.DESCENDING;
        }

        switch (order) {
            case 0:
                orderQuery = "date";
                break;
            case 1:
                orderQuery = "name";
                break;
            default:
                orderQuery = "date";
                break;
        }

        if (begin != null && end != null) {
            queryRef = mFirestore.collection(coll_ref).whereGreaterThan("date", begin).whereLessThan("date", end).orderBy(orderQuery, direction);
        } else {
            queryRef = mFirestore.collection(coll_ref).whereGreaterThan("date", Long.toString(todayDate - dateCorrection)).orderBy("date", Query.Direction.DESCENDING);
        }

        historyProgressBar.setVisibility(View.VISIBLE);
        ListenerRegistration firestoreListener = queryRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(getContext(), getString(R.string.save_fail) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("HISTORY", e.getMessage());
                    return;
                }

                List<Ticket> userList = new ArrayList<>();

                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Ticket ticket = doc.toObject(Ticket.class);
                    ticket.setTagId(doc.getString("id"));
                    ticket.setTagName(doc.getString("name"));
                    ticket.setLatitude(doc.getString("latitude"));
                    ticket.setLongitude(doc.getString("longitude"));
                    ticket.setReadTime(doc.getString("date"));
                    tagList.add(ticket);
                }

                ticketAdapter = new TicketAdapter(tagList, getContext(), mFirestore);
                historyRecyclerView.setAdapter(ticketAdapter);
                historyProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

}














































