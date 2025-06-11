package io.nekohasekai.sagernet.ui

import android.os.Bundle
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.LayoutAdvancedSettingsActivityBinding
import com.google.android.material.snackbar.Snackbar

class AdvancedSettingsActivity : ThemedActivity() {
    lateinit var binding: LayoutAdvancedSettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutAdvancedSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.advanced)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_navigation_close)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, AdvancedSettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun snackbarInternal(text: CharSequence): Snackbar {
        return Snackbar.make(binding.coordinator, text, Snackbar.LENGTH_LONG)
    }

}
