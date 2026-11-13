package com.example.webservicedemo.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.webservicedemo.R
import com.example.webservicedemo.data.model.PostItem
import com.example.webservicedemo.databinding.DialogCreatePostBinding
import com.example.webservicedemo.databinding.DialogInspectJsonBinding
import com.example.webservicedemo.databinding.FragmentRestFeedBinding
import com.example.webservicedemo.ui.adapter.PostAdapter
import com.example.webservicedemo.ui.viewmodel.FeedUiState
import com.example.webservicedemo.ui.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch

class RestFeedFragment : Fragment() {

    private var _binding: FragmentRestFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var postAdapter: PostAdapter

    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onInspectJson = { post -> showInspectDialog(post) },
            onEdit = { post -> showEditPostDialog(post) },
            onDelete = { post -> confirmDeletePost(post) }
        )
        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.primary)
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadFeed()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadFeed()
        }

        binding.fabCreatePost.setOnClickListener {
            showCreatePostDialog()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.feedState.collect { state ->
                    renderFeedState(state)
                }
            }
        }
    }

    private fun renderFeedState(state: FeedUiState) {
        when (state) {
            is FeedUiState.Loading -> {
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.viewLoading.visibility = View.VISIBLE
                    binding.rvPosts.visibility = View.GONE
                }
                binding.viewError.visibility = View.GONE
                binding.viewEmpty.visibility = View.GONE
            }

            is FeedUiState.Success -> {
                binding.swipeRefresh.isRefreshing = false
                binding.viewLoading.visibility = View.GONE
                binding.viewError.visibility = View.GONE

                postAdapter.submitList(state.displayedPosts)

                if (state.displayedPosts.isEmpty()) {
                    binding.rvPosts.visibility = View.GONE
                    binding.viewEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvPosts.visibility = View.VISIBLE
                    binding.viewEmpty.visibility = View.GONE
                }

                // Update Stats Card
                val resp = state.response
                binding.tvStatTotal.text = getString(R.string.stat_total_posts, state.allPosts.size)
                binding.tvStatStatus.text = "${resp.statusCode} ${resp.statusMessage}".ifBlank { "200 OK" }
                binding.tvStatLatency.text = "${resp.durationMs} ms"
                binding.tvStatSize.text = resp.formattedSize

                val statusColor = when {
                    resp.isSuccess -> ContextCompat.getColor(requireContext(), R.color.status_2xx)
                    resp.isClientError -> ContextCompat.getColor(requireContext(), R.color.status_4xx)
                    else -> ContextCompat.getColor(requireContext(), R.color.status_5xx)
                }
                binding.tvStatStatus.setTextColor(statusColor)
            }

            is FeedUiState.Error -> {
                binding.swipeRefresh.isRefreshing = false
                binding.viewLoading.visibility = View.GONE
                binding.rvPosts.visibility = View.GONE
                binding.viewEmpty.visibility = View.GONE
                binding.viewError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }

            FeedUiState.Idle -> {
                // Initial
            }
        }
    }

    private fun showCreatePostDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogCreatePostBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvDialogTitle.text = "HTTP POST Request"
        dialogBinding.tvDialogMethodBadge.text = "POST"
        dialogBinding.tvDialogMethodBadge.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.method_post_bg)
        dialogBinding.tvDialogMethodBadge.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.method_post)
        )

        dialogBinding.btnCancelPost.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSubmitPost.setOnClickListener {
            val title = dialogBinding.etPostTitle.text?.toString()?.trim().orEmpty()
            val body = dialogBinding.etPostBody.text?.toString()?.trim().orEmpty()

            if (title.isBlank()) {
                dialogBinding.tilPostTitle.error = "Title is required"
                return@setOnClickListener
            }

            dialogBinding.btnSubmitPost.isEnabled = false
            dialogBinding.btnSubmitPost.text = "Sending HTTP POST..."

            viewModel.createPost(title, body) { success, _ ->
                dialog.dismiss()
                if (success) {
                    binding.rvPosts.scrollToPosition(0)
                    Snackbar.make(
                        binding.root,
                        "Successfully sent HTTP POST to web service!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        dialog.show()
    }

    private fun showEditPostDialog(post: PostItem) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogCreatePostBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvDialogTitle.text = "HTTP PUT Request (#${post.id})"
        dialogBinding.tvDialogSubtitle.text = "Target: https://jsonplaceholder.typicode.com/posts/${post.id}"
        dialogBinding.tvDialogMethodBadge.text = "PUT"
        dialogBinding.tvDialogMethodBadge.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.method_put_bg)
        dialogBinding.tvDialogMethodBadge.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.method_put)
        )

        dialogBinding.etPostTitle.setText(post.title)
        dialogBinding.etPostBody.setText(post.body)

        dialogBinding.btnCancelPost.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSubmitPost.setOnClickListener {
            val title = dialogBinding.etPostTitle.text?.toString()?.trim().orEmpty()
            val body = dialogBinding.etPostBody.text?.toString()?.trim().orEmpty()

            if (title.isBlank()) {
                dialogBinding.tilPostTitle.error = "Title is required"
                return@setOnClickListener
            }

            dialogBinding.btnSubmitPost.isEnabled = false
            dialogBinding.btnSubmitPost.text = "Sending HTTP PUT..."

            viewModel.updatePost(post.id, title, body) { success, _ ->
                dialog.dismiss()
                if (success) {
                    Snackbar.make(
                        binding.root,
                        "HTTP PUT updated post #${post.id} on web service!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        dialog.show()
    }

    private fun confirmDeletePost(post: PostItem) {
        Snackbar.make(
            binding.root,
            "Send HTTP DELETE for item #${post.id}?",
            Snackbar.LENGTH_LONG
        ).setAction("DELETE") {
            viewModel.deletePost(post.id) { success, _ ->
                if (success) {
                    Snackbar.make(
                        binding.root,
                        "HTTP DELETE completed for item #${post.id}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }.show()
    }

    private fun showInspectDialog(post: PostItem) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogInspectJsonBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val prettyJson = gson.toJson(post)
        dialogBinding.tvInspectTitle.text = "JSON Payload for Post #${post.id}"
        dialogBinding.tvInspectUrl.text = "GET https://jsonplaceholder.typicode.com/posts/${post.id}"
        dialogBinding.tvInspectCode.text = prettyJson

        dialogBinding.btnCopyJson.setOnClickListener {
            copyToClipboard("Post #${post.id} JSON", prettyJson)
            Toast.makeText(requireContext(), "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnCloseInspect.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = RestFeedFragment()
    }
}
