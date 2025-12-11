package com.example.smartirr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import com.example.smartirr.databinding.ActivityMainBinding
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import org.bouncycastle.crypto.engines.ChaChaEngine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.json.JSONObject


lateinit var mqttClient: Mqtt5BlockingClient
lateinit var mqttAsyncClient: Mqtt5AsyncClient
private val sensorData = SensorData()

lateinit var vm: SensorViewModel


@SuppressLint("StaticFieldLeak")
private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "Main Activity"
        private val CHACHA_KEY = byteArrayOf(
            0x2a,0x91.toByte(),0x7c,0x4d,0x9f.toByte(),0x13,0x55,0x60,
            0x8a.toByte(),0x01,0x22,0x4f,0xc7.toByte(),0x3e,0x18,0xb2.toByte(),
            0x9d.toByte(),0x77,0x0a,0x46,0x51,0x3c,0x6e,0x99.toByte(),
            0xf0.toByte(),0xab.toByte(),0x04,0x18,0x88.toByte(),0x33,0x5c,0x71
        )

        val CHANNEL_ID = "waterLevelAlert"
        val CHANNEL_NAME = "waterLevelAlert"
        val CHANNEL_DESCRIPTION = "Will Appear at Detection of Rain Water"

        val NOTIFICATION_ID = 13
    }

    @SuppressLint("MissingInflatedId", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.sensor = sensorData
        connect()

        vm = run {
            ViewModelProvider(this)[SensorViewModel::class.java]
        }


        vm.sensor.observe(this) { sensor ->
            binding.sensor = sensor
            binding.statusText.text = "Status: last update ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setDescription(CHANNEL_DESCRIPTION)
            val notificationManager =
                getSystemService<NotificationManager?>(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        vm.triggerNotification.observe(this) { shouldNotify ->
            if (shouldNotify == true) {
                showNotification()
            }
        }

    }

    fun connect() {
        mqttClient = Mqtt5Client.builder()
            .serverHost("10.234.3.129") // Replace with your broker address
            .serverPort(1883) // Default MQTT port
            .identifier("android-client-id")
            .buildBlocking();

        try {
            mqttClient.connect()
            subscribe()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun subscribe() {

        mqttAsyncClient = mqttClient.toAsync()

        Log.i(TAG,"mein subscribe ")

        mqttAsyncClient.subscribeWith()
            .topicFilter("test")
            .callback { publish ->
                Log.i(TAG,"messagennnnn")
                val bytes = publish.payloadAsBytes // DO NOT convert to String here
                Log.d(TAG, "Message received on topic ${publish.topic}: ${bytes?.size ?: 0} bytes")
                handleIncomingMessage(bytes)
            }
            .send()
    }

    private fun handleIncomingMessage(rawBytes: ByteArray?) {

        if (rawBytes == null) {
            Log.e(TAG, "Empty payload")
            return
        }

        try {
            val plaintextJsonString: String = when {
                looksLikeEncrypted(rawBytes) -> {
                    // first 8 bytes = nonce, rest = ciphertext
                    if (rawBytes.size <= 8) {
                        throw IllegalArgumentException("Encrypted payload too short")
                    }
                    val nonce = rawBytes.copyOfRange(0, 8)
                    val cipher = rawBytes.copyOfRange(8, rawBytes.size)
                    val plain = chacha20Decrypt(CHACHA_KEY, nonce, cipher)
                    String(plain, Charsets.UTF_8)
                }
                else -> {
                    // assume payload is plaintext UTF-8 JSON
                    String(rawBytes, Charsets.UTF_8)
                }
            }

            Log.i(TAG,"plain text bbbbbb ${plaintextJsonString}")

            // Normalize quotes if needed (your existing logic)
            val normalized = if (plaintextJsonString.trim().startsWith("{") && plaintextJsonString.contains('\'')) {
                plaintextJsonString.replace('\'', '"')
            } else plaintextJsonString

            val j = JSONObject(normalized)
            vm.updateFromJson(j) // your existing VM update call
            Log.d(TAG, "JSON parsed and VM updated")
        } catch (e: Exception) {
            Log.e(TAG, "JSON/decrypt error: ${e.message}", e)
        }
    }

    // Heuristic to decide if payload is encrypted binary (adjust as needed)
    private fun looksLikeEncrypted(bytes: ByteArray): Boolean {
        // Your ESP32 publishes 8-byte nonce + ASCII JSON cipher (likely containing non-printable bytes).
        // If the payload contains many non-printable bytes, treat as encrypted.
        var nonPrintable = 0
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c < 0x09 || (c in 0x0E..0x1F) || c > 0x7E) nonPrintable++
        }

        Log.i(TAG,"In lookslike${nonPrintable > bytes.size / 5} ")
        // if > 20% of bytes non-printable treat as binary encrypted
        return nonPrintable > bytes.size / 5
    }

    // Decrypt using BouncyCastle ChaChaEngine (20 rounds)
    private fun chacha20Decrypt(key: ByteArray, nonce8: ByteArray, cipher: ByteArray): ByteArray {
        require(key.size == 32) { "Key must be 32 bytes" }
        require(nonce8.size == 8) { "Nonce must be 8 bytes (as used by your ESP32)" }

        // ChaChaEngine expects a 64-bit nonce when using the "8 byte IV" variant.
        val engine = ChaChaEngine(20)
        val params = ParametersWithIV(KeyParameter(key), nonce8)
        engine.init(false, params) // false for decryption/stream (stream cipher uses same op)

        val out = ByteArray(cipher.size)
        engine.processBytes(cipher, 0, cipher.size, out, 0)
        return out
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification() {
        val largeIcon = BitmapFactory.decodeResource(resources, com.example.smartirr.R.drawable.ic_water_drop) // use your icon

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop_2) // small icon (must be monochrome for Android 13+)
            .setLargeIcon(largeIcon) // large, colorful icon
            .setContentTitle ("Rain Water Detected")
            .setContentText("Data received from water level sensor — rain water detected!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // 🔊 default system sound
            .setAutoCancel(true) // Dismisses notification when tapped

        // Intent to open MainActivity on tap
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

}