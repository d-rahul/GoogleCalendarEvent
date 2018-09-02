package com.hb.googlecalendareventssample;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.api.client.util.DateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;



public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ServicesViewHolder> {
    private Context context;
    private ArrayList<CalendarEvents> mCalendarEvents;
    private EventClicked eventClick;

    public EventsAdapter(Activity activity, ArrayList<CalendarEvents> events, EventClicked eventClicked) {
        context = activity;
        mCalendarEvents = new ArrayList<>();
        mCalendarEvents.clear();
        mCalendarEvents = events;
        eventClick = eventClicked;
    }

    @Override
    public ServicesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ServicesViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_calendar_events, parent, false));
    }

    @Override
    public void onBindViewHolder(final ServicesViewHolder holder, final int position) {
        final CalendarEvents events = mCalendarEvents.get(position);
        holder.tvTitle.setText(events.title);
        holder.tvDesc.setText(events.description);
        if (events.dateOnly) {
            holder.tvDateandTime.setText(events.datetime.toString());
        } else {
            holder.tvDateandTime.setText(getEventStartDateTime(events.datetime));
        }
        holder.tvVenue.setText(events.venue);

        holder.llParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (eventClick != null) {
                    eventClick.onEventClickListener(position, events);
                }
            }
        });
    }

    String datetime;

    private String getEventStartDateTime(DateTime eventDateTime) {
        DateFormat original = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        DateFormat target = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date date = original.parse(eventDateTime.toString());
            datetime = target.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return datetime;
    }


    @Override
    public int getItemCount() {
        return mCalendarEvents.size();
    }

//    public void setItems(ArrayList<CalendarEvents> events) {
//        mCalendarEvents.clear();
//        mCalendarEvents = events;
//        notifyDataSetChanged();
//    }

    class ServicesViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle, tvDesc, tvDateandTime, tvVenue;
        private LinearLayout llParent;

        public ServicesViewHolder(View itemView) {
            super(itemView);
            llParent = (LinearLayout) itemView.findViewById(R.id.llParent);
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            tvDesc = (TextView) itemView.findViewById(R.id.tvDesc);
            tvDateandTime = (TextView) itemView.findViewById(R.id.tvDateandTime);
            tvVenue = (TextView) itemView.findViewById(R.id.tvVenue);
        }
    }

}
