package com.example.scanconnect

import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SetupInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = """Online features setup

This starter project includes offline scanning, image processing, saving, PDF export and Android sharing.

To enable real online chat:
1. Create a Firebase project.
2. Add an Android app with package com.example.scanconnect.
3. Download google-services.json into the app/ folder.
4. Enable Firebase Authentication providers you want (Google and Phone; Facebook needs Meta app credentials).
5. Add Cloud Firestore and Firebase Storage rules.
6. For audio/video calls, configure WebRTC signaling, STUN/TURN servers, call permissions, and push notifications.

Important: online messages and calls are NOT implemented in this starter build. They need backend configuration, account credentials, security rules and testing. Do not put private keys in a public GitHub repository."""
            textSize = 16f
            setPadding(28, 28, 28, 28)
            gravity = Gravity.CENTER_VERTICAL
        })
    }
}
