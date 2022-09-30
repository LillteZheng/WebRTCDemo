package com.zhengsr.webrtcdemo.activity

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.zhengsr.webrtcdemo.R
import kotlinx.android.synthetic.main.activity_camera.*
import org.webrtc.*

private const val TAG = "CameraActivity"
class CameraActivity : AppCompatActivity() {
    val VIDEO_TRACK_ID = "ARDAMSv0"
    val AUDIO_TRACK_ID = "ARDAMSa0"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // step 1 创建PeerConnectionFactory,进行全局初始化和资源加载
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        //创建 peerConnectionFactory 对象
        val peerConnectionFactory =
            PeerConnectionFactory.builder()
                .createPeerConnectionFactory()

        // create AudioSource and AudioTrack
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource)



        viewRender.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                //创建并启动VideoCapturer
                // 用PeerConnectionFactory创建VideoSource
                // 用PeerConnectionFactory和VideoSource创建VideoTrack
                createCameraCapture()?.let { camera->
                    val videoSource = peerConnectionFactory.createVideoSource(camera.isScreencast)

                    val eglBaseContext = EglBase.create().eglBaseContext
                    //拿到 surface工具类，用来表示camera 初始化的线程
                    val surfaceTextureHelper = SurfaceTextureHelper.create("caputerTHread", eglBaseContext)
                    //用来表示当前初始化 camera 的线程，和 application context，当调用 startCapture 才会回调。
                    camera.initialize(surfaceTextureHelper, application, videoSource.capturerObserver)
                    //开始采集
                    camera.startCapture(
                        viewRender.width,
                        viewRender.height,
                        30
                    )
                    //是否镜像
                    //viewRender.setMirror(true)
                    // 初始化 SurfaceViewRender ，这个方法非常重要，不初始化黑屏
                    viewRender.init(eglBaseContext,null)

                    //添加视频轨道
                    val videoTrack =
                        peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource)

                    // 添加渲染接收端器到轨道中，画面开始呈现
                    videoTrack.addSink(viewRender)
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