package com.example.s1623165.coinz;

public class Notification {

    private int noteImageResource;
    private String title;
    private String description;

    public Notification(int image, String text1, String text2) {
        this.noteImageResource = image;
        this.title = text1;
        this.description = text2;
    }

    public int getNoteImageResource() { return noteImageResource; }

    public String getDescription() { return description; }

    public String getTitle() { return title; }

}
