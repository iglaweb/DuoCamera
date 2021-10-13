package ru.igla.duocamera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import ru.igla.duocamera.utils.logI

class MainActivity :
    AppCompatActivity(),
    OnBackFragmentGoListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.apply {
            title = "DuoCamera"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        supportFragmentManager.commit {
            logI { "replace<PermissionsFragment>" }
            replace<PermissionsFragment>(R.id.fragment_container, null, null)
        }
    }

    override fun onFinishFragment() {
        supportFragmentManager.commit {
            logI { "replace<SelectorFragment>" }
            replace<SelectorFragment>(R.id.fragment_container, null, null)
        }
    }
}