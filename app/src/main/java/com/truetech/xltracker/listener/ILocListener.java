package com.truetech.xltracker.listener;

import android.location.Location;
import android.location.LocationListener;

/**
 * Created by Ajwar on 22.06.2017.
 */
public interface ILocListener extends LocationListener {
    Location getCurrentLoc();
    void setCurrentLoc(Location currentLoc);
}
