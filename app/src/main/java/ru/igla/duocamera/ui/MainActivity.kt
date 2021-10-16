package ru.igla.duocamera.ui

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import ru.igla.duocamera.R
import ru.igla.duocamera.ui.AboutDialog
import ru.igla.duocamera.utils.logI


class MainActivity :
    AppCompatActivity(),
    OnBackFragmentGoListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.apply {
            title = "Select preview size"
        }

        supportFragmentManager.commit {
            logI { "replace<PermissionsFragment>" }
            replace<PermissionsFragment>(R.id.fragment_container, null, null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                AboutDialog.show(this)
            }
            R.id.licenses -> {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.about_license_title))
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onFinishFragment() {
        supportFragmentManager.commit {
            logI { "replace<SelectorFragment>" }
            replace<SelectorFragment>(R.id.fragment_container, null, null)
        }
    }
}