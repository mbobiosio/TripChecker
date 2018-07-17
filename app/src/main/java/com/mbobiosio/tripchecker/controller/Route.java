package com.mbobiosio.tripchecker.controller;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by Mbuodile Obiosio on 7/17/18
 * cazewonder@gmail.com
 */
public class Route {

    public Distance distance;
    public Duration  duration;
    public String endAddress;
    public LatLng endLocation;
    public String startAddress;
    public LatLng startLocation;

    public List<LatLng> points;
}
