package io.nekohasekai.sagernet.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.LayoutLogcatBinding
import io.nekohasekai.sagernet.ktx.*
import io.nekohasekai.sagernet.widget.ListListener
import libcore.Libcore
import moe.matsuri.nb4a.utils.SendLog

class LogcatFragment : ToolbarFragment(R.layout.layout_logcat),
    Toolbar.OnMenuItemClickListener {

    lateinit var binding: LayoutLogcatBinding

    @SuppressLint("RestrictedApi", "WrongConstant")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (activity is AdvancedSettingsActivity) {
            val advancedActivity = activity as AdvancedSettingsActivity
            val includedToolbar = view.findViewById<Toolbar>(R.id.toolbar)
            includedToolbar.isVisible = false

            val activityToolbar = advancedActivity.findViewById<Toolbar>(R.id.toolbar)
            this.toolbar = activityToolbar

            activityToolbar.setTitle(R.string.menu_log)
            activityToolbar.setNavigationIcon(R.drawable.ic_navigation_close)
            activityToolbar.setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            activityToolbar.menu.clear()
            activityToolbar.inflateMenu(R.menu.logcat_menu)
            activityToolbar.setOnMenuItemClickListener(this)
        } else {
            super.onViewCreated(view, savedInstanceState)
            toolbar.setTitle(R.string.menu_log)
            toolbar.inflateMenu(R.menu.logcat_menu)
            toolbar.setOnMenuItemClickListener(this)
        }

        binding = LayoutLogcatBinding.bind(view)

        if (Build.VERSION.SDK_INT >= 23) {
            binding.textview.breakStrategy = 0 // simple
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root, ListListener)

        reloadSession()
        // TODO new logcat
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity is AdvancedSettingsActivity) {
            val advancedActivity = activity as AdvancedSettingsActivity
            val toolbar = advancedActivity.findViewById<Toolbar>(R.id.toolbar)
            toolbar.setTitle(R.string.advanced)
            toolbar.setNavigationIcon(R.drawable.ic_navigation_close)
            toolbar.setNavigationOnClickListener {
                advancedActivity.finish()
            }
            toolbar.menu.clear()
        }
    }

    private fun getColorForLine(line: String): ForegroundColorSpan {
        var color = ForegroundColorSpan(Color.GRAY)
        when {
            line.contains("INFO[") || line.contains(" [Info]") -> {
                color = ForegroundColorSpan((0xFF86C166).toInt())
            }

            line.contains("ERROR[") || line.contains(" [Error]") -> {
                color = ForegroundColorSpan(Color.RED)
            }

            line.contains("WARN[") || line.contains(" [Warning]") -> {
                color = ForegroundColorSpan(Color.RED)
            }
        }
        return color
    }

    private fun reloadSession() {
        val span = SpannableString(
            String(SendLog.getNekoLog(50 * 1024))
        )
        var offset = 0
        for (line in span.lines()) {
            val color = getColorForLine(line)
            span.setSpan(
                color, offset, offset + line.length, SPAN_EXCLUSIVE_EXCLUSIVE
            )
            offset += line.length + 1
        }
        binding.textview.text = span

        binding.scroolview.post {
            binding.scroolview.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear_logcat -> {
                runOnDefaultDispatcher {
                    try {
                        Libcore.nekoLogClear()
                        Runtime.getRuntime().exec("/system/bin/logcat -c")
                    } catch (e: Exception) {
                        onMainDispatcher {
                            snackbar(e.readableMessage).show()
                        }
                        return@runOnDefaultDispatcher
                    }
                    onMainDispatcher {
                        binding.textview.text = ""
                    }
                }

            }

            R.id.action_send_logcat -> {
                val context = requireContext()
                runOnDefaultDispatcher {
                    SendLog.sendLog(context, "DumDum")
                }
            }

            R.id.action_refresh -> {
                reloadSession()
            }
        }
        return true
    }

}
