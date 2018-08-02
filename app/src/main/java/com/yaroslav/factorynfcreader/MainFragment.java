package com.yaroslav.factorynfcreader;

import android.os.Bundle;
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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

public class MainFragment extends Fragment {
    FirebaseFirestore mFirestore;

    /** RecyclerView для отображения списка меток */
    RecyclerView historyRecyclerView;
    /** Бегунок прогресса загрузки данных */
    ProgressBar historyProgressBar;
    /** Адаптер класса Ticket для отображения данных считанных меток */
    TicketAdapter ticketAdapter;

    /** Свойство - идентификатор фрагмента; определяет какой именно фрагмент
     * должен быть отображен в данный момент */
    private int fragId;
    /** Свойство - название фрагмента для отображения в Status Bar */
    private String fragName = "";
    /** Свойство - идентификатор меню; определяет какой элемент меню
     * будет отображен в данный момент */
    private int menuNumber;
    /** Свойство - идентификатор сортировки; определяет по какому значению нужно сортировать метки */
    private int orderNumber;
    /** Свойство - Тип Сортировки; определяет какого типа должна произвестись сортировка -
     * по возрастанию или по убыванию */
    private boolean orderType;
    /** Свойство - дата в миллисекундах, хранимая строковой переменной, для определения нижней границы
     * временного интервала */
    private String beginSort;
    /** Свойство - дата в миллисекундах, хранимая строковой переменной, для определения верхней границы
     * временного интервала */
    private String endSort;

    /** Свойство - постоянное значение ссылки на коллекцию документов в Firestore */
    private static final String coll_ref = "tickets";

    /** Свойство - хранит значение времени в миллисекундах, равное приблизительно 1 суткам */
    private static final long dateCorrection = 1000 * 60 * 60 * 24;
    /** Свойство - принимает значение времени в миллисекундах в текущий момент времени */
    private static long todayDate = new Date().getTime();

    /** Конструктор класса без параметров*/
    public MainFragment() {

    }

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    /** Метод - один из методов жизненного цикла фрагмента.
     * Вызывается при создании фрагмента. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirestore = FirebaseFirestore.getInstance();
    }

    /** Метод - один из методов жизненного цикла фрагмента. Вызывается после метода onCreate
     * инициализация фрагментов в зависимости от значения переменной идентификатор Фрагмента */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView;

        switch (fragId) {
            case 1:
                rootView = inflater.inflate(R.layout.history_list, container, false);
                initHistory(rootView, orderNumber, orderType, beginSort, endSort);
                setFragName(getString(R.string.title_history));
                setHasOptionsMenu(true);
                break;
            case 2:
                rootView = inflater.inflate(R.layout.default_layout, container, false);
                setFragName(getString(R.string.app_name));
                break;
            default:
                rootView = inflater.inflate(R.layout.default_layout, container, false);
                setFragName(getString(R.string.app_name));
                break;
        }

        ((MainActivity) getActivity()).getSupportActionBar().setTitle(fragName);

        return rootView;
    }

    /** Метод - добавление дополнительного элемента в меню в правом верхнем углу;
     * В зависимости от значения переменной menuNumber отображаются различные
     * элементы, как правило отдельный фрагмент - отдельное меню */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menuNumber == 1) {
            inflater.inflate(R.menu.sort_menu, menu);
        }
    }

    /** Метод - установка значения свойства Название Фрагмента */
    public void setFragName(String fragName) {
        this.fragName = fragName;
    }

    /** Метод - установка значения свойства идентификатор Фрагмента */
    public void setFragId(int fragId) {
        this.fragId = fragId;
    }

    /** Метод - установка значения свойства идентификатор Меню */
    public void setMenuNumber(int menuNumber) {
        this.menuNumber = menuNumber;
    }

    /** Метод - установка значения свойства Начало интервала для сортировки */
    public void setBeginSort(String beginSort) {
        this.beginSort = beginSort;
    }

    /** Метод - установка значения свойства Конец интервала для сортировки */
    public void setEndSort(String endSort) {
        this.endSort = endSort;
    }

    /** Метод - установка значения свойства идентификатор Сортировки */
    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    /** Метод - установка значения свойства Тип Сортировки */
    public void setOrderType(boolean orderType) {
        this.orderType = orderType;
    }

    /** Метод - инициализация фрагмента для отображения считанных меток.
     * Принимает параметры для обработки и сортировки данных меток,
     * считанных в интервале, переданным с параметрами Начало интервала и Конец интервала */
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














































