# Smart Irrigation System

**This project implements an IoT-based smart irrigation system using ESP32 modules, MQTT protocol, and ChaCha-Poly encryption for secure data transmission.**

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
