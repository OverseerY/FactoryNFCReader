package com.yaroslav.factorynfcreader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {
    private List<Ticket> tagList;
    private Context context;
    private FirebaseFirestore mFirestore;

    /** Конструктор с параметрами - список объектов меток, контекст приложения, ссылка на Firestore */
    public TicketAdapter(List<Ticket> tagList, Context context, FirebaseFirestore mFirestore) {
        this.tagList = tagList;
        this.context = context;
        this.mFirestore = mFirestore;
    }

    @NonNull
    @Override
    public TicketAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_item, parent, false);
        return new ViewHolder(view);
    }

    /** Метод - связывание полей макета с конкретными значениями объекта */
    @Override
    public void onBindViewHolder(@NonNull TicketAdapter.ViewHolder holder, int position) {
        Ticket ticket = tagList.get(position);

        holder.tiTitle.setText(ticket.getTagName());
        holder.tiTime.setText(converteTime(ticket.getReadTime())); //Convert to normal string
    }

    @Override
    public int getItemCount() {
        return tagList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tiTitle, tiTime;

        ViewHolder(View view) {
            super(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Ticket selected_tag = tagList.get(getAdapterPosition());
                }
            });

            tiTitle = view.findViewById(R.id.tiTitle);
            tiTime = view.findViewById(R.id.tiTime);
        }
    }

    /** Метод - преобразование времени из миллисекунд в указанные формат */
    private String converteTime(String value) {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long temp = Long.parseLong(value);
        Date date = new Date(temp);
        return format.format(date);
    }
}
