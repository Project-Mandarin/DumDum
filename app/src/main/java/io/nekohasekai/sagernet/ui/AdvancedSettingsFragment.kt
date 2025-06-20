package io.nekohasekai.sagernet.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.view.View
import android.widget.EditText
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.*
import io.nekohasekai.sagernet.widget.ListListener
import moe.matsuri.nb4a.ui.*

class AdvancedSettingsFragment : PreferenceFragmentCompat() {

    private val reloadListener = Preference.OnPreferenceChangeListener { _, _ ->
        needReload()
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DataStore.configurationStore
        addPreferencesFromResource(R.xml.advanced_settings_preferences)

        val mtu = findPreference<MTUPreference>(Key.MTU)!!
        val tunImplementation = findPreference<SimpleMenuPreference>(Key.TUN_IMPLEMENTATION)!!
        val serviceMode = findPreference<SimpleMenuPreference>(Key.SERVICE_MODE)!!
        val logLevel = findPreference<LongClickListPreference>(Key.LOG_LEVEL)!!
        val trafficSniffing = findPreference<Preference>(Key.TRAFFIC_SNIFFING)!!
        val bypassLan = findPreference<SwitchPreference>(Key.BYPASS_LAN)!!
        val acquireWakeLock = findPreference<SwitchPreference>(Key.ACQUIRE_WAKE_LOCK)!!
        val batteryOptimizations = findPreference<Preference>(Key.BATTERY_OPTIMIZATIONS)!!

        mtu.onPreferenceChangeListener = reloadListener
        trafficSniffing.onPreferenceChangeListener = reloadListener
        bypassLan.onPreferenceChangeListener = reloadListener
        acquireWakeLock.onPreferenceChangeListener = reloadListener
        tunImplementation.onPreferenceChangeListener = reloadListener

        serviceMode.setOnPreferenceChangeListener { _, _ ->
            if (DataStore.serviceState.started) SagerNet.stopService()
            true
        }

        logLevel.dialogLayoutResource = R.layout.layout_loglevel_help
        logLevel.setOnPreferenceChangeListener { _, _ ->
            needRestart()
            true
        }
        logLevel.setOnLongClickListener {
            if (context == null) return@setOnLongClickListener true

            val view = EditText(context).apply {
                inputType = EditorInfo.TYPE_CLASS_NUMBER
                var size = DataStore.logBufSize
                setText(size.toString())
            }

            MaterialAlertDialogBuilder(requireContext()).setTitle("Log buffer size (kb)")
                .setView(view)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    DataStore.logBufSize = view.text.toString().toInt()
                    if (DataStore.logBufSize <= 0) DataStore.logBufSize = 50
                    needRestart()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(app.packageName)) {
                batteryOptimizations.setOnPreferenceClickListener {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            "package:${app.packageName}".toUri(),
                        )
                    )
                    true
                }
            } else {
                batteryOptimizations.isEnabled = false
                batteryOptimizations.setSummary(R.string.ignore_battery_optimizations_disabled)
            }
        } else {
            batteryOptimizations.isEnabled = false
        }

        val showLogcat = findPreference<Preference>("showLogcat")
        showLogcat?.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.settings_container, LogcatFragment())
                .addToBackStack(null)
                .commit()
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(listView, ListListener)
    }
}
