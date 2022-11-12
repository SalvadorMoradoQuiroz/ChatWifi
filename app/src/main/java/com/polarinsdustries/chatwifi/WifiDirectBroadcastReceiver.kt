package com.polarinsdustries.chatwifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.channels.BroadcastChannel

class WifiDirectBroadcastReceiver(private var manager: WifiP2pManager, private var channel:WifiP2pManager.Channel, private var activity: MainActivity): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        var action:String = intent!!.action.toString()
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            //Revisa si el wifi esta activado y notifica a la actividad

        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //Llama WifiP2pManager.requestPeers() para obtener la lista los dispositivos
            if(manager!=null){
                manager.requestPeers(channel, activity.peerListListener)
            }

        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            //Responde ante una nueva conexión o desconexión
            if(manager!=null){
                var networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                if(networkInfo!!.isConnected){
                    manager.requestConnectionInfo(channel, activity.connectionInfoListener)
                }else{
                    activity.textView_Status.text = "No conectado"
                }
            }
        }
    }

}