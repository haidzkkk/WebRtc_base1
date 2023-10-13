package com.example.webrtc.data.api

import android.util.Log
import com.example.webrtc.MainActivity
import com.example.webrtc.data.model.Message
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SocketManager() {

     private var mSocket: Socket? = null

    companion object{
        private const val BASE_URL: String = "http://192.168.1.10:3001"

        const val STORE_USER: String = "store_user"
        const val START_CALL: String = "start_call"
        const val CREATE_OFFER: String = "create_offer"
        const val CREATE_ANSWER: String = "create_answer"
        const val ICE_CANDIDATE: String = "ice_candidate"

        const val DONE: String = "done"
        const val CALL_RESPONSE: String = "call_response"
        const val OFFER_RECEIVED: String = "offer_received"
        const val ANSWER_RECEIVED: String = "answer_received"

        private var socketManager: SocketManager? = null

        fun getInstance(): SocketManager{
            if (socketManager == null){
                socketManager = SocketManager()
            }

            socketManager!!.mSocket = IO.socket(BASE_URL)
            return socketManager!!
        }
    }

    public fun connect(){
        mSocket?.connect()
    }

    public fun disconnect(){
        mSocket?.disconnect()
    }

    public fun sendMessage(data: Message){
        Log.e("TAG", "sendMessage: ${mSocket?.hashCode()}", )
        mSocket?.emit("message", Gson().toJson(data))
    }

    public fun lisstenSocket(userName: String, callBack: (message: Message?) -> Unit){
        mSocket!!.on("client-$userName"){
            CoroutineScope(Dispatchers.Main).launch {
                callBack(Gson().fromJson(it[0].toString(), Message::class.java))
            }
        }
    }

}