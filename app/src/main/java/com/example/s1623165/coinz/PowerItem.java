package com.example.s1623165.coinz;

// The Power item is used to be displayed in the Power-ups
public class PowerItem {

    private int powerImageResource;
    private String title;
    private int price;

    public PowerItem(int image, String title, int price) {
        this.powerImageResource = image;
        this.title = title;
        this.price = price;
    }

    public int getPowerImageResource() { return powerImageResource; }

    public String getTitle() { return  this.title; }

    public int getPrice() { return this.price; }
}
