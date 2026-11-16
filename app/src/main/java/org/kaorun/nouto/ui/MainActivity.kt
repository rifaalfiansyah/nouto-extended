package org.kaorun.nouto.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.utils.BlackThemeHelper
import org.kaorun.nouto.ui.utils.ColorThemeHelper

class MainActivity : AppCompatActivity() {
    lateinit var roomBackup: RoomBackup

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorThemeHelper.apply(this)
        BlackThemeHelper.apply(this)
        roomBackup = RoomBackup(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
    }
}