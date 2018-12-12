package com.example.s1623165.coinz;

import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.mapboxsdk.geometry.LatLng;

public final class Coin {

    private String id;
    private String currency;
    private double value;
    private LatLng location;

    public Coin(Builder builder) {
        this.id = builder.id;
        this.currency = builder.currency;
        this.value = builder.value;
        this.location = builder.location;
    }

    public String getId() {
        return this.id;
    }

    public double getValue() {
        return this.value;
    }

    public String getCurrency() {
        return this.currency;
    }

    public LatLng getLocation() {
        return this.location;
    }

    public static class Builder {
        private String id;
        private String currency;
        private double value;
        private LatLng location;

        public Coin build() {
            return new Coin(this);
        }

        public Builder setID(String ID) {
            this.id = ID;
            return this;
        }
        public Builder setCurrency(String cur) {
            this.currency = cur;
            return this;
        }
        public Builder setValue(double val) {
            this.value = val;
            return this;
        }
        public Builder setLocation(LatLng loc) {
            this.location = loc;
            return this;
        }
    }

    public String toString() {
        Gson gson  = new Gson();
        return gson.toJson(this);
    }

    public void setValue(double newValue) { this.value = newValue; }
    public void setCurrency(String newValue) { this.currency = newValue; }
}


