package com.example.s1623165.coinz;

public class CoinItem {

    private int coinImageResource;
    private String title;
    private String description;
    private String id;

    public CoinItem(int image, String text1, String text2, String ID) {
        this.coinImageResource = image;
        this.title = text1;
        this.description = text2;
        this.id = ID;
    }

    public int getCoinImageResource() { return coinImageResource; }

    public String getDescription() { return description; }

    public String getTitle() { return title; }

    public String getId() { return id; }
}
