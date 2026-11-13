package com.example.webservicedemo.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.webservicedemo.R
import com.example.webservicedemo.data.model.HttpMethod
import com.example.webservicedemo.data.model.HttpRequestConfig
import com.example.webservicedemo.data.model.HttpResponseResult
import com.example.webservicedemo.data.network.HttpServiceEngine
import com.example.webservicedemo.databinding.FragmentHttpPlaygroundBinding
import com.example.webservicedemo.ui.viewmodel.MainViewModel
import com.example.webservicedemo.ui.viewmodel.RequestUiState
import kotlinx.coroutines.launch

class HttpPlaygroundFragment : Fragment() {

    private var _binding: FragmentHttpPlaygroundBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private var lastResponseResult: HttpResponseResult? = null
    private var activeResponseTab: Int = TAB_FORMATTED

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHttpPlaygroundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setupMethodSelector()
        setupPresets()
        setupHeadersToggle()
        setupBodyButtons()
        setupSendButton()
        setupResponseTabs()
        setupCopyButton()
        observeViewModel()
    }

    private fun setupMethodSelector() {
        binding.chipGroupMethods.setOnCheckedStateChangeListener { _, checkedIds ->
            val method = getSelectedMethod()
            if (method.supportsBody) {
                binding.layoutRequestBody.visibility = View.VISIBLE
                if (binding.etRequestBody.text.isNullOrBlank()) {
                    insertSampleBody()
                }
                ensureContentTypeHeader()
            } else {
                binding.layoutRequestBody.visibility = View.GONE
            }
        }
    }

    private fun getSelectedMethod(): HttpMethod {
        return when (binding.chipGroupMethods.checkedChipId) {
            R.id.chipMethodPost -> HttpMethod.POST
            R.id.chipMethodPut -> HttpMethod.PUT
            R.id.chipMethodPatch -> HttpMethod.PATCH
            R.id.chipMethodDelete -> HttpMethod.DELETE
            R.id.chipMethodHead -> HttpMethod.HEAD
            else -> HttpMethod.GET
        }
    }

    private fun setMethodChip(method: HttpMethod) {
        val chipId = when (method) {
            HttpMethod.GET -> R.id.chipMethodGet
            HttpMethod.POST -> R.id.chipMethodPost
            HttpMethod.PUT -> R.id.chipMethodPut
            HttpMethod.PATCH -> R.id.chipMethodPatch
            HttpMethod.DELETE -> R.id.chipMethodDelete
            HttpMethod.HEAD -> R.id.chipMethodHead
        }
        binding.chipGroupMethods.check(chipId)
    }

    private fun setupPresets() {
        binding.chipPresetGetPost.setOnClickListener {
            setMethodChip(HttpMethod.GET)
            binding.etUrl.setText("https://jsonplaceholder.typicode.com/posts/1")
            binding.etHeaders.setText("Accept: application/json\nUser-Agent: Android-WebServiceDemo/1.0")
        }

        binding.chipPresetPost.setOnClickListener {
            setMethodChip(HttpMethod.POST)
            binding.etUrl.setText("https://jsonplaceholder.typicode.com/posts")
            binding.etHeaders.setText("Content-Type: application/json; charset=UTF-8\nAccept: application/json")
            binding.etRequestBody.setText("{\n  \"title\": \"Aesthetic Android Networking\",\n  \"body\": \"Demonstrating web services with HTTP in modern Android.\",\n  \"userId\": 1\n}")
        }

        binding.chipPresetPut.setOnClickListener {
            setMethodChip(HttpMethod.PUT)
            binding.etUrl.setText("https://jsonplaceholder.typicode.com/posts/1")
            binding.etHeaders.setText("Content-Type: application/json; charset=UTF-8\nAccept: application/json")
            binding.etRequestBody.setText("{\n  \"id\": 1,\n  \"title\": \"Updated Article Title\",\n  \"body\": \"Updated content via HTTP PUT web service.\",\n  \"userId\": 1\n}")
        }

        binding.chipPresetDelete.setOnClickListener {
            setMethodChip(HttpMethod.DELETE)
            binding.etUrl.setText("https://jsonplaceholder.typicode.com/posts/1")
            binding.etHeaders.setText("Accept: application/json")
        }

        binding.chipPresetHttpBin200.setOnClickListener {
            setMethodChip(HttpMethod.GET)
            binding.etUrl.setText("https://httpbin.org/status/200")
            binding.etHeaders.setText("Accept: application/json")
        }

        binding.chipPresetHttpBin404.setOnClickListener {
            setMethodChip(HttpMethod.GET)
            binding.etUrl.setText("https://httpbin.org/status/404")
            binding.etHeaders.setText("Accept: application/json")
        }

        binding.chipPresetHttpBinDelay.setOnClickListener {
            setMethodChip(HttpMethod.GET)
            binding.etUrl.setText("https://httpbin.org/delay/2")
            binding.etHeaders.setText("Accept: application/json")
        }
    }

    private fun setupHeadersToggle() {
        binding.btnToggleHeaders.setOnClickListener {
            val isVisible = binding.layoutHeadersContent.visibility == View.VISIBLE
            binding.layoutHeadersContent.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.tvHeadersCount.text = if (isVisible) "Tap to edit" else "Tap to hide"
        }
    }

    private fun setupBodyButtons() {
        binding.btnFormatRequestBody.setOnClickListener {
            val raw = binding.etRequestBody.text?.toString().orEmpty()
            if (raw.isNotBlank()) {
                val formatted = HttpServiceEngine.formatJson(raw)
                binding.etRequestBody.setText(formatted)
                Toast.makeText(requireContext(), "Formatted JSON", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnInsertSampleBody.setOnClickListener {
            insertSampleBody()
        }
    }

    private fun insertSampleBody() {
        binding.etRequestBody.setText("{\n  \"title\": \"Sample Article\",\n  \"body\": \"Hello from Android Web Service Demo!\",\n  \"userId\": 1\n}")
    }

    private fun ensureContentTypeHeader() {
        val currentHeaders = binding.etHeaders.text?.toString().orEmpty()
        if (!currentHeaders.contains("Content-Type", ignoreCase = true)) {
            val updated = if (currentHeaders.isBlank()) {
                "Content-Type: application/json; charset=UTF-8"
            } else {
                "$currentHeaders\nContent-Type: application/json; charset=UTF-8"
            }
            binding.etHeaders.setText(updated)
        }
    }

    private fun setupSendButton() {
        binding.btnSendRequest.setOnClickListener {
            val url = binding.etUrl.text?.toString()?.trim().orEmpty()
            if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                binding.tilUrl.error = "Please enter a valid URL starting with http:// or https://"
                return@setOnClickListener
            }
            binding.tilUrl.error = null

            val method = getSelectedMethod()
            val headersMap = parseHeaders(binding.etHeaders.text?.toString().orEmpty())
            val body = if (method.supportsBody) {
                binding.etRequestBody.text?.toString()?.trim()
            } else {
                null
            }

            val config = HttpRequestConfig(
                method = method,
                url = url,
                headers = headersMap,
                body = body,
                engine = viewModel.engineType.value
            )

            viewModel.sendCustomRequest(config)
        }

        binding.chipPlaygroundEngine.setOnClickListener {
            viewModel.toggleEngine()
        }
    }

    private fun parseHeaders(headersText: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        headersText.lines().forEach { line ->
            val colonIndex = line.indexOf(':')
            if (colonIndex != -1) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                if (key.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
        return map
    }

    private fun setupResponseTabs() {
        binding.chipGroupResponseTabs.setOnCheckedStateChangeListener { _, checkedIds ->
            activeResponseTab = when (binding.chipGroupResponseTabs.checkedChipId) {
                R.id.chipTabRaw -> TAB_RAW
                R.id.chipTabHeaders -> TAB_HEADERS
                R.id.chipTabCurl -> TAB_CURL
                else -> TAB_FORMATTED
            }
            updateResponseTabContent()
        }
    }

    private fun setupCopyButton() {
        binding.btnCopyResponse.setOnClickListener {
            val text = binding.tvResponseBody.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("HTTP Response", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.engineType.collect { engine ->
                        binding.chipPlaygroundEngine.text = engine.displayName
                    }
                }

                launch {
                    viewModel.requestState.collect { state ->
                        renderRequestState(state)
                    }
                }
            }
        }
    }

    private fun renderRequestState(state: RequestUiState) {
        when (state) {
            is RequestUiState.Loading -> {
                binding.btnSendRequest.isEnabled = false
                binding.btnSendRequest.text = "Executing Request..."
                binding.progressSend.visibility = View.VISIBLE
            }

            is RequestUiState.Result -> {
                binding.btnSendRequest.isEnabled = true
                binding.btnSendRequest.text = getString(R.string.send_request)
                binding.progressSend.visibility = View.GONE

                val resp = state.response
                lastResponseResult = resp

                // Update Status Badges
                val statusText = if (resp.statusCode > 0) {
                    "${resp.statusCode} ${resp.statusMessage}"
                } else {
                    resp.statusMessage.ifBlank { "Network Error" }
                }
                binding.tvRespStatusBadge.text = statusText

                val statusColor = when {
                    resp.isSuccess -> ContextCompat.getColor(requireContext(), R.color.status_2xx)
                    resp.isRedirect -> ContextCompat.getColor(requireContext(), R.color.status_3xx)
                    resp.isClientError -> ContextCompat.getColor(requireContext(), R.color.status_4xx)
                    else -> ContextCompat.getColor(requireContext(), R.color.status_5xx)
                }
                val statusBg = when {
                    resp.isSuccess -> ContextCompat.getColorStateList(requireContext(), R.color.status_2xx_bg)
                    resp.isRedirect -> ContextCompat.getColorStateList(requireContext(), R.color.status_3xx_bg)
                    resp.isClientError -> ContextCompat.getColorStateList(requireContext(), R.color.status_4xx_bg)
                    else -> ContextCompat.getColorStateList(requireContext(), R.color.status_5xx_bg)
                }

                binding.tvRespStatusBadge.setTextColor(statusColor)
                binding.tvRespStatusBadge.backgroundTintList = statusBg

                binding.tvRespDurationBadge.text = "${resp.durationMs} ms"
                binding.tvRespSizeBadge.text = resp.formattedSize
                binding.tvRespEngineBadge.text = resp.engineUsed.displayName

                updateResponseTabContent()
            }

            RequestUiState.Idle -> {
                binding.btnSendRequest.isEnabled = true
                binding.btnSendRequest.text = getString(R.string.send_request)
                binding.progressSend.visibility = View.GONE
            }
        }
    }

    private fun updateResponseTabContent() {
        val resp = lastResponseResult ?: return
        when (activeResponseTab) {
            TAB_FORMATTED -> {
                if (resp.body.isNotBlank()) {
                    binding.tvResponseBody.text = HttpServiceEngine.formatJson(resp.body)
                } else if (resp.errorReason != null) {
                    binding.tvResponseBody.text = "Error: ${resp.errorReason}"
                } else {
                    binding.tvResponseBody.text = "// (Empty Response Body)"
                }
            }

            TAB_RAW -> {
                binding.tvResponseBody.text = resp.body.ifBlank { resp.errorReason ?: "// (Empty Response Body)" }
            }

            TAB_HEADERS -> {
                if (resp.headers.isNotEmpty()) {
                    val sb = StringBuilder()
                    resp.headers.forEach { (k, v) ->
                        sb.append(k).append(": ").append(v).append("\n")
                    }
                    binding.tvResponseBody.text = sb.toString().trimEnd()
                } else {
                    binding.tvResponseBody.text = "// No Response Headers Captured"
                }
            }

            TAB_CURL -> {
                binding.tvResponseBody.text = resp.curlEquivalent.ifBlank {
                    HttpServiceEngine.buildCurlCommand(
                        HttpRequestConfig(
                            method = getSelectedMethod(),
                            url = binding.etUrl.text?.toString().orEmpty()
                        )
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAB_FORMATTED = 0
        private const val TAB_RAW = 1
        private const val TAB_HEADERS = 2
        private const val TAB_CURL = 3

        fun newInstance() = HttpPlaygroundFragment()
    }
}
