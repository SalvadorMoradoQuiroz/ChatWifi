package com.polarinsdustries.chatwifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import org.w3c.dom.Text
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.net.ServerSocket

class MainActivity : AppCompatActivity() {

    lateinit var textView_Status: TextView
    private lateinit var button_OnOff: Button
    private lateinit var button_Discover: Button
    private lateinit var listView_Devices: ListView
    private lateinit var textView_Messages: TextView
    private lateinit var editText_Message: EditText
    private lateinit var imageButton_Send: ImageButton

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    private var peers: ArrayList<WifiP2pDevice> = ArrayList<WifiP2pDevice>()
    private lateinit var deviceNameArray: Array<String>
    private lateinit var deviceArray: Array<WifiP2pDevice>

    private lateinit var socket:Socket

    private lateinit var serverClass: ServerClass
    private lateinit var clientClass: ClientClass

    private  var isHost:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView_Status = findViewById(R.id.textView_Status)
        button_OnOff = findViewById(R.id.button_OnOff)
        button_Discover = findViewById(R.id.button_Discover)
        listView_Devices = findViewById(R.id.listView_Devices)
        textView_Messages = findViewById(R.id.textView_Messages)
        editText_Message = findViewById(R.id.editText_Message)
        imageButton_Send = findViewById(R.id.imageButton_Send)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = WifiDirectBroadcastReceiver(manager, channel, this@MainActivity)

        intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        listeners()
    }

    private fun listeners() {
        button_OnOff.setOnClickListener {
            val intent: Intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivityForResult(intent, 1)
        }

        button_Discover.setOnClickListener {
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    textView_Status.text = "Se inicio la busqueda de dispositivos."
                }
                override fun onFailure(i: Int) {
                    textView_Status.text = "No se inicio la busqueda de dispositivos."
                }
            })
        }

        listView_Devices.setOnItemClickListener(object:OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val device:WifiP2pDevice = deviceArray[position]
                var config:WifiP2pConfig = WifiP2pConfig()
                config.deviceAddress = device.deviceAddress
                manager.connect(channel, config, object:WifiP2pManager.ActionListener{
                    override fun onSuccess() {
                        textView_Status.text = "Conectado a ${device.deviceAddress}"
                    }

                    override fun onFailure(p0: Int) {
                        textView_Status.text = "No conectado"
                    }
                })
            }
        })

        imageButton_Send.setOnClickListener{
            var executor: ExecutorService = Executors.newSingleThreadExecutor()
            var msg:String = editText_Message.text.toString()
            executor.execute(Runnable {
                if(msg!=null && isHost){
                    serverClass.write(msg.toByteArray())
                }else if(msg!=null && !isHost){
                    clientClass.write(msg.toByteArray())
                }
            })
        }
    }

    var peerListListener =
        PeerListListener { wifiP2pDeviceList ->
            if (wifiP2pDeviceList != peers) {
                peers.clear()
                peers.addAll(wifiP2pDeviceList.deviceList)
                deviceNameArray =  Array(wifiP2pDeviceList.deviceList.size) {""}
                deviceArray = Array(wifiP2pDeviceList.deviceList.size) {WifiP2pDevice()}

                var index:Int = 0
                for(device:WifiP2pDevice in wifiP2pDeviceList.deviceList){
                    deviceNameArray[index] = device.deviceName
                    deviceArray[index] = device
                    index++
                }

                var adapter:ArrayAdapter<String> = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, deviceNameArray)
                if(peers.size == 0){
                    textView_Status.text = "No se encontraron dispositivos."
                    return@PeerListListener
                }
            }
        }

    var connectionInfoListener = ConnectionInfoListener {
        val groupOwnerAddress : InetAddress = it.groupOwnerAddress
        if(it.groupFormed && it.isGroupOwner){
            textView_Status.text = "Host"
            isHost = true
            serverClass = ServerClass()
            serverClass.start()
        }else if(it.groupFormed){
            textView_Status.text = "Client"
            isHost = false
            clientClass = ClientClass(groupOwnerAddress)
            clientClass.start()

        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    inner class ClientClass : Thread {
        private var hostAdd: String = ""
        private var inputStream: InputStream? = null
        private var outputStream: OutputStream? = null

        constructor(hosAddress: InetAddress) {
            this.hostAdd = hosAddress.hostAddress
            this@MainActivity.socket = Socket()
        }

        fun write(bytes:ByteArray){
            try {
                outputStream?.write(bytes)
            }catch (e:IOException){
                e.printStackTrace()
            }
        }


        override fun run() {

            try {
                socket.connect(InetSocketAddress(hostAdd, 8888), 500)
                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var executor: ExecutorService = Executors.newSingleThreadExecutor()
            var handler: Handler = Handler(Looper.getMainLooper())

            executor.execute {
                val buffer = ByteArray(1024)
                var bytes: Int
                while (socket != null) {
                    try {
                        bytes = inputStream!!.read(buffer)
                        if (bytes > 0) {
                            val finalBytes = bytes

                            handler.post {
                                val tempMSG: String = String(buffer, 0, finalBytes)
                                textView_Messages.text = tempMSG
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    inner class ServerClass(): Thread() {
        var serverSocket: ServerSocket? = null
        private var inputStream: InputStream? = null
        private var outputStream: OutputStream? = null

        fun write(bytes:ByteArray){
            try {
                outputStream?.write(bytes)
            }catch (e:IOException){
                e.printStackTrace()
            }
        }

        override fun run() {
            try {
                serverSocket = ServerSocket(8888)
                socket = serverSocket!!.accept()

                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()
            }  catch (e: Exception) {
                e.printStackTrace()
            }

            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {
                val buffer = ByteArray(1024)
                var bytes: Int
                while (socket != null) {
                    try {
                        bytes = inputStream!!.read(buffer)
                        if (bytes > 0) {
                            val finalBytes = bytes

                            handler.post {
                                val tempMSG: String = String(buffer, 0, finalBytes)
                                textView_Messages.text = tempMSG
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

}