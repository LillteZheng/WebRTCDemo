package com.zhengsr.webrtcdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.zhengsr.webrtcdemo.activity.CameraActivity
import com.zhengsr.webrtcdemo.activity.PeerLocalActivity
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class MainActivity : AppCompatActivity() {
    private  val TAG = "MainActivity"
    private lateinit var socket: Socket
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun camera(view: View) {
        startActivity(Intent(this, CameraActivity::class.java))
    }

    fun peercamera(view: View) {
        startActivity(Intent(this, PeerLocalActivity::class.java))
    }




}