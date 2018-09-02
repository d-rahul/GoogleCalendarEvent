package com.hb.googlecalendareventssample;

import com.google.api.client.util.DateTime;

import java.io.Serializable;
import java.util.Date;



public class CalendarEvents implements Serializable {
    public String title;
    public String description;
    public DateTime datetime;
    public String venue;
    public String id;
    public boolean dateOnly;
}
