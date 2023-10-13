package com.example.webrtc

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import com.codewithkael.webrtcprojectforrecord.utils.RTCAudioManager
import com.example.webrtc.data.api.SocketManager
import com.example.webrtc.data.api.SocketManager.Companion.ANSWER_RECEIVED
import com.example.webrtc.data.api.SocketManager.Companion.CALL_RESPONSE
import com.example.webrtc.data.api.SocketManager.Companion.ICE_CANDIDATE
import com.example.webrtc.data.api.SocketManager.Companion.OFFER_RECEIVED
import com.example.webrtc.data.api.WebRTCClient
import com.example.webrtc.data.model.IceCandidateModel
import com.example.webrtc.data.model.Message
import com.example.webrtc.databinding.ActivityCallBinding
import com.example.webrtc.ui.MyPeerConnectionObserver
import com.google.gson.Gson
import org.webrtc.*
import org.webrtc.voiceengine.WebRtcAudioManager

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private lateinit var userName: String
    private var targetUserName: String = ""
    private lateinit var socketManager: SocketManager
    private var webRTCClient: WebRTCClient? = null
    private val gson = Gson()
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this)}
    private var isSpeakerMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        lissten()
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.disconnect()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun lissten() {
        socketManager.lisstenSocket(userName){
            Log.e("lisstenSocket", "$it", )
            when(it?.type){
                CALL_RESPONSE ->{
                    if(it.data == "ok"){
                        binding.apply {
                            callLayout.isVisible = true
                            whoToCallLayout.isVisible = false

                            webRTCClient?.initializeSurfaceView(localView)
                            webRTCClient?.initializeSurfaceView(remoteView)
                            webRTCClient?.startLocalVideo(localView)
                            webRTCClient?.call(targetUserNameEt.text.toString())
                        }
                    }else{
                        Toast.makeText(this, "${it.data}", Toast.LENGTH_SHORT).show()
                    }
                }

                OFFER_RECEIVED ->{
                    binding.apply {
                        incomingCallLayout.isVisible = true
                        incomingNameTV.text = "${it.name.toString()} is calling you"

                        acceptButton.setOnClickListener{_ ->
                            incomingCallLayout.isVisible = false
                            callLayout.isVisible = true
                            whoToCallLayout.isVisible = false
                            remoteViewLoading.isVisible = false

                            webRTCClient?.initializeSurfaceView(localView)
                            webRTCClient?.initializeSurfaceView(remoteView)
                            webRTCClient?.startLocalVideo(localView)
                            targetUserName = it.name ?: ""

                            var session = SessionDescription(SessionDescription.Type.OFFER, it.data.toString()) //người trả lời nhận: sdp cua nguoi goi,
                            webRTCClient?.onRemoteSessionReceived(session)
                            webRTCClient?.answer(it.name!!)
                        }
                        rejectButton.setOnClickListener{
                            incomingCallLayout.isVisible = false
                        }
                    }
                }

                ANSWER_RECEIVED ->{
                    val session = SessionDescription(SessionDescription.Type.ANSWER, it.data.toString()) //người gọi nhận: sdp cua nguoi trả lời
                    webRTCClient?.onRemoteSessionReceived(session)
                    binding.remoteViewLoading.isVisible = false
                }

                ICE_CANDIDATE -> {
                    Log.e("TAG", "ICE_CANDIDATE: ${it.data}", )
                    var receivingIceCandidate = gson.fromJson(it.data.toString(), IceCandidateModel::class.java)
                    webRTCClient?.addIceCandidate(IceCandidate(
                        receivingIceCandidate.sdpMid,
                        Math.toIntExact(receivingIceCandidate.sdpMLineIndex.toLong()),
                        receivingIceCandidate.sdpCandidate
                    ))
                }
            }
        }

        binding.callBtn.setOnClickListener{
            targetUserName = binding.targetUserNameEt.text.toString().trim()
            if (userName != "")
            socketManager.sendMessage(
                Message(SocketManager.START_CALL,userName, targetUserName, null)
            )
        }

        binding.switchCameraButton.setOnClickListener{
            webRTCClient?.swichVideoCapture()
        }

        binding.micButton.setOnClickListener{
            if(isMute){
                isMute = false
                binding.micButton.setImageResource(R.drawable.baseline_mic_off_24)
            }else{
                isMute = true
                binding.micButton.setImageResource(R.drawable.ic_baseline_mic_24)
            }
            webRTCClient?.toggleAudio(isMute)
        }

        binding.videoButton.setOnClickListener{
            if (isCameraPause){
                isCameraPause = false
                binding.videoButton.setImageResource(R.drawable.baseline_videocam_off_24)
            }else{
                isCameraPause = true
                binding.videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
            }
            webRTCClient?.toggleVideo(isCameraPause)
        }

        binding.audioOutputButton.setOnClickListener{
            if (isSpeakerMode){
                isSpeakerMode = false
                binding.audioOutputButton.setImageResource(R.drawable.baseline_hearing_24)
                rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
            }else{
                isSpeakerMode = true
                binding.audioOutputButton.setImageResource(R.drawable.baseline_volume_up_24)
                rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
            }
        }

        binding.endCallButton.setOnClickListener{
            binding.apply {
                callLayout.isVisible = false
                whoToCallLayout.isVisible = true
                incomingCallLayout.isVisible = false
                webRTCClient?.endCall()
            }
        }
    }

    private fun init(){
        userName = intent.getStringExtra("userName") ?: ""
        binding.tvUsername.text = userName

        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)

        socketManager = SocketManager.getInstance()
        socketManager.connect()
        webRTCClient = WebRTCClient(application, userName, socketManager, object: MyPeerConnectionObserver(){
            override fun onIceCandidate(p0: IceCandidate?) {    // khi có một ICE candidate sẵn sàng để truyền đi
                super.onIceCandidate(p0)
                webRTCClient?.addIceCandidate(p0)
                val candidate = hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )
                socketManager.sendMessage(
                    Message(ICE_CANDIDATE, userName, targetUserName, candidate)
                )
            }

            override fun onAddStream(p0: MediaStream?) {      // khi một media stream mới được thêm vào kết nối
                super.onAddStream(p0)
                p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
                Log.d("TAG", "onAddStream: $p0")
            }
        })
    }


}