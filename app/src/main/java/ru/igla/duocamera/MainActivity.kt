package ru.igla.duocamera

import android.os.Bundle
import android.util.Log
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
            Log.i(TAG, "replace<PermissionsFragment>")
            replace<PermissionsFragment>(R.id.fragment_container, null, null)
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    override fun onFinishFragment() {
        logI { "onFinishFragment" }
        supportFragmentManager.commit {
            Log.i(TAG, "replace<SelectorFragment>")
            replace<SelectorFragment>(R.id.fragment_container, null, null)
        }
    }
}