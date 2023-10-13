package com.example.webrtc.ui

import org.webrtc.*

open class MyPeerConnectionObserver: PeerConnection.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
    // Sự kiện này xảy ra khi trạng thái của kết nối ICE thay đổi.
    // Điều này cho phép ứng dụng của bạn theo dõi trạng thái của kết nối ICE,
    // ví dụ: kết nối, đang kết nối, hoặc ngừng kết nối.
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
    }

    override fun onIceCandidate(p0: IceCandidate?) {

    //  xảy ra khi có một ICE candidate sẵn sàng để truyền đi.
    //  ICE candidate chứa thông tin về địa chỉ IP và cổng của một thiết bị để thiết lập kết nối.
    //  Ứng dụng của bạn có thể thu thập và truyền các ứng cử viên này giữa các thiết bị để thiết lập kết nối.
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onAddStream(p0: MediaStream?) {

    //  xảy ra khi một media stream mới được thêm vào kết nối.
    //  Điều này có thể xảy ra khi bạn bắt đầu nhận dữ liệu audio hoặc video từ thiết bị khác trong kết nối.
    }

    override fun onRemoveStream(p0: MediaStream?) {
    // Sự kiện này xảy ra khi một media stream bị loại bỏ khỏi kết nối.
    // Điều này có thể xảy ra khi media stream ngừng truyền hoặc khi kết nối bị đóng.
    }

    override fun onDataChannel(p0: DataChannel?) {
    // Sự kiện này xảy ra khi một data channel mới được tạo hoặc được mở trên kết nối.
    // Kênh dữ liệu cho phép truyền dữ liệu tùy chỉnh giữa các thiết bị trong kết nối
    }

    override fun onRenegotiationNeeded() {
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
    }
}