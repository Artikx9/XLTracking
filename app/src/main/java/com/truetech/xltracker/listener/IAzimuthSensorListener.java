package com.truetech.xltracker.listener;

import android.content.Context;
import android.hardware.SensorEventListener;

/**
 * Created by Ajwar on 20.06.2017.
 */
public interface IAzimuthSensorListener extends SensorEventListener {
    void enable(Context context);
    void disable(Context context);
}
