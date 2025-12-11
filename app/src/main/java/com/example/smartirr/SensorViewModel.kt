package com.example.smartirr

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SensorViewModel: ViewModel() {

    companion object {
        val TAG = "Sensor ViewModel"
    }
    private val _sensor = MutableLiveData(SensorData())
    val sensor: LiveData<SensorData> = _sensor

    private val _triggerNotification = MutableLiveData<Boolean>()
    val triggerNotification: LiveData<Boolean> = _triggerNotification

    fun updateFromJson(j: org.json.JSONObject) {
        val cur = _sensor.value ?: SensorData()

        // store previous
        cur.prevTemp = cur.temp
        cur.prevHumidity = cur.humidity
        cur.prevWaterLevel = cur.waterLevel
        cur.prevMoisture = cur.moisture

        if (j.has("TempC")) cur.temp = j.optDouble("TempC")
        if (j.has("Humidity")) cur.humidity = if (j.opt("Humidity") is Number) j.optInt("Humidity") else j.optInt("Humidity", cur.humidity ?: 0)
        if (j.has("WaterLevel")) cur.waterLevel = j.optInt("WaterLevel")
        if (j.has("Moisture")) cur.moisture = j.optInt("Moisture")

        _sensor.postValue(cur)
        Log.i(TAG,"AFter cureee ${cur}")

        if (cur.waterLevel != null && cur.waterLevel!! > 1500) {
            _triggerNotification.postValue(true)
        }
    }

}