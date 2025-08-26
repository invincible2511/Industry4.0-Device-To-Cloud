package com.socomech;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.sdk.iot.device.*;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MqttToIotHubBridge {

	// ================== CONFIG ==================
	private static final String IOT_HUB_DEVICE_CONNECTION_STRING = "HostName=<hostname>.azure-devices.net;DeviceId=Socomech;SharedAccessKey=<sharedAccessKey>";
	private static final Logger logger = LoggerFactory.getLogger(MqttToIotHubBridge.class);
	
	private static final String MQTT_BROKER = "<MQTT Broker>";
	private static final String MQTT_TOPIC = "<Topic>";
	private static final String MQTT_USERNAME = "Username";
	private static final String MQTT_PASSWORD = "Abcd@1234";

	private static final int SEND_INTERVAL_SECONDS = 30;

	private static volatile String lastPayload = null;

	public static void main(String[] args) {
		try {
			// ===== 1. Connect to Azure IoT Hub =====
			DeviceClient iotHubClient = new DeviceClient(IOT_HUB_DEVICE_CONNECTION_STRING, IotHubClientProtocol.MQTT);
			iotHubClient.open(true);
			logger.info("Connected to Azure IoT Hub");

			// ===== 2. Connect to HiveMQ MQTT broker =====
			MqttClient mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId());
			MqttConnectOptions mqttOptions = new MqttConnectOptions();
			mqttOptions.setUserName(MQTT_USERNAME);
			mqttOptions.setPassword(MQTT_PASSWORD.toCharArray());
			mqttOptions.setCleanSession(true);

			mqttClient.setCallback(new MqttCallback() {
				@Override
				public void connectionLost(Throwable cause) {
					logger.error("MQTT connection lost: {} " , cause.getMessage());
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) {
					lastPayload = new String(message.getPayload());
					logger.info("Received from MQTT: {} " , lastPayload);
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					// Not used for subscriber
				}
			});

			mqttClient.connect(mqttOptions);
			mqttClient.subscribe(MQTT_TOPIC);
			logger.info("Connected to MQTT broker and subscribed to topic");

			// ===== 3. Schedule sending to IoT Hub every 30 seconds =====
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(() -> {
				try {
					if (lastPayload != null) {
						// Parse MQTT JSON
						ObjectMapper mapper = new ObjectMapper();
						JsonNode root = mapper.readTree(lastPayload);
						long timestamp = root.path("timestamp").asLong();
						String hardwareId = root.path("hardwareId").asText();
						JsonNode readings = root.path("readings");

						// For each reading, create a normalized message
						Iterator<String> fieldNames = readings.fieldNames();
						while (fieldNames.hasNext()) {
							String tag = fieldNames.next();
							double value = readings.get(tag).asDouble();

							// Build normalized JSON
							ObjectNode normalized = mapper.createObjectNode();
							normalized.put("timestamp", timestamp);
							normalized.put("tagName", tag);
							normalized.put("value", value);
							normalized.put("deviceName", "Socomech");
							normalized.put("deviceID", hardwareId);

							// Send to IoT Hub
							iotHubClient.sendEventAsync(new Message(normalized.toString()), (sentMessage, e, context) -> {
								if (e == null) {
									logger.info("Sent normalized: {}", normalized);
								} else {
									logger.error("Error: {} ", e.getMessage());
								}
							}, null);
						}
					}
				} catch (Exception ex) {
					logger.error("Exception:{} " , ex.getMessage());
				}
			}, 0, SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

			// ===== 4. Keep program running =====
			logger.info("Bridge is running. Press Ctrl+C to exit.");
			Thread.currentThread().join(); // keeps the main thread alive

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
