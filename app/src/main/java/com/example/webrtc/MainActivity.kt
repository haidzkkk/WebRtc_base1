package com.example.webrtc

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.webrtc.data.api.SocketManager
import com.example.webrtc.data.api.SocketManager.Companion.DONE
import com.example.webrtc.data.model.Message
import com.example.webrtc.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {

    lateinit var socketManager: SocketManager
    lateinit var binding: ActivityMainBinding
    var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        socketManager = SocketManager.getInstance()
        socketManager.connect()
        checkPer()

    }

    private fun lisstenClickUI() {
        binding.enterBtn.setOnClickListener {
            userName = binding.username.text.toString().trim()

            if (!userName.isNullOrEmpty()) {
                var message = Message(
                    type = SocketManager.STORE_USER,
                    userName,
                    null,
                    null
                )
                socketManager.sendMessage(message)

                socketManager.lisstenSocket(userName!!) {
                    if (it != null && it.type == DONE) {
                        socketManager.disconnect()
                        startActivity(Intent(this, CallActivity::class.java).putExtra("userName", userName))
                        finish()
                    } else {
                        Toast.makeText(this, "Đã tồn tại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkPer() {
        PermissionX.init(this)
            .permissions(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
            )
            .request { allGranded, _, _ ->
                if (allGranded) {
                    lisstenClickUI()
                } else {
                    Toast.makeText(this, "You should accept audio and camera", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

}