package com.zhengsr.webrtcdemo.activity

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.zhengsr.webrtcdemo.PeerObserver
import com.zhengsr.webrtcdemo.R
import com.zhengsr.webrtcdemo.SdpObserver
import kotlinx.android.synthetic.main.activity_peer_local.*
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer


/**
 * @author by zhengshaorui 2022/9/30
 * describe：
 */
private const val TAG = "PeerLocalActivity"
class PeerLocalActivity : AppCompatActivity() {
    val VIDEO_TRACK_ID = "ARDAMSv0"
    val AUDIO_TRACK_ID = "ARDAMSa0"
    private lateinit var peerConnectionFactory:PeerConnectionFactory
    private lateinit var localMediaStream:MediaStream
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peer_local)

        // step 1 创建PeerConnectionFactory,进行全局初始化和资源加载
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val eglBaseContext = EglBase.create().eglBaseContext
        //创建编解码对象
        val videoEncode = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val videoDecode = DefaultVideoDecoderFactory(eglBaseContext)
        val options = PeerConnectionFactory.Options()
        //创建 peerConnectionFactory 对象
        peerConnectionFactory =
            PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoDecoderFactory(videoDecode)
                .setVideoEncoderFactory(videoEncode)
                .createPeerConnectionFactory()

        // create AudioSource and AudioTrack
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource)

        configLocalRender(eglBaseContext)

        configRemoteRender(eglBaseContext)
    }



    /**
     * 配置本地相机预览
     */
    fun configLocalRender(eglBaseContext:EglBase.Context){
        localRender.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                createCameraCapture()?.let { camera->
                    val videoSource = peerConnectionFactory.createVideoSource(camera.isScreencast)
                    Log.d(TAG, "surfaceCreated() called with: camera = $camera")
                    //拿到 surface工具类，用来表示camera 初始化的线程
                    val surfaceTextureHelper = SurfaceTextureHelper.create("caputerTHread", eglBaseContext)
                    //用来表示当前初始化 camera 的线程，和 application context，当调用 startCapture 才会回调。
                    camera.initialize(surfaceTextureHelper, application, videoSource.capturerObserver)
                    //开始采集
                    camera.startCapture(
                        localRender.width,
                        localRender.height,
                        30
                    )
                    // 初始化 SurfaceViewRender ，这个方法非常重要，不初始化黑屏
                    localRender.init(eglBaseContext,null)

                    //添加视频轨道
                    val videoTrack =
                        peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource)

                    // 添加渲染接收端器到轨道中，画面开始呈现
                    videoTrack.addSink(localRender)

                    // 创建 mediastream
                    localMediaStream = peerConnectionFactory.createLocalMediaStream("MediaStream")
                    // 将视频轨添加到 mediastram 中，等待连接时传输
                    localMediaStream.addTrack(videoTrack)
                }

            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })



    }

    /**
     * 配置接收端的渲染surface
     */
    fun configRemoteRender(eglBaseContext:EglBase.Context){
        remoteRender.holder.addCallback(object:SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                //等待接收数据，这里只要初始化即可
                remoteRender.init(eglBaseContext,null)
                remoteRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                remoteRender.setEnableHardwareScaler(false)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })
    }

    fun trans(view: View) {
        createPeerConnection()
    }
    private var localPeerConnection:PeerConnection? = null
    private var remotePeerConnection:PeerConnection? = null
    private fun createPeerConnection(){
        val iceServers: List<IceServer> = ArrayList()
        //内部会转成 RTCConfiguration
        localPeerConnection = peerConnectionFactory.createPeerConnection(iceServers,object:PeerObserver(){
            override fun onAddStream(strem: MediaStream?) {
                super.onAddStream(strem)
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                //透传给另外一端
                remotePeerConnection?.addIceCandidate(p0)

            }
        })
        remotePeerConnection = peerConnectionFactory.createPeerConnection(iceServers,object :PeerObserver(){
            override fun onAddStream(strem: MediaStream?) {
                super.onAddStream(strem)
                //拿到视频流，添加 sink ，SurfaceViewRender 会直接渲染
                strem?.let {
                    runOnUiThread {
                        it.videoTracks[0].addSink(remoteRender)
                    }
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                //透传给另外一端
                localPeerConnection?.addIceCandidate(p0)

            }
        })

        Log.d(TAG, "zsr createPeerConnection: "+localPeerConnection.hashCode()+" "+remotePeerConnection.hashCode())
        localPeerConnection?.addStream(localMediaStream)
        //客户端创建 offer
        localPeerConnection?.createOffer(object:SdpObserver("创建local offer"){
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                Log.d(TAG, "创建 local offer success: ")
                /**
                 * 此时调用setLocalDescription()方法将该Offer保存到本地Local域，
                 * 然后将Offer发送给对方
                 */
                localPeerConnection?.setLocalDescription(SdpObserver("local 设置本地 sdp"),p0)
                //2. 同时，接收端 setRemoteDescription 把 offer 保存到远端域
                remotePeerConnection?.setRemoteDescription(SdpObserver("把 sdp 给到 remote"),p0)

                //3. 此时remote 已经接收端了 offer ,所以创建 answer
                remotePeerConnection?.createAnswer(object:SdpObserver("创建 remote answer"){
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        super.onCreateSuccess(p0)
                        Log.d(TAG, " 创建 remote answer success: ")
                        /**
                         * 4. 此时调用setLocalDescription()方法将该 answer 保存到本地 remote 域，
                         * 然后将 answer 发送给对方
                         */
                        remotePeerConnection?.setLocalDescription(SdpObserver("remote 设置本地 sdp"),p0)
                        //5. local 把 answer 保存到它的 remote 端，此时 sdp 交换结束
                        localPeerConnection?.setRemoteDescription(SdpObserver("把 sdp 给到 local"),p0)
                    }

                }, MediaConstraints())
            }
        }, MediaConstraints())


    }

    /**
     * 或者 CameraCapture
     */
    private fun createCameraCapture(): VideoCapturer? {
        val enumerator = Camera1Enumerator(false)
        val deviceNames = enumerator.deviceNames
        //使用后置摄像头
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                }
            }
        }
        return null

    }


}