package tw.mdo.mqttclient

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.UnsupportedEncodingException

class MainActivity : AppCompatActivity() {
    var brokerAddress: EditText? = null
    var textToSend: EditText? = null
    var topicToSend: EditText? = null
    var subscriptionTopic: EditText? = null
    var connectButton: Button? = null
    var sendButton: Button? = null
    var subscribeButton: Button? = null
    var receivedMessage: TextView? = null
    var connectionStatus: TextView? = null
    var client: MqttAndroidClient? = null
    var serverURL = "tcp://broker.hivemq.com:1883"
    var topic = "mqtt/topic"
    var sTopic = "mqtt/sensorData"
    var connectionFlag = false

    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val actionBar = supportActionBar
        actionBar?.setLogo(getDrawable(R.drawable.mqtt_icon))
        /////////////////////////////////////////////////////////////////
        brokerAddress = findViewById(R.id.broker_address)
        textToSend = findViewById(R.id.text_to_send)
        topicToSend = findViewById(R.id.topic_to_send)
        subscriptionTopic = findViewById(R.id.subscription_topic)
        subscribeButton = findViewById(R.id.subscribe_button)
        connectButton = findViewById(R.id.connect_to_broker_button)
        sendButton = findViewById(R.id.send_button)
        receivedMessage = findViewById(R.id.received_message)
        connectionStatus = findViewById(R.id.connection_status)
        /////////////////////////////////////////////////////////////////
        connectButton.setOnClickListener(View.OnClickListener {
            serverURL = "tcp://" + brokerAddress.getText().toString() + ":1883"
            connectToBroker()
        })
        /////////////////////////////////////////////////////////////////
        sendButton.setOnClickListener(View.OnClickListener {
            topic = topicToSend.getText().toString()
            sendMessage(topic)
        })
        /////////////////////////////////////////////////////////////////
        subscribeButton.setOnClickListener(View.OnClickListener {
            sTopic = subscriptionTopic.getText().toString()
            subscribeToTopic(sTopic)
        })
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun connectToBroker() {
        val clientId = MqttClient.generateClientId()
        client = MqttAndroidClient(this.applicationContext, serverURL, clientId)
        try {
            val token = client!!.connect()
            token.actionCallback = object : IMqttActionListener {
                @SuppressLint("SetTextI18n")
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    connectionStatus!!.text = "Connected To $serverURL"
                    connectionFlag = true
                    sendButton!!.isEnabled = true
                    subscribeButton!!.isEnabled = true
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Toast.makeText(applicationContext, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun sendMessage(topic: String?) {
        val payload = textToSend!!.text.toString()
        val encodedPayload: ByteArray
        try {
            encodedPayload = payload.toByteArray(charset("UTF-8"))
            val message = MqttMessage(encodedPayload)
            client!!.publish(topic, message)
            Toast.makeText(applicationContext, "Sent", Toast.LENGTH_SHORT).show()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun subscribeToTopic(topic: String?) {
        try {
            if (client!!.isConnected) {
                client!!.subscribe(topic, 0)
                Toast.makeText(applicationContext, "Subscribed", Toast.LENGTH_SHORT).show()
                client!!.setCallback(object : MqttCallback {
                    @SuppressLint("SetTextI18n")
                    override fun connectionLost(cause: Throwable) {
                        connectionStatus!!.text = "Connection Failed"
                        connectionFlag = false
                    }

                    @Throws(Exception::class)
                    override fun messageArrived(topic: String, message: MqttMessage) {
                        receivedMessage!!.text = message.toString()
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken) {}
                })
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (connectionFlag) {
            try {
                val disconnectToken = client!!.disconnect()
                disconnectToken.actionCallback = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        finish()
                    }
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
            connectionFlag = false
        }
    } ////////////////////////////////////////////////////////////////////////////////////////////////
}