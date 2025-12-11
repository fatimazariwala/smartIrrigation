package com.example.smartirr

data class SensorData(
    var temp: Double? = null,
    var humidity: Int? = null,
    var waterLevel: Int? = null,
    var moisture: Int? = null,

    // prev values for change detection
    var prevTemp: Double? = null,
    var prevHumidity: Int? = null,
    var prevWaterLevel: Int? = null,
    var prevMoisture: Int? = null
) {
    // display helpers (used from binding)
    val tempDisplay: String get() = temp?.let { String.format("%.1f °C", it) } ?: "-- °C"
    val humidityDisplay: String get() = humidity?.let { "$it %" } ?: "-- %"
    val waterLevelDisplay: String get() = waterLevel?.let { "$it ml" } ?: "-- ml"
    val moistureDisplay: String get() = moisture?.let { "$it %" } ?: "-- %"

    val humidityPercent: Int get() = humidity?.coerceIn(0,100) ?: 0
    val moisturePercent: Int get() {
        return moisture?.let {
            val mapped = moisture ?: 0
            mapped.coerceIn(0,100)
        } ?: 0
    }
}
