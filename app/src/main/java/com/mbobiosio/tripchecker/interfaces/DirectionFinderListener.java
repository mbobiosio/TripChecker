package com.mbobiosio.tripchecker.interfaces;

import com.mbobiosio.tripchecker.controller.Route;

import java.util.List;

/**
 * Created by Mbuodile Obiosio on 7/17/18
 * cazewonder@gmail.com
 */
public interface DirectionFinderListener {

    void onDirectionFinderStart();
    void onDirectionFinderSuccess(List<Route> route);
}
