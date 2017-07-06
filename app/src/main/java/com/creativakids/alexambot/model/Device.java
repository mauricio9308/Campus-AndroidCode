package com.creativakids.alexambot.model;

/**
 * Reference for bluetooth device in the link list
 *
 * Created by Mauricio Lara on 6/25/17.
 */
public class Device {

    private String id;
    private String name;

    public Device(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
