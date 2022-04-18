package com.rabbito.internetdictionary

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_spalsh_screen.*

class SpalshScreen : AppCompatActivity() {

    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_spalsh_screen)

        //Animation loading the animations
        val logo_animation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        val text_animation = AnimationUtils.loadAnimation(this, R.anim.text_animation)

        //Set the animation
        splash_logo.animation = logo_animation
        splash_text.animation = text_animation

        handler = Handler()
        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000) //delaying 3.0 seconds to open mainactivity
    }
}