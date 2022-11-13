package com.polarinsdustries.chatwifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import kotlinx.coroutines.channels.BroadcastChannel

class WifiDirectBroadcastReceiver(private val manager: WifiP2pManager, private val channel:WifiP2pManager.Channel, private val activity: MainActivity): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("ACTION", intent!!.action.toString())
        when(intent!!.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                //val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                //activity.isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                // The peer list has changed! We should probably do something about
                // that.
                Log.d("TAG", "P2P peers changed")
                if(manager!=null){
                    manager.requestPeers(channel, activity.peerListListener)
                }

            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                // Connection state changed! We should probably do something about
                // that.
                if(manager!=null){
                    var networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if(networkInfo!!.isConnected){
                        manager.requestConnectionInfo(channel, activity.connectionInfoListener)
                    }else{
                        activity.textView_Status.text = "No conectado"
                        activity.connection = false
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                /*(activity.supportFragmentManager.findFragmentById(R.id.frag_list) as DeviceListFragment)
                    .apply {
                        updateThisDevice(
                            intent.getParcelableExtra(
                                WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice
                        )
                    }*/
            }
        }
    }

}