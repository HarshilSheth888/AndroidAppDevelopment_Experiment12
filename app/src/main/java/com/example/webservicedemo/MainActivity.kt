package com.example.webservicedemo

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.webservicedemo.data.model.HttpEngineType
import com.example.webservicedemo.databinding.ActivityMainBinding
import com.example.webservicedemo.databinding.DialogHttpGuideBinding
import com.example.webservicedemo.ui.fragment.HttpPlaygroundFragment
import com.example.webservicedemo.ui.fragment.RestFeedFragment
import com.example.webservicedemo.ui.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootCoordinator) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupViewPagerAndTabs()
        setupTopBarControls()
        setupNetworkMonitor()
        observeViewModel()
    }

    private fun setupViewPagerAndTabs() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) {
                    RestFeedFragment.newInstance()
                } else {
                    HttpPlaygroundFragment.newInstance()
                }
            }
        }
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_rest_feed)
                1 -> getString(R.string.tab_http_inspector)
                else -> ""
            }
        }.attach()
    }

    private fun setupTopBarControls() {
        binding.chipEngineToggle.setOnClickListener {
            viewModel.toggleEngine()
        }

        binding.btnGuide.setOnClickListener {
            showGuideDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.engineType.collect { engine ->
                        binding.tvEngineSubtitle.text = getString(R.string.engine_label, engine.displayName)
                        binding.chipEngineToggle.text = if (engine == HttpEngineType.OKHTTP) "OkHttp" else "HttpURL"
                    }
                }

                launch {
                    viewModel.toastEvent.collect { message ->
                        Snackbar.make(binding.rootCoordinator, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupNetworkMonitor() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        updateNetworkStatus(isNetworkAvailable(connectivityManager))

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { updateNetworkStatus(true) }
            }

            override fun onLost(network: Network) {
                runOnUiThread { updateNetworkStatus(false) }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {
            // Fallback gracefully if permissions or system restriction
        }
    }

    private fun isNetworkAvailable(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateNetworkStatus(isOnline: Boolean) {
        if (isOnline) {
            binding.chipNetworkStatus.text = getString(R.string.status_online)
            binding.chipNetworkStatus.setTextColor(ContextCompat.getColor(this, R.color.status_2xx))
            binding.chipNetworkStatus.chipIconTint = ContextCompat.getColorStateList(this, R.color.status_2xx)
            binding.chipNetworkStatus.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.status_2xx_bg)
        } else {
            binding.chipNetworkStatus.text = getString(R.string.status_offline)
            binding.chipNetworkStatus.setTextColor(ContextCompat.getColor(this, R.color.status_5xx))
            binding.chipNetworkStatus.chipIconTint = ContextCompat.getColorStateList(this, R.color.status_5xx)
            binding.chipNetworkStatus.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.status_5xx_bg)
        }
    }

    private fun showGuideDialog() {
        val dialog = BottomSheetDialog(this)
        val guideBinding = DialogHttpGuideBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(guideBinding.root)

        guideBinding.btnCloseGuide.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            try {
                val cm = getSystemService(ConnectivityManager::class.java)
                cm?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
    }
}
