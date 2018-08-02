package com.yaroslav.factorynfcreader;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

public class TagsSortDialogFragment extends DialogFragment{

    AppCompatRadioButton ascOrder;
    AppCompatRadioButton desOrder;
    Button cancelButton;
    Button okButton;
    Spinner selectOrderBy;
    EditText startDate;
    EditText finishDate;
    TextView startMillisec;
    TextView finishMillisec;

    Calendar calendar;
    private static final long dateCorrection = 1000 * 60 * 60 * 24;
    private static long todayDate = new Date().getTime();

    private boolean isAscending;
    private boolean isDefinedRange;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tags_sort_dialog, container, false);

        calendar = Calendar.getInstance();

        ascOrder = view.findViewById(R.id.tsdAscending);
        desOrder = view.findViewById(R.id.tsdDescending);
        cancelButton = view.findViewById(R.id.tsdCancel);
        okButton = view.findViewById(R.id.tsdOk);
        selectOrderBy = view.findViewById(R.id.tsdOrderBy);
        startDate = view.findViewById(R.id.tsdBegin);
        finishDate = view.findViewById(R.id.tsdEnd);
        startMillisec = view.findViewById(R.id.tsdBeginMillisec);
        finishMillisec = view.findViewById(R.id.tsdEndMillisec);

        ArrayAdapter<CharSequence> orderByAdapter = ArrayAdapter.createFromResource(view.getContext(), R.array.order_by, android.R.layout.simple_spinner_dropdown_item);
        selectOrderBy.setAdapter(orderByAdapter);

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDate(startDate, startMillisec);
            }
        });

        finishDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDate(finishDate, finishMillisec);
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedOrder = (int) selectOrderBy.getSelectedItemId();
                if (ascOrder.isChecked()) isAscending = true;
                if (desOrder.isChecked()) isAscending = false;
                String begin = startMillisec.getText().toString();
                String end = finishMillisec.getText().toString();
                if (begin.equals("")) {
                    begin = Long.toString(todayDate - dateCorrection);
                }
                if (end.equals("")) {
                    end = Long.toString(todayDate);
                }

                sortedTags(selectedOrder, isAscending, begin, end);

                getDialog().dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /** order - name/date, orderType - asc/des, begin/end - dates in msec */
    private void sortedTags(int order, boolean orderType, String begin, String end) {
        MainFragment fragment = new MainFragment();
        FragmentTransaction ft;
        ft = (getActivity()).getSupportFragmentManager().beginTransaction();
        fragment.setFragId(1);
        fragment.setOrderType(orderType);
        fragment.setOrderNumber(order);
        fragment.setBeginSort(begin);
        fragment.setEndSort(end);
        fragment.setMenuNumber(1);
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private void setDate(final EditText editText, final TextView viewText) {
        DatePickerDialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar = setDefaultTime(calendar);
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                getDateInMilliseconds(editText, viewText);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(todayDate);
        dialog.show();
    }

    private void getDateInMilliseconds(EditText edit_field, TextView text) {
        long currentDateInMillisec = calendar.getTimeInMillis();
        String str = (DateUtils.formatDateTime(getContext(), currentDateInMillisec,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR ));
        String msec = Long.toString(currentDateInMillisec);
        //Toast.makeText(getContext(), "" + currentDateFromMillisec, Toast.LENGTH_SHORT).show();
        edit_field.setText(str);
        text.setText(msec);
    }

    private String convertMillisecToDate(String msec) {
        if (msec != "" && !msec.isEmpty()) {
            long date_in_msec = Long.parseLong(msec);
            return (DateUtils.formatDateTime(getContext(), date_in_msec, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        } else {
            return "";
        }
    }

    public static Calendar setDefaultTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }
}























