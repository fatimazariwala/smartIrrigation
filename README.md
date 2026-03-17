# Smart Irrigation System

**This project implements an IoT-based smart irrigation system using ESP32 modules, MQTT protocol, and ChaCha-Poly encryption for secure data transmission.**

<img width="1257" height="890" alt="Screenshot 2026-03-17 at 9 17 49 PM" src="https://github.com/user-attachments/assets/b56fdeb2-5832-49f5-a8b0-f3d0cb957ed2" />

## Sensors Used
- **Temperature sensor** - Monitors environmental temperature changes
- **Humidity sensor** - Detects humidity variations
- **Moisture sensor** - Measures soil moisture to trigger automatic watering
- **Water sensor** - Detects rainfall and controls water pumps

## ESP32 Architecture
- **Subscriber ESP32** - Collects real-time sensor data from the field
- **Publisher ESP32** - Executes actions (pump control, LCD display) based on thresholds

## MQTT Communication Flow
**MQTT protocol enables efficient data exchange:**
- Publisher broadcasts encrypted sensor data
- **Subscriber 1**: ESP32 module performs irrigation actions
- **Subscriber 2**: Mobile app displays structured real-time data

## Security Implementation
**ChaCha-Poly encryption ensures end-to-end data protection:**
- Data encrypted at publisher before transmission
- Key exchange performed upon subscriber connection
- Decryption happens at each subscriber for secure access

  ## Author
  *Aditya Gupta*
  *Fatima Zariwala*
