package com.example.Listen.database;

//menampilkan history music yang telah diputar
public class DbSchema {
    public static class RecentHistory {
        public static String TABLE_NAME = "RecentHistory";
       public static class Cols {
            public static final String ID = "id";
            public static final String TIME_PLAYED = "time_played";
        }
    }

    public static class playBackQueue {
    }
}
