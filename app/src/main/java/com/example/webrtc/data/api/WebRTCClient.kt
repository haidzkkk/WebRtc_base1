package com.example.webrtc.data.api

import android.app.Application
import com.example.webrtc.data.model.Message
import org.webrtc.*
import org.webrtc.voiceengine.WebRtcAudioManager

class WebRTCClient(
    private val application: Application,
    private val userName: String,
    private val socketManager: SocketManager,
    private val observer: PeerConnection.Observer
) {

    init {
        initPeerConnectionFactory(application)
    }

    private val eglContext = EglBase.create()                                                       // EGL quản lý đồ họa. được sử dụng để thiết lập môi trường đồ họa và là một phần quan trọng trong việc đảm bảo rằng bất kỳ đối tượng nào sử dụng OpenGL hoặc OpenGL ES hoạt động đúng cách trong ứng dụng
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(                                                                 // danh sách các máy chủ ICE để trợ giúp thiết lập và duy trì kết nối
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer(),
        PeerConnection.IceServer("stun:openrelay.metered.ca:80"),                               // máy chủ STUN dùng để giải quyết vấn đề liên quan đến địa chỉ IP và NAT. là nó sẽ lấy IP và cổng đích của một thiết bị khi bị tường lửa chặn
        PeerConnection.IceServer("turn:openrelay.metered.ca:80","openrelayproject","openrelayproject"), // máy chỉ TURN, sử dụng khi STUN không đủ để giải quyết vấn đề NAT, NAT là kỹ thuật sử dụng trong mạng máy tính để ánh xạ địa chỉ IP và cổng của các thiết bị trong local network sang địa chỉ IP và cổng của router hoặc gateway khi dữ liệu đi ra mạng Internet hoặc mạng công cộng
        PeerConnection.IceServer("turn:openrelay.metered.ca:443","openrelayproject","openrelayproject"),
        PeerConnection.IceServer("turn:openrelay.metered.ca:443?transport=tcp","openrelayproject","openrelayproject"),
        )
    private val peerConnection by lazy { createPeerConnection(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) } // để tạo một nguồn video cho việc truyền dữ liệu video trong kết nối WebRTC, false là tắt tính năng chia sẻ màn hình
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) } // để tạo một nguồn âm thanh cho việc truyền dữ liệu video trong kết nối WebRTC

    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null                                                 // VideoTrack thường được sử dụng để truyền video trong ứng dụng WebRTC
    private var localAudioTrack: AudioTrack? = null                                                 // AudioTrack cũng như vậy

    private fun initPeerConnectionFactory(application: Application) {
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application) // InitializationOptions được sử dụng để cấu hình việc khởi tạo của PeerConnectionFactory
            .setEnableInternalTracer(true)                                                          // cho phép bật bộ theo dõi nội bộ, giúp gỡ lỗi và giám sát các hoạt động của WebRTC
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")                                      // Thử nghiệm trường được sử dụng để thử nghiệm các tính năng hoặc cấu hình thử nghiệm trong WebRTC. Trong trường hợp này, nó bật H.264 high profile cho việc mã hóa video
            .createInitializationOptions()

        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(                                     // factory mã hóa video mặc định, mã hóa video trước khi truyền
                eglContext.eglBaseContext, true, true))                                             // đảm bảo rằng các thành phần xử lý video sử dụng cùng một ngữ cảnh EGL. Tính nhất quán, Tối ưu hóa hiệu suất, Tránh xung đột và lỗi
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))          // factory giải mã video khi nhận được
            .setOptions(PeerConnectionFactory.Options().apply {                                     // thể đặt các tùy chọn khác cho PeerConnectionFactory
                disableEncryption = true                                                            // tắt mã hóa
                disableNetworkMonitor = true                                                        // tắt giám sát mạng
            }).createPeerConnectionFactory()
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)                      // khởi tạo PeerConnection
    }

    // cấu hình và khởi tạo một đối tượng SurfaceViewRenderer, là một thành phần cho việc hiển thị video
    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.run {                                                                               // run giống apply nhưng nó trả kqua cuối cùng
            setEnableHardwareScaler(true)                                                           // Bật bộ điều chỉnh phần cứng, có thể cải thiện hiệu suất khi hiển thị video
            setMirror(true)                                                                         // bật chế độ gương giúp video hiển thị đúng hướng
            init(eglContext.eglBaseContext, null)                                                   // eglBaseContext giúp đảm bảo rằng SurfaceViewRenderer hiểu cách làm việc với đồ họa trong ứng dụng
        }
    }

    // video và âm thanh từ thiết bị của người dùng được thu và truyền đi trong kết nối WebRTC.
    // Video được hiển thị trên surface và âm thanh được truyền đi cùng với dữ liệu video
    fun startLocalVideo(surface: SurfaceViewRenderer) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglContext.eglBaseContext)     // Tạo một trợ lý cho Texture của SurfaceViewRenderer, sử dụng trong việc hiển thị video.
        videoCapturer = getVideoCapturer(application)                                               // Lấy một đối tượng videoCapturer để thu video từ thiết bị của người dùng
        videoCapturer?.initialize(                                                                  // Khởi tạo và cấu hình videoCapturer để thu video.
            surfaceTextureHelper,
            surface.context,
            localVideoSource.capturerObserver                                                       // để theo dõi và xử lý sự kiện liên quan đến việc thu video.
        )
        videoCapturer?.startCapture(320, 240, 30)                                                   // Bắt đầu quá trình thu video từ thiết bị với kích thước 320x240 pixel và fps 30
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_track", localVideoSource)// VideoTrack từ videoCapturer sẽ chứa video từ thiết bị của người dùng
        localVideoTrack?.addSink(surface)                                                           // Đặt SurfaceViewRenderer là sink cho VideoTrack. Điều này có nghĩa là video từ localVideoTrack sẽ được hiển thị trên SurfaceViewRenderer để người dùng xem
        localAudioTrack =                                                                           // Tạo một AudioTrack để thu âm thanh từ thiết bị
            peerConnectionFactory.createAudioTrack("local_track_audio", localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")         // Tạo một LocalMediaStream để chứa cả VideoTrack và AudioTrack
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)

        peerConnection?.addStream(localStream)                                                      // Thêm LocalMediaStream vào peerConnection. Điều này đảm bảo rằng video và âm thanh từ thiết bị của người dùng sẽ được truyền đi trong kết nối WebRTC
    }

    // Hàm này chọn và tạo một Capturer để chụp video từ thiết bị camera.
    // Nó sử dụng Camera2Enumerator để lựa chọn thiết bị camera trước và tạo một Capturer cho nó
     private fun getVideoCapturer(application: Application): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw
            IllegalStateException()
        }
    }

    fun call(target: String) {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createOffer(object : SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object: SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onSetSuccess() {

                        val offer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketManager.sendMessage(
                            Message(SocketManager.CREATE_OFFER, userName, target, offer)
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                }, desc)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }
        }, mediaConstraints)
    }

    fun onRemoteSessionReceived(session: SessionDescription) {
        peerConnection?.setRemoteDescription(object: SdpObserver{
            override fun onCreateSuccess(p0: SessionDescription?) {

            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        }, session)
    }

    fun answer(target: String) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createAnswer(object: SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object: SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }
                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketManager.sendMessage(Message(SocketManager.CREATE_ANSWER, userName, target, answer))
                    }

                    override fun onCreateFailure(p0: String?) {
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                }, desc)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }
        }, constraints)

    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)
    }

    fun swichVideoCapture(){
        videoCapturer?.switchCamera(null)
    }

    fun toggleAudio(isMute: Boolean) {
        localAudioTrack?.setEnabled(isMute)
    }

    fun toggleVideo(isCameraPause: Boolean) {
        localVideoTrack?.setEnabled(isCameraPause)
    }

    fun endCall() {
        peerConnection?.close()
    }
}