# IoT MQTT to Azure IoT Hub Bridge

This project connects to an **MQTT broker**, fetches messages, and forwards them to **Azure IoT Hub** at a fixed interval (every 30 seconds).
It is implemented as a simple **Java Maven project**.

---

## 🚀 Features

* Connects to an MQTT broker and subscribes to a topic.
* Forwards each incoming MQTT message to Azure IoT Hub.
* Runs continuously until manually stopped.
* Sends data every **30 seconds** to Azure IoT Hub.
* Configurable through environment variables or code.

---

## 🛠️ Requirements

* Java 11 or higher
* Maven 3.6+
* Azure IoT Hub set up with a registered device (Device Connection String required)
* An MQTT broker running (e.g., Mosquitto, HiveMQ)

---

## ⚙️ Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-repo/iot-mqtt-bridge.git
cd iot-mqtt-bridge
```

### 2. Configure

Update the following variables in the code (`MqttToIotHubBridge.java`):

```java
// MQTT Config
private static final String MQTT_BROKER = "tcp://localhost:1883"; // Your MQTT broker
private static final String MQTT_TOPIC = "energy/data";           // Your topic

// Azure IoT Hub Device Connection String
private static final String IOT_HUB_CONNECTION_STRING = "<your-device-connection-string>";
```

---

## ▶️ Run the Project

### Using Maven

```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.MqttToIotHubBridge"
```

### Using JAR

```bash
mvn clean package
java -jar target/iot-mqtt-bridge-1.0-SNAPSHOT.jar
```

---

## 📡 Data Flow

1. **MQTT Broker → Java App**: Subscribes to MQTT topic and fetches JSON payloads.
2. **Java App → Azure IoT Hub**: Wraps the payload and sends it to IoT Hub every 30 seconds.
3. **Azure IoT Explorer**: Use [Azure IoT Explorer](https://learn.microsoft.com/en-us/azure/iot-fundamentals/howto-use-iot-explorer) to view incoming messages.

---

## 🔄 Example Input (from MQTT)

```json
{
  "body": {
    "timestamp": 1756121229,
    "hardwareId": "3",
    "tag": "energy",
    "value": 11904.837,
    "unit": "kWh"
  },
  "enqueuedTime": "Mon Aug 25 2025 17:02:24 GMT+0530 (India Standard Time)",
  "properties": {
    "$.cdid": "Socomech"
  }
}
```

## ✅ Example Output (to IoT Hub)

```json
{
  "datatype": "energy",
  "timestamp": 1756121302,
  "value": -5.8639383,
  "deviceID": "3",
  "tagName": "energy",
  "deviceName": "Socomech"
}
```

---

## ⚡ Customization

* Change MQTT broker or topic inside `MqttToIotHubBridge.java`.
* Adjust message interval (default: 30 seconds) by updating the `SCHEDULE_PERIOD` variable.
* Add additional mappings in the message transformation logic as required.

---

## 🛑 Stop the Program

Press `CTRL + C` in the terminal to stop message forwarding.

---
