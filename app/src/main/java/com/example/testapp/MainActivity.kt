package com.example.testapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testapp.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.nio.charset.StandardCharsets
import java.util.*

import android.app.NotificationManager
import android.content.ComponentName
import android.os.Build
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {
    var activityContext: Context? = null
    private val wearableAppCheckPayload = "AppOpenWearable"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var wearableDeviceConnected: Boolean = false

    private var currentAckFromWearForAppOpenCheck: String? = null
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"

    private val TAG_GET_NODES: String = "getnodes1"
    private val TAG_MESSAGE_RECEIVED: String = "receive1"

    private var messageEvent: MessageEvent? = null
    private var wearableNodeUri: String? = null

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if(!isNotificationPermissionGranted()){
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }


        activityContext = this
        wearableDeviceConnected = false

//      안드로이드에서 Wear가 연결되어 있는지 확인하고 띄워주는 기능
        binding.checkwearablesButton.setOnClickListener {
            if (!wearableDeviceConnected) {
                val tempAct: Activity = activityContext as MainActivity
                //Couroutine
                initialiseDevicePairing(tempAct)
            }
        }


//        안드로이드상에서 메시지를 보내는 기능
//        binding.sendmessageButton.setOnClickListener {
//            if (wearableDeviceConnected) {
//                if (binding.messagecontentEditText.text!!.isNotEmpty()) {
//
//                    val nodeId: String = messageEvent?.sourceNodeId!!
//                    // Set the data of the message to be the bytes of the Uri.
//                    val payload: ByteArray =
//                        binding.messagecontentEditText.text.toString().toByteArray()
//
//                    // Send the rpc
//                    // Instantiates clients without member variables, as clients are inexpensive to
//                    // create. (They are cached and shared between GoogleApi instances.)
//                    val sendMessageTask =
//                        Wearable.getMessageClient(activityContext!!)
//                            .sendMessage(nodeId, MESSAGE_ITEM_RECEIVED_PATH, payload)
//
//                    sendMessageTask.addOnCompleteListener {
//                        if (it.isSuccessful) {
//                            Log.d("send1", "Message sent successfully")
//                            val sbTemp = StringBuilder()
//                            sbTemp.append("\n")
//                            sbTemp.append(binding.messagecontentEditText.text.toString())
//                            sbTemp.append(" (Sent to Wearable)")
//                            Log.d("receive1", " $sbTemp")
//                            binding.messagelogTextView.append(sbTemp)
//
//                            binding.scrollviewText.requestFocus()
//                            binding.scrollviewText.post {
//                                binding.scrollviewText.scrollTo(0, binding.scrollviewText.bottom)
//                            }
//                        } else {
//                            Log.d("send1", "Message failed.")
//                        }
//                    }
//                } else {
//                    Toast.makeText(
//                        activityContext,
//                        "Message content is empty. Please enter some message and proceed",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
        }

    private fun isNotificationPermissionGranted() : Boolean{
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.isNotificationListenerAccessGranted(ComponentName(application,MyNotificationListenerService::class.java))
        }
        else{
            return NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(applicationContext.packageName)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initialiseDevicePairing(tempAct: Activity) {
        //Coroutine
        launch(Dispatchers.Default) {
            var getNodesResBool: BooleanArray? = null

            try {
                getNodesResBool =
                    getNodes(tempAct.applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //  페어링 상태에 대한 알림 플로팅을 띄우는 기능
            withContext(Dispatchers.Main) {
                if (getNodesResBool!![0]) {
                    //if message Acknowlegement Received
                    if (getNodesResBool[1]) {
                        Toast.makeText(
                            activityContext,
                            "Wearable device paired and app is open. Tap the \"Send Message to Wearable\" button to send the message to your wearable device.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.deviceconnectionStatusTv.text =
                            "Wearable device paired and app is open."
                        binding.deviceconnectionStatusTv.visibility = View.VISIBLE
                        wearableDeviceConnected = true
//                        binding.sendmessageButton.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            activityContext,
                            "A wearable device is paired but the wearable app on your watch isn't open. Launch the wearable app and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.deviceconnectionStatusTv.text =
                            "Wearable device paired but app isn't open."
                        binding.deviceconnectionStatusTv.visibility = View.VISIBLE
                        wearableDeviceConnected = false
//                        binding.sendmessageButton.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(
                        activityContext,
                        "No wearable device paired. Pair a wearable device to your phone using the Wear OS app and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.deviceconnectionStatusTv.text =
                        "Wearable device not paired and connected."
                    binding.deviceconnectionStatusTv.visibility = View.VISIBLE
                    wearableDeviceConnected = false
//                    binding.sendmessageButton.visibility = View.GONE
                }
            }
        }
    }


//  연결된 Wear의 노드를 가져오는 부분
    private fun getNodes(context: Context): BooleanArray {
        val nodeResults = HashSet<String>()
        val resBool = BooleanArray(2)
        resBool[0] = false //nodePresent
        resBool[1] = false //wearableReturnAckReceived
        val nodeListTask =
            Wearable.getNodeClient(context).connectedNodes
        try {
            // Block on a task and get the result synchronously (because this is on a background thread).
            val nodes =
                Tasks.await(
                    nodeListTask
                )
            Log.e(TAG_GET_NODES, "Task fetched nodes")
            for (node in nodes) {
                Log.e(TAG_GET_NODES, "inside loop")
                nodeResults.add(node.id)
                try {
                    val nodeId = node.id
                    // Set the data of the message to be the bytes of the Uri.
                    val payload: ByteArray = wearableAppCheckPayload.toByteArray()
                    // Send the rpc
                    // Instantiates clients without member variables, as clients are inexpensive to
                    // create. (They are cached and shared between GoogleApi instances.)
                    val sendMessageTask =
                        Wearable.getMessageClient(context)
                            .sendMessage(nodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
                    try {
                        // Block on a task and get the result synchronously (because this is on a background thread).
                        val result = Tasks.await(sendMessageTask)
                        Log.d(TAG_GET_NODES, "send message result : $result")
                        resBool[0] = true

                        //Wait for 700 ms/0.7 sec for the acknowledgement message
                        //Wait 1
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(100)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 1")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 2
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(250)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 2")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 3
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(350)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 5")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        resBool[1] = false
                        Log.d(
                            TAG_GET_NODES,
                            "ACK thread timeout, no message received from the wearable "
                        )
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                } catch (e1: Exception) {
                    Log.d(TAG_GET_NODES, "send message exception")
                    e1.printStackTrace()
                }
            } //end of for loop
        } catch (exception: Exception) {
            Log.e(TAG_GET_NODES, "Task failed: $exception")
            exception.printStackTrace()
        }
        return resBool
    }


    override fun onDataChanged(p0: DataEventBuffer) {
    }

    //  Wear에서 받은 메시지를 띄우는 부분
    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        if (p0.path == MESSAGE_ITEM_RECEIVED_PATH){
            val message = String(p0.data)
            Log.d("WearMessageListner", "Received message : $message")

            val notificationListenerService = MyNotificationListenerService()
            val notifications : Array<StatusBarNotification> = notificationListenerService.activeNotifications

            for (sbn in notifications){

                val wExt = Notification.WearableExtender(sbn.notification)
                val act = wExt.actions.firstOrNull() {
                        action -> action.remoteInputs != null && action.remoteInputs.isNotEmpty() &&
                        (action.title.toString().contains("reply", true) || action.title.toString().contains("답장", true))
                }
                if (act != null){
                    notificationListenerService.callResponder(
                        sbn.notification.extras?.getString("android.title"),
                        sbn.notification.extras?.get("android.text"),
                        act,
                        message
                    )
                    break
                }
            }

        }
        try {
            val s =
                String(p0.data, StandardCharsets.UTF_8)
            val messageEventPath: String = p0.path
            Log.d(
                TAG_MESSAGE_RECEIVED,
                "onMessageReceived() Received a message from watch:"
                        + p0.requestId
                        + " "
                        + messageEventPath
                        + " "
                        + s
            )
            if (messageEventPath == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
                currentAckFromWearForAppOpenCheck = s
                Log.d(
                    TAG_MESSAGE_RECEIVED,
                    "Received acknowledgement message that app is open in wear"
                )

                val sbTemp = StringBuilder()
                sbTemp.append(binding.messagelogTextView.text.toString())
                sbTemp.append("\nWearable device connected.")
                Log.d("receive1", " $sbTemp")
                binding.messagelogTextView.text = sbTemp
//                binding.textInputLayout.visibility = View.VISIBLE

                binding.checkwearablesButton.visibility = View.GONE
                messageEvent = p0
                wearableNodeUri = p0.sourceNodeId
            } else if (messageEventPath.isNotEmpty() && messageEventPath == MESSAGE_ITEM_RECEIVED_PATH) {

                try {
                    binding.messagelogTextView.visibility = View.VISIBLE
//                    binding.textInputLayout.visibility = View.VISIBLE
//                    binding.sendmessageButton.visibility = View.VISIBLE

                    val sbTemp = StringBuilder()
                    sbTemp.append("\n")
                    sbTemp.append(s)
                    sbTemp.append(" - (Received from wearable)")
                    Log.d("receive1", " $sbTemp")
                    binding.messagelogTextView.append(sbTemp)

                    binding.scrollviewText.requestFocus()
                    binding.scrollviewText.post {
                        binding.scrollviewText.scrollTo(0, binding.scrollviewText.bottom)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("receive1", "Handled")
        }
    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {
    }


    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
