package com.rentapup.web;

import com.mongodb.*;
import com.rentapup.web.obj.Booking;
import com.rentapup.web.obj.Dog;
import com.rentapup.web.obj.Query;
import com.rentapup.web.obj.Renter;
import org.bson.types.ObjectId;

import java.awt.print.Book;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by elijahstaple on 4/22/17.
 */
class QueryHelper {
    static String testAuth(DBCollection authData, String username, String password){
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        byte[] passwordHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        DBObject searchquery = QueryBuilder.start("username").is(username)
                .and("passwordHash").is(passwordHash).get();
        DBCursor cursor = authData.find(searchquery, QueryBuilder.start("_id").is(0).and("userId").is(1).get());

        if(cursor.count() == 1){
            return ((ObjectId) cursor.next().get("userId")).toHexString();
        }
        return null;
    }

    static String addBooking(DBCollection bookingData, DBCollection dogData, DBCollection renterData, String start, String end, String dog, String user){

        ObjectId dogId = DBHandling.getDogId(dogData, dog);
        ObjectId renterId = DBHandling.getRenterId(renterData, user);
        Date startDate = DBHandling.getDate(start);
        Date endDate = DBHandling.getDate(end);

        if(dogAvailable(dogData, bookingData, dog, start, end) == "Success") {
            Booking booking = new Booking(startDate, endDate, dogId, renterId);
            bookingData.insert(booking);
            return "Success";
        }
        return null;
    }

    static String dogAvailable(DBCollection dogData, DBCollection bookingData, String dogName, String start, String end){
        ObjectId dogId = DBHandling.getDogId(dogData, dogName);
        Date startDate = DBHandling.getDate(start);
        Date endDate = DBHandling.getDate(end);

        //start time occurs during existing booking
        DBObject sdm = QueryBuilder.start("startTime").lessThanEquals(startDate).and("endTime").greaterThanEquals(startDate).and("dogId").is(dogId).get();
        //existing booking ends during attempted booking
        DBObject etm = QueryBuilder.start("endTime").greaterThanEquals(startDate).and("endTime").lessThanEquals(endDate).and("dogId").is(dogId).get();
        //existing booking starts during attempted booking
        DBObject stm = QueryBuilder.start("startTime").greaterThanEquals(startDate).and("startTime").lessThan(endDate).and("dogId").is(dogId).get();
        //end time occurs during existing booking
        DBObject edm = QueryBuilder.start("startTime").lessThanEquals(startDate).and("endTime").greaterThanEquals(startDate).and("dogId").is(dogId).get();

        DBCursor sdmcursor = bookingData.find(sdm);
        DBCursor etmcursor = bookingData.find(etm);
        DBCursor edmcursor = bookingData.find(edm);
        DBCursor stmcursor = bookingData.find(stm);
        if(sdmcursor.count() > 0 || edmcursor.count() > 0 || stmcursor.count() > 0 || etmcursor.count() > 0 ){
            return null;
        }
        return "Success";
    }

    static Map<String,String> dogAvailableByTime(DBCollection dogData, DBCollection bookingData, String start, String end){
        List<String> availableDogNames = new ArrayList<>();
        List<String> availableDogIds = new ArrayList<>();
        Date startDate = DBHandling.getDate(start);
        Date endDate = DBHandling.getDate(end);

        //start time occurs during existing booking
        DBObject sdm = QueryBuilder.start("startTime").lessThanEquals(startDate).and("endTime").greaterThanEquals(startDate).get();
        //existing booking ends during attempted booking
        DBObject etm = QueryBuilder.start("endTime").greaterThanEquals(startDate).and("endTime").lessThanEquals(endDate).get();
        //existing booking starts during attempted booking
        DBObject stm = QueryBuilder.start("startTime").greaterThanEquals(startDate).and("startTime").lessThanEquals(endDate).get();
        //end time occurs during existing booking
        DBObject edm = QueryBuilder.start("startTime").lessThanEquals(startDate).and("endTime").greaterThanEquals(startDate).get();

        DBCursor sdmcursor = bookingData.find(sdm);
        DBCursor etmcursor = bookingData.find(etm);
        DBCursor edmcursor = bookingData.find(edm);
        DBCursor stmcursor = bookingData.find(stm);

        DBObject searchDog = QueryBuilder.start().get();
        DBCursor dogCursor = bookingData.find(searchDog);
        DBCursor dogNCursor = dogData.find(searchDog);

        Map<String,String> myMap = new HashMap<String,String>();
        while(dogCursor.hasNext()){
            String dogId = dogCursor.next().get("dogId").toString();
            ObjectId id = new ObjectId(dogId);
            String dogName = DBHandling.getDogName(dogData, id);
            myMap.put(dogId, dogName);
        }

        ArrayList<DBCursor> cursors = new ArrayList<>();
        cursors.add(sdmcursor);
        cursors.add(etmcursor);
        cursors.add(edmcursor);
        cursors.add(stmcursor);
        Integer count = 0;
        Integer cur = 0;
        for (DBCursor cursor: cursors) {
            if(cursor.count() > 0) {
                count = 0;
                while(cursor.hasNext()){
                    cursor.next();
                    for (String mapId: myMap.keySet()) {
                        if(((ObjectId) cursor.curr().get("dogId")).toHexString().equals(mapId)){
                            myMap.remove(mapId);
                            break;
                        }
                        count++;
                    }
                }
            }
            cur++;
        }
        return myMap;
    }

    static String searchByDog(DBCollection bookingData, DBCollection dogData, String dog, String start, String end){
        if(dogAvailable(dogData, bookingData, dog, start, end) != null){
            String returning = String.format("The dog %s is available from %s to %s", dog, DBHandling.getDate(start), DBHandling.getDate(end));
            return returning;
        }

        return null;
    }

    static Map<String,String> searchByTime(DBCollection bookingData, DBCollection dogData, String start, String end){

        if(dogAvailableByTime(dogData, bookingData, start, end) != null){
            Map<String,String> returning = dogAvailableByTime(dogData, bookingData, start, end);
            System.out.print("returning");
            System.out.println(returning);
            return returning;
        }

        return null;
    }

    static String newRenter(DBCollection col, String name, int age) {
        Renter renter = new Renter(name, age, "", "/assets/images/default.png");
        col.insert(renter);
        return renter.getid().toHexString();
    }

    static Query<String, Object> renterProfile(DBCollection col, String id) {
        Query<String, Object> results;
        DBObject query = QueryBuilder.start("_id").is(new ObjectId(id)).and("type").is(2).get();
        DBCursor cursor = col.find(query);
        if(cursor.count() == 1){
            Renter renter =  new Renter(cursor.next());
            results = new Query<String, Object>();
            results.put("id", renter.getid().toHexString());
            results.put("name", renter.getname());
            results.put("age", renter.getage());
            results.put("description", renter.getdescription());
            results.put("location", renter.getlocation().toString());
        } else {
            results = null;
        }
        return results;
    }

    static Query<String, ArrayList> getRenterBookings(DBCollection bookingData, String renterId) {
        Query<String, ArrayList> results = new Query<>();
        ArrayList<String> res1 = new ArrayList<>();
        ArrayList<String> res2 = new ArrayList<>();
        DBObject searchquery = QueryBuilder.start("renterId").is(renterId).get();
        DBCursor cursor = bookingData.find(searchquery);
        while(cursor.hasNext()) {
            System.out.println(((ObjectId) cursor.next().get("dogId")).toHexString());
            res1.add(cursor.curr().toString());
            res2.add(((ObjectId) cursor.curr().get("dogId")).toHexString());
        }
        results.put("1", res1);
        results.put("2", res2);
        return results;
    }

    static ArrayList<String> getDogNames(DBCollection dogData, ArrayList<String> bookings) {
        ArrayList<String> results = new ArrayList<>();
        Integer n = 2;
        for (String booking : bookings) {
            DBObject searchquery = QueryBuilder.start("_id").is(new ObjectId(booking)).get();
            DBCursor cursor = dogData.find(searchquery);
            if (cursor.hasNext())
                results.add(((BasicDBObject) cursor.next()).getString("name"));
        }
        return results;
    }

    static String cancelBooking(DBCollection bookingData, String bookingId) {
        DBObject searchquery = QueryBuilder.start("_id").is(new ObjectId(bookingId)).get();//.and("startDate").greaterThan(new Date(System.currentTimeMillis())).get();
//        DBCursor cursor = bookingData.find(searchquery);
        bookingData.remove(searchquery);
//        if(cursor.hasNext()) return "Failed";
        return "Success";
    }
}
