package org.kaorun.nouto.ui.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.kaorun.nouto.BuildConfig
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.FragmentMainBinding
import org.kaorun.nouto.ui.adapter.NoteAdapter
import org.kaorun.nouto.ui.components.MainSearchView
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.fragments.base.BaseFragment
import org.kaorun.nouto.ui.model.LayoutMode
import org.kaorun.nouto.ui.utils.InsetsHandler
import org.kaorun.nouto.ui.utils.MarginItemDecoration
import org.kaorun.nouto.viewmodel.NotesViewModel
import org.kaorun.nouto.viewmodel.SearchViewModel

class MainFragment : BaseFragment(R.layout.fragment_main) {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var noteAdapter: NoteAdapter
    private val viewModel: NotesViewModel by navGraphViewModels(R.id.nav_graph)
    private val searchViewModel: SearchViewModel by viewModels()
    private var currentLayoutMode: LayoutMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        showRestoreSnackbar()
        observeNotes()
        setupRecyclerView()
        setupLayoutMode()
        setupSearchView()
        setupListeners()
        setupInsets()
    }

    private fun showRestoreSnackbar() {
        val intent = requireActivity().intent
        if (intent.getBooleanExtra("restore_success", false)) {
            Snackbar
                .make(
                    binding.root,
                    getString(R.string.restore_database_success),
                    Snackbar.LENGTH_SHORT
                )
                .setAnchorView(binding.fab)
                .show()
            intent.removeExtra("restore_success")
        }
    }

    private fun observeNotes() {
        viewModel.displayedNotes.observe(viewLifecycleOwner) { notes ->
            val newNote = notes.firstOrNull { it !in noteAdapter.currentList }
            noteAdapter.submitList(notes.toList()) {
                newNote?.let {
                    binding.recyclerView.smoothScrollToPosition(notes.indexOf(it))
                }
                binding.recyclerView.post {
                    binding.recyclerView.invalidateItemDecorations()
                }
            }
            val isSearching = !viewModel.searchQuery.value.isNullOrBlank()
            binding.recyclerView.isInvisible = notes.isEmpty() && isSearching
            binding.notesEmptyLayout.root.isVisible = notes.isEmpty() && !isSearching
            binding.nothingFoundLayout.root.isVisible = notes.isEmpty() && isSearching
        }

        viewModel.deletedNotes.observe(viewLifecycleOwner) {}

        viewModel.pendingDelete.observe(viewLifecycleOwner) { note ->
            note ?: return@observe
            setupSnackbar(note)
            viewModel.clearPendingDelete()
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onItemClick = { note -> openNoteFragment(note.id) },
            onDeleteClick = { note ->
                viewModel.setPendingDelete(note)
            }
        )
        binding.recyclerView.adapter = noteAdapter
    }

    private fun setupListeners() {
        setupNavigationButton()

        binding.fab.setOnClickListener {
            openNoteFragment(null)
        }

        binding.layoutButton.setOnClickListener {
            switchLayout()
        }
    }

    private fun setupSideSheet() {
        val sideSheetDialog = SideSheetDialog(requireContext())
        with(sideSheetDialog) {
            setContentView(R.layout.side_sheet)
            setFitsSystemWindows(false)
            show()
            setSheetEdge(Gravity.START)
        }

        sideSheetDialog.window?.let { window ->
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.navigationBarColor = Color.TRANSPARENT
                window.statusBarColor = Color.TRANSPARENT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val activityAppearance = requireActivity().window.insetsController
                    ?.systemBarsAppearance ?: 0
                window.insetsController?.setSystemBarsAppearance(
                    activityAppearance,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                val isLightMode =
                    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                        Configuration.UI_MODE_NIGHT_YES
                WindowInsetsControllerCompat(
                    window,
                    window.decorView
                ).isAppearanceLightStatusBars = isLightMode
            }
        }

        sideSheetDialog.findViewById<View>(R.id.sidebar_container)?.let { container ->
            InsetsHandler.applyViewInsets(container)
        }

        sideSheetDialog.findViewById<TextView>(R.id.sidebar_version)
            ?.text = "v${BuildConfig.VERSION_NAME}"

        val notesCount = viewModel.displayedNotes.value?.size ?: 0
        sideSheetDialog.findViewById<TextView>(R.id.nav_notes_count)?.apply {
            isVisible = notesCount > 0
            text = notesCount.toString()
        }

        val trashCount = viewModel.deletedNotes.value?.size ?: 0
        sideSheetDialog.findViewById<TextView>(R.id.nav_trash_count)?.apply {
            isVisible = trashCount > 0
            text = trashCount.toString()
        }

        sideSheetDialog.findViewById<View>(R.id.nav_notes)?.setOnClickListener {
            sideSheetDialog.dismiss()
        }

        sideSheetDialog.findViewById<View>(R.id.nav_trash)?.setOnClickListener {
            sideSheetDialog.dismiss()
            openTrashFragment()
        }

        sideSheetDialog.findViewById<View>(R.id.nav_settings)?.setOnClickListener {
            sideSheetDialog.dismiss()
            openSettingsFragment()
        }
    }

    private fun setupInsets() {
        val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
        InsetsHandler.applyViewInsets(
            binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false
        )
        InsetsHandler.applyViewInsets(binding.recyclerView, false)
        InsetsHandler.applyViewInsets(
            binding.searchRecyclerView,
            isTopPaddingEnabled = false,
            isBottomPaddingEnabled = true
        )
        InsetsHandler.applyViewInsets(binding.fab, fabMargin)
    }

    private fun setupNavigationButton() {
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (query.isNullOrBlank()) {
                binding.navigationButton.setIconResource(R.drawable.menu_24px)
                binding.navigationButton.setOnClickListener { setupSideSheet() }
            } else {
                binding.navigationButton.setIconResource(R.drawable.arrow_back_24px)
                binding.navigationButton.setOnClickListener {
                    viewModel.setSearchQuery(null)
                    binding.searchBar.clearText()
                }
            }
        }
    }

    private fun setupSearchView() {
        val searchResetCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.setSearchQuery(null)
                binding.searchBar.clearText()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            searchResetCallback
        )

        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            searchResetCallback.isEnabled = !query.isNullOrBlank()
        }
        MainSearchView(
            binding.searchView,
            binding.searchBar,
            binding.fab,
            binding.searchRecyclerView,
            binding.searchSuggestionsEmptyLayout.root,
            viewModel,
            searchViewModel,
            resources,
            viewLifecycleOwner,
            requireActivity()
        )
    }

    private fun setupSnackbar(note: Note) {
        Snackbars.showSnackbarWithUndo(
            view = binding.root,
            anchorView = binding.fab,
            message = getString(R.string.note_deleted_message),
            undoAction = {
                viewModel.unmarkDeleted(note)
            }
        )
    }

    private fun navigate(directions: NavDirections) {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().navigate(directions)
    }

    private fun openNoteFragment(noteId: Int?) =
        navigate(MainFragmentDirections.actionMainFragmentToNoteFragment(noteId ?: -1))

    private fun openTrashFragment() =
        navigate(MainFragmentDirections.actionMainFragmentToTrashFragment())

    private fun openSettingsFragment() =
        navigate(MainFragmentDirections.actionMainFragmentToSettingsMainFragment())

    private fun setupLayoutMode() {
        viewModel.layoutMode.observe(viewLifecycleOwner) { layoutMode ->
            val animate = currentLayoutMode != null && currentLayoutMode != layoutMode
            currentLayoutMode = layoutMode
            applyLayoutMode(layoutMode, animate)
        }
    }

    @SuppressLint("PrivateResource")
    private fun applyLayoutMode(layoutMode: LayoutMode, animate: Boolean = false) {
        binding.layoutButton.setIconResource(
            if (layoutMode == LayoutMode.GRID) R.drawable.view_agenda_24px
            else R.drawable.grid_view_24px
        )

        if (animate) {
            binding.layoutButton.animate().cancel()
            binding.layoutButton.scaleX = 0.8f
            binding.layoutButton.scaleY = 0.8f
            binding.layoutButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.6f))
                .start()

            TransitionManager.endTransitions(binding.recyclerView)
            val transition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(ChangeBounds())
                addTransition(Fade(Fade.IN))
                duration = 380L
                interpolator = AnimationUtils.loadInterpolator(
                    requireContext(),
                    com.google.android.material.R.interpolator.m3_sys_motion_easing_emphasized
                )
            }
            TransitionManager.beginDelayedTransition(binding.recyclerView, transition)
        }

        val existingLayoutManager = binding.recyclerView.layoutManager as? StaggeredGridLayoutManager
        if (existingLayoutManager != null) {
            existingLayoutManager.spanCount = layoutMode.spanCount
        } else {
            binding.recyclerView.layoutManager =
                StaggeredGridLayoutManager(layoutMode.spanCount, RecyclerView.VERTICAL)
        }

        repeat(binding.recyclerView.itemDecorationCount) {
            binding.recyclerView.removeItemDecorationAt(0)
        }

        binding.recyclerView.addItemDecoration(
            MarginItemDecoration(
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_outer_margin
                ),
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_inner_margin
                ),
                layoutMode.spanCount
            )
        )

        binding.recyclerView.clipChildren = layoutMode == LayoutMode.GRID
        binding.recyclerView.invalidateItemDecorations()
    }

    private fun switchLayout() {
        viewModel.toggleLayoutMode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentLayoutMode = null
        _binding = null
    }
}