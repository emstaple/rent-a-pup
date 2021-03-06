package com.rentapup.web;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.rentapup.web.obj.Booking;
import com.rentapup.web.obj.Query;
import com.rentapup.web.obj.Renter;
import org.bson.types.ObjectId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

/**
 * Created by callie on 4/27/17.
 */
public class DBHandling {

    static ObjectId getDogId(DBCollection dogData, String dogName){

        DBObject searchquery = QueryBuilder.start("name").is(dogName).get();
        DBCursor dogcursor = dogData.find(searchquery);
        ObjectId dogId = (ObjectId) dogcursor.one().get("_id");
        return dogId;
    }
    static String getDogName(DBCollection dogData, ObjectId dogId){

        DBObject searchquery = QueryBuilder.start("_id").is(dogId).get();
        DBCursor dogcursor = dogData.find(searchquery);
        String dogName = dogcursor.one().get("name").toString();
        return dogName;
    }

    static ObjectId getRenterId(DBCollection renterData, String renterName){

        DBObject searchquery = QueryBuilder.start("name").is(renterName).get();
        DBCursor rentercursor = renterData.find(searchquery);
        ObjectId renterId = (ObjectId) rentercursor.one().get("_id");
        return renterId;
    }



    static Date getDate(String time){
               
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd h:mm");
        //DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm");
        try {
            Date newDate = formatter.parse(time);
            return newDate;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

}
