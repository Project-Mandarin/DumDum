package io.nekohasekai.sagernet.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.LayoutAboutBinding
import io.nekohasekai.sagernet.ktx.*
import io.nekohasekai.sagernet.plugin.PluginManager.loadString
import io.nekohasekai.sagernet.utils.PackageCache
import io.nekohasekai.sagernet.widget.ListListener
import libcore.Libcore
import moe.matsuri.nb4a.plugin.Plugins

class AboutFragment : ToolbarFragment(R.layout.layout_about) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view, ListListener)
        toolbar.setTitle(R.string.menu_about)

        parentFragmentManager.beginTransaction()
            .replace(R.id.about_fragment_holder, AboutContent())
            .commitAllowingStateLoss()
    }

    class AboutContent : MaterialAboutFragment() {

        override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {

            var versionName = "${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR} ${BuildConfig.GIT_COMMIT} ${BuildConfig.BUILD_TIME})"

            if (BuildConfig.DEBUG) {
                versionName += " DEBUG"
            }

            return MaterialAboutList.Builder()
                .addCard(
                    MaterialAboutCard.Builder()
                    .outline(false)
                    .addItem(
                        MaterialAboutActionItem.Builder()
                        .icon(R.drawable.ic_baseline_update_24)
                        .text(R.string.app_version)
                        .subText(versionName + System.lineSeparator() + Libcore.versionBox().lines().first())
                        .setOnClickAction {
                            requireContext().launchCustomTab(
                                "https://github.com/Project-Mandarin/DumDum/releases"
                            )
                        }
                        .build())
                    .addItem(
                        MaterialAboutActionItem.Builder()
                        .icon(R.drawable.ic_action_description)
                        .text(R.string.github)
                        .setOnClickAction {
                            requireContext().launchCustomTab(
                                "https://github.com/Project-Mandarin/DumDum"
                            )
                        }
                        .build())
                    .addItem(
                        MaterialAboutActionItem.Builder()
                            .icon(R.drawable.ic_baseline_gift_24)
                            .text(R.string.support_us)
                            .subText(R.string.support_us_summary)
                            .setOnClickAction {
                                requireContext().launchCustomTab(
                                    "https://github.com/Project-Mandarin/DumDum/wiki/Support-Us"
                                )
                            }
                            .build())
                    .apply {
                        PackageCache.awaitLoadSync()
                        for ((_, pkg) in PackageCache.installedPluginPackages) {
                            try {
                                val pluginId =
                                    pkg.providers?.get(0)?.loadString(Plugins.METADATA_KEY_ID)
                                if (pluginId.isNullOrBlank()) continue
                                addItem(
                                    MaterialAboutActionItem.Builder()
                                    .icon(R.drawable.ic_baseline_nfc_24)
                                    .text(
                                        getString(
                                            R.string.version_x,
                                            pluginId
                                        ) + " (${Plugins.displayExeProvider(pkg.packageName)})"
                                    )
                                    .subText("v" + pkg.versionName)
                                    .setOnClickAction {
                                        startActivity(Intent().apply {
                                            action =
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            data = Uri.fromParts(
                                                "package", pkg.packageName, null
                                            )
                                        })
                                    }
                                    .build())
                            } catch (e: Exception) {
                                Logs.w(e)
                            }
                        }
                    }
                    .build())
                .build()

        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            view.findViewById<RecyclerView>(R.id.mal_recyclerview).apply {
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            }
        }

    }

}

