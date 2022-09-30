package com.zhengsr.webrtcdemo

import android.util.Log
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * @author by zhengshaorui 2022/9/30
 * describe：
 */
open class SdpObserver(val msg:String) : SdpObserver {
    private  val TAG = "SdpObserver"
    init {
        Log.d(TAG, msg)
    }
    override fun onCreateSuccess(p0: SessionDescription?) {
       
    }

    override fun onSetSuccess() {
       
    }

    override fun onCreateFailure(p0: String?) {
       
    }

    override fun onSetFailure(p0: String?) {
       
    }
}