package com.example.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
//import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ✅ Log to confirm splash is loading
        Log.d("SplashActivity", "onCreate: Splash started")

        // ✅ Load your splash layout
        setContentView(R.layout.activity_splash)

        // ✅ Navigate to MainActivity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("SplashActivity", "Navigating to MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}