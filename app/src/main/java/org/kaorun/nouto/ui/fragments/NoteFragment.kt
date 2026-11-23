package org.kaorun.nouto.ui.fragments

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import androidx.transition.TransitionManager
import com.google.android.material.behavior.HideViewOnScrollBehavior
import com.onegravity.rteditor.RTManager
import com.onegravity.rteditor.api.RTApi
import com.onegravity.rteditor.api.RTMediaFactoryImpl
import com.onegravity.rteditor.api.RTProxyImpl
import com.onegravity.rteditor.api.format.RTFormat
import kotlinx.coroutines.launch
import kotlin.math.hypot
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.FragmentNoteBinding
import org.kaorun.nouto.ui.components.DeleteDialog
import org.kaorun.nouto.ui.components.NoteActionsMenu
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.components.TextStyleFloatingToolbar
import org.kaorun.nouto.ui.fragments.base.BaseFragment
import org.kaorun.nouto.ui.utils.AppLockManager
import org.kaorun.nouto.ui.utils.DateFormatUtils
import org.kaorun.nouto.ui.utils.FileUtils
import org.kaorun.nouto.ui.utils.InsetsHandler
import org.kaorun.nouto.ui.utils.toPlainText
import org.kaorun.nouto.ui.utils.trimHtml
import org.kaorun.nouto.data.PreferenceAppearanceKeys
import org.kaorun.nouto.viewmodel.NotesViewModel

class NoteFragment : BaseFragment(R.layout.fragment_note) {
    private lateinit var rtManager: RTManager
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotesViewModel by navGraphViewModels(R.id.nav_graph)
    private val args: NoteFragmentArgs by navArgs()
    private val filePicker = setupFilePicker()
    private var note: Note? = null
    private var isDeleting = false
    private var isRestoring = false
    private var isPinned = false
    private var isImporting = false
    private var isTitleVisible = true
    private var isInitializingNote = false
    private var isInsertingNote = false
    private var savedTitleHtml: String = ""
    private var savedContentHtml: String = ""
    private var currentNoteTime: Long = System.currentTimeMillis()
    private var metadataAnimator: ValueAnimator? = null
    private val metadataHoldHandler = Handler(Looper.getMainLooper())
    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private val metadataUpdateHandler = Handler(Looper.getMainLooper())
    private val metadataUpdateRunnable = Runnable { updateMetadata() }
    private val autoSaveRunnable = Runnable {
        if (!isDeleting && !isImporting && _binding != null) {
            saveNote()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRtEditor(savedInstanceState)
        setupNote()
        setupInsets()
        setupToolbar()
        setupListeners()
    }

    private fun setupRtEditor(savedInstanceState: Bundle?) {
        val rtApi = RTApi(
            requireContext(),
            RTProxyImpl(requireActivity()),
            RTMediaFactoryImpl(requireContext(), true)
        )

        rtManager = RTManager(rtApi, savedInstanceState)
        rtManager.registerEditor(binding.noteTitle, true)
        rtManager.registerEditor(binding.noteContent, true)
    }

    private fun setupNote() {
        val noteId = args.noteId

        if (noteId != -1) {
            val isInViewOnlyMode = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(PreferenceAppearanceKeys.IS_VIEW_ONLY_MODE, false)

            viewModel.getNote(noteId).observe(viewLifecycleOwner) { existingNote ->
                val it = existingNote ?: return@observe
                if (note == null) {
                    isInitializingNote = true
                    note = it
                    isPinned = it.isPinned
                    currentNoteTime = it.time

                    rtManager.unregisterEditor(binding.noteTitle)
                    rtManager.unregisterEditor(binding.noteContent)
                    binding.noteTitle.setRichTextEditing(true, it.title)
                    binding.noteContent.setRichTextEditing(true, it.content)
                    rtManager.registerEditor(binding.noteTitle, true)
                    rtManager.registerEditor(binding.noteContent, true)
                    binding.noteTitle.setSelection(binding.noteTitle.text?.length ?: 0)
                    binding.noteContent.setSelection(binding.noteContent.text?.length ?: 0)

                    savedTitleHtml = getNormalizedHtml(binding.noteTitle.getText(RTFormat.HTML))
                    savedContentHtml = getNormalizedHtml(binding.noteContent.getText(RTFormat.HTML))

                    isInitializingNote = false
                    updateMetadata()
                    updateUIState(note, !isRestoring, isInViewOnlyMode)
                } else {
                    note = it
                    isPinned = it.isPinned
                    currentNoteTime = it.time
                    updateMetadata()
                    if (isRestoring) {
                        updateUIState(note, false, isInViewOnlyMode)
                    }
                }
            }
        } else {
            isInitializingNote = true
            currentNoteTime = System.currentTimeMillis()
            if (args.title != null || args.content != null) {
                isImporting = true
                rtManager.unregisterEditor(binding.noteTitle)
                rtManager.unregisterEditor(binding.noteContent)
                binding.noteTitle.setRichTextEditing(true, args.title)
                binding.noteContent.setRichTextEditing(true, args.content)
                rtManager.registerEditor(binding.noteTitle, true)
                rtManager.registerEditor(binding.noteContent, true)
                savedTitleHtml = getNormalizedHtml(binding.noteTitle.getText(RTFormat.HTML))
                savedContentHtml = getNormalizedHtml(binding.noteContent.getText(RTFormat.HTML))
                isInitializingNote = false
                updateMetadata()
                updateUIState(null, false)
            } else {
                binding.noteTitle.setRichTextEditing(true, null)
                binding.noteContent.setRichTextEditing(true, null)
                savedTitleHtml = getNormalizedHtml(binding.noteTitle.getText(RTFormat.HTML))
                savedContentHtml = getNormalizedHtml(binding.noteContent.getText(RTFormat.HTML))
                isInitializingNote = false
                updateMetadata()
                updateUIState(null, true)
            }
        }
    }

    private fun setupToolbar() {
        val textStyleFloatingToolbar = TextStyleFloatingToolbar(
            rtManager = rtManager,
            actionUndo = binding.actionUndo,
            actionRedo = binding.actionRedo,
            styleBold = binding.styleBold,
            styleItalic = binding.styleItalic,
            styleUnderline = binding.styleUnderline,
            styleStrikethrough = binding.styleStrikethrough,
            styleBulletList = binding.styleBulletList,
            styleNumberedList = binding.styleNumberedList,
            fields = listOf(binding.noteTitle, binding.noteContent),
            onFormatChanged = {
                _binding?.root?.post {
                    onEditorChanged()
                }
            }
        )
        textStyleFloatingToolbar.setupFloatingToolbar()
    }

    private fun setupListeners() {
        setupChangeListener()
        setupMetadataListeners()

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            with(binding.buttonEditNote) {
                if (isExtended && scrollY > oldScrollY) shrink()
                if (!isExtended && scrollY < oldScrollY) extend()
                if (scrollY == 0) extend()
            }
            if (scrollY < oldScrollY || scrollY == 0) {
                slideInFloatingToolbar()
            }
        }

        listOf(binding.noteTitle, binding.noteContent).forEach { editor ->
            editor.setOnClickListener {
                slideInFloatingToolbar()
            }
            editor.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    slideInFloatingToolbar()
                }
            }
        }

        binding.buttonEditNote.setOnClickListener {
            updateUIState(
                note = note,
                isShowKeyboard = true,
                isInViewOnlyMode = false,
                animateEditTransition = true
            )
        }

        binding.topAppBar.setNavigationOnClickListener {
            closeNoteFragment()
        }

        binding.buttonDeletePermanently.setOnClickListener {
            val currentNote = note ?: return@setOnClickListener
            DeleteDialog.show(requireContext(), resources) {
                isDeleting = true
                viewModel.deleteNote(currentNote)
                closeNoteFragment()
            }
        }

        binding.buttonRestore.setOnClickListener {
            val currentNote = note ?: return@setOnClickListener
            isRestoring = true
            showFloatingToolbar()
            viewModel.unmarkDeleted(currentNote)
            setupRestoreSnackbar(currentNote)
        }

        binding.buttonBack.setOnClickListener {
            closeNoteFragment()
        }

        binding.buttonImport.setOnClickListener {
            isImporting = false
            setupImportSnackbar()
            closeNoteFragment()
        }

        binding.buttonMenu.setOnClickListener { view ->
            NoteActionsMenu.show(
                anchor = view,
                note = note ?: Note(
                    title = binding.noteTitle.getText(RTFormat.HTML),
                    content = binding.noteContent.getText(RTFormat.HTML),
                    time = currentNoteTime,
                    isPinned = isPinned
                ),
                onPin = {
                    setupPinSnackbar()
                    isPinned = !isPinned
                    saveNote()
                },
                onExport = { exportNote() },
                onDelete = {
                    note?.let {
                        isDeleting = true
                        viewModel.setPendingDelete(it)
                        closeNoteFragment()
                    } ?: run {
                        DeleteDialog.show(requireContext(), resources) {
                            isDeleting = true
                            closeNoteFragment()
                        }
                    }
                }
            )
        }
    }

    private fun setupRestoreSnackbar(note: Note) {
        Snackbars.showSnackbarWithUndo(
            view = binding.root,
            anchorView = binding.floatingToolbar,
            message = getString(R.string.note_restored_message),
            undoAction = {
                viewModel.markDeleted(note)
            }
        )
    }

    private fun setupPinSnackbar() {
        val messageRes = if (isPinned) {
            R.string.note_unpinned_message
        } else R.string.note_pinned_message

        Snackbars.showSnackbarWithUndo(
            view = binding.root,
            anchorView = binding.floatingToolbar,
            message = getString(messageRes),
            undoAction = {
                isPinned = !isPinned
                saveNote()
            }
        )
    }

    private fun setupExportSnackbar(e: String? = null) {
        val message = e?.let {
            "${getString(R.string.error_occured)}: $e"
        } ?: run {
            getString(R.string.export_success)
        }
        Snackbars.showSnackbarNoAction(
            view = binding.root,
            anchorView = binding.floatingToolbar,
            message = message
        )
    }

    private fun setupImportSnackbar() {
        val activityView = requireActivity().findViewById<View>(android.R.id.content)
        Snackbars.showSnackbarNoAction(
            view = activityView,
            anchorView = null,
            message = getString(R.string.note_imported_message)
        )
    }

    private fun getNormalizedHtml(html: String?): String {
        val raw = html.orEmpty()
        return if (raw.toPlainText().isBlank()) "" else raw.trimHtml()
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentTitle = getNormalizedHtml(binding.noteTitle.getText(RTFormat.HTML))
        val currentContent = getNormalizedHtml(binding.noteContent.getText(RTFormat.HTML))
        if (note == null && currentTitle.isEmpty() && currentContent.isEmpty()) return false
        return currentTitle != savedTitleHtml || currentContent != savedContentHtml
    }

    private fun updateMetadata() {
        val b = _binding ?: return
        b.noteTime.text = DateFormatUtils.formatNoteTime(requireContext(), currentNoteTime)
        val plainContent = b.noteContent.getText(RTFormat.HTML).orEmpty().toPlainText()
        val wordCount = if (plainContent.isBlank()) 0 else plainContent.split(Regex("\\s+")).size
        val charCount = plainContent.length
        b.noteWords.text = getString(R.string.note_words_count, wordCount)
        b.noteCharacters.text = getString(R.string.note_characters_count, charCount)
    }

    private fun scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
        autoSaveHandler.postDelayed(autoSaveRunnable, 500L)
    }

    private fun onEditorChanged() {
        if (!isInitializingNote && _binding != null && !isImporting) {
            metadataUpdateHandler.removeCallbacks(metadataUpdateRunnable)
            metadataUpdateHandler.postDelayed(metadataUpdateRunnable, METADATA_UPDATE_DEBOUNCE_MS)
            if (hasUnsavedChanges()) {
                scheduleAutoSave()
            }
        }
    }

    private fun setupChangeListener() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onEditorChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        listOf(
            binding.styleBold,
            binding.styleItalic,
            binding.styleUnderline,
            binding.styleStrikethrough,
            binding.styleBulletList,
            binding.styleNumberedList
        ).forEach {
            it.addOnCheckedChangeListener { _, _ ->
                binding.root.post {
                    onEditorChanged()
                }
            }
        }

        listOf(binding.noteTitle, binding.noteContent).forEach {
            it.addTextChangedListener(watcher)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMetadataListeners() {
        val metadataButtons = listOf(
            binding.noteTime,
            binding.noteWords,
            binding.noteCharacters
        )
        val bouncyInterpolator = OvershootInterpolator(1.2f)
        val density = resources.displayMetrics.density
        val basePaddingPx = (8 * density).toInt()
        val outerRadiusPx = 24f * density
        val innerRadiusPx = 6f * density
        val gapPx = resources.getDimensionPixelSize(R.dimen.recycler_view_segmented_list_margin)
        val currentWeights = FloatArray(metadataButtons.size) { 1f }
        var currentActiveButton: View? = null

        fun updateButtonAppearance(bounceFactor: Float = 1f) {
            metadataButtons.forEachIndexed { i, button ->
                val w = currentWeights[i].coerceIn(0f, 1f)
                if (w <= 0.005f && button != currentActiveButton && currentActiveButton != null) {
                    button.visibility = View.GONE
                    button.scaleX = 1f
                    button.scaleY = 1f
                } else {
                    button.visibility = View.VISIBLE
                    (button.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                        lp.weight = w.coerceAtLeast(0.001f)
                        lp.marginStart = if (i == 0) 0 else (gapPx * minOf(w, currentWeights[i - 1].coerceIn(0f, 1f))).toInt()
                    }
                    val pad = (basePaddingPx * w).toInt()
                    if (button.paddingLeft != pad) {
                        button.setPadding(pad, button.paddingTop, pad, button.paddingBottom)
                    }

                    if (currentActiveButton != null && button != currentActiveButton) {
                        button.scaleX = 1f
                        button.scaleY = 1f
                        button.alpha = (w / 0.10f).coerceIn(0f, 1f)
                    } else {
                        button.scaleX = bounceFactor
                        button.scaleY = bounceFactor
                        button.alpha = (w / 0.10f).coerceIn(0f, 1f)
                    }

                    val leftNeighborFactor = if (i == 0) 0f else currentWeights[i - 1].coerceIn(0f, 1f)
                    val rightNeighborFactor = if (i == metadataButtons.lastIndex) 0f else currentWeights[i + 1].coerceIn(0f, 1f)
                    val leftRadius = outerRadiusPx - (outerRadiusPx - innerRadiusPx) * leftNeighborFactor
                    val rightRadius = outerRadiusPx - (outerRadiusPx - innerRadiusPx) * rightNeighborFactor

                    button.shapeAppearanceModel = button.shapeAppearanceModel.toBuilder()
                        .setTopLeftCornerSize(leftRadius)
                        .setBottomLeftCornerSize(leftRadius)
                        .setTopRightCornerSize(rightRadius)
                        .setBottomRightCornerSize(rightRadius)
                        .build()
                }
            }
        }

        updateButtonAppearance()

        fun animateMetadataState(activeButton: View?, durationMs: Long = 700L) {
            val container = _binding?.noteMetadataContainer ?: return
            metadataAnimator?.cancel()
            currentActiveButton = activeButton

            val startWeights = currentWeights.copyOf()
            val targetWeights = FloatArray(metadataButtons.size) { i ->
                if (activeButton == null || metadataButtons[i] == activeButton) 1f else 0f
            }

            metadataButtons.forEachIndexed { i, button ->
                button.ellipsize = null
                if (targetWeights[i] > 0f) {
                    button.visibility = View.VISIBLE
                }
            }

            metadataAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMs
                interpolator = bouncyInterpolator
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    val clampedFraction = fraction.coerceIn(0f, 1f)
                    for (i in metadataButtons.indices) {
                        currentWeights[i] =
                            startWeights[i] + (targetWeights[i] - startWeights[i]) * clampedFraction
                    }
                    val overshoot = fraction - clampedFraction
                    val bounceScale = 0.94f + 0.06f * clampedFraction + overshoot * 0.35f
                    updateButtonAppearance(bounceScale)
                    container.requestLayout()
                }
                doOnEnd {
                    metadataButtons.forEach { it.ellipsize = TextUtils.TruncateAt.END }
                }
                start()
            }
        }

        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        val holdThresholdMs = 150L
        var downX = 0f
        var downY = 0f
        var isHoldTriggered = false
        var isMovedOut = false
        var pendingHoldRunnable: Runnable? = null

        fun cancelPendingHold() {
            pendingHoldRunnable?.let { metadataHoldHandler.removeCallbacks(it) }
            pendingHoldRunnable = null
        }

        metadataButtons.forEach { button ->
            button.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        cancelPendingHold()
                        downX = event.x
                        downY = event.y
                        isHoldTriggered = false
                        isMovedOut = false
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        v.isPressed = true

                        val holdRunnable = Runnable {
                            isHoldTriggered = true
                            if (currentActiveButton != v) {
                                animateMetadataState(v)
                            }
                        }
                        pendingHoldRunnable = holdRunnable
                        metadataHoldHandler.postDelayed(holdRunnable, holdThresholdMs)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!isHoldTriggered && !isMovedOut &&
                            hypot(event.x - downX, event.y - downY) > touchSlop
                        ) {
                            isMovedOut = true
                            cancelPendingHold()
                            v.isPressed = false
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        cancelPendingHold()
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                        v.isPressed = false
                        if (isHoldTriggered) {
                            animateMetadataState(null)
                        } else if (!isMovedOut) {
                            v.performClick()
                            animateMetadataState(
                                activeButton = if (currentActiveButton == v) null else v,
                                durationMs = 420L
                            )
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        cancelPendingHold()
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                        v.isPressed = false
                        if (isHoldTriggered) {
                            animateMetadataState(null)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(
            view = binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false,
        )
        InsetsHandler.applyViewInsets(
            view = binding.contentContainer,
            isTopPaddingEnabled = false,
            isBottomPaddingEnabled = true
        )
        InsetsHandler.applyViewInsets(
            view = binding.noteContent,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = true,
            isHorizontalPaddingEnabled = false,
            isTopSystemPaddingEnabled = false,
            additionalPadding = resources.getDimensionPixelSize(R.dimen.note_container_bottom_margin)
        )

        InsetsHandler.applyViewInsets(
            view = binding.buttonGroupImport,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = true,
            isHorizontalPaddingEnabled = true,
            isAdditionalTopPaddingEnabled = true,
            isAdditionalHorizontalPaddingEnabled = true,
            isTopSystemPaddingEnabled = false,
            additionalPadding = resources.getDimensionPixelSize(R.dimen.button_group_padding)
        )
        InsetsHandler.applyViewInsets(
            binding.buttonEditNote,
            resources.getDimensionPixelSize(R.dimen.fab_margin)
        )
        InsetsHandler.applyImeInsets(binding.buttonEditNote)
        InsetsHandler.applyImeInsets(binding.scrollView)
        InsetsHandler.applyImeInsets(binding.floatingToolbar)
        InsetsHandler.applyViewInsets(
            view = binding.buttonGroupBottom,
            isAdditionalTopPaddingEnabled = true,
            isAdditionalHorizontalPaddingEnabled = true,
            isTopSystemPaddingEnabled = false,
            additionalPadding = resources.getDimensionPixelSize(R.dimen.button_group_padding)
        )
    }

    private fun slideInFloatingToolbar() {
        val toolbar = _binding?.floatingToolbar ?: return
        if (!toolbar.isVisible) return
        val behavior = (toolbar.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
            as? HideViewOnScrollBehavior<*>
        @Suppress("UNCHECKED_CAST")
        (behavior as? HideViewOnScrollBehavior<View>)?.slideIn(toolbar)
    }

    @SuppressLint("PrivateResource")
    private fun showFloatingToolbar() {
        val toolbar = _binding?.floatingToolbar ?: return
        slideInFloatingToolbar()
        if (toolbar.isVisible && toolbar.alpha == 1f && toolbar.translationY == 0f) return

        toolbar.animate().cancel()
        val slideOffset = 48f * resources.displayMetrics.density
        toolbar.alpha = 0f
        toolbar.translationY = slideOffset
        toolbar.isVisible = true
        toolbar.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(
                resources.getInteger(
                    com.google.android.material.R.integer.m3_sys_motion_duration_medium4
                ).toLong()
            )
            .setInterpolator(
                AnimationUtils.loadInterpolator(
                    requireContext(),
                    com.google.android.material.R.interpolator.m3_sys_motion_easing_emphasized_decelerate
                )
            )
            .start()
    }

    private fun updateUIState(
        note: Note?,
        isShowKeyboard: Boolean,
        isInViewOnlyMode: Boolean = false,
        animateEditTransition: Boolean = false
    ) {
        val isDeleted = !isDeleting && (note?.isDeleted ?: false)
        val isEditable = !isInViewOnlyMode && !isImporting && !isDeleted

        listOf(binding.noteTitle, binding.noteContent).forEach {
            it.isEnabled = !isDeleted
            it.isFocusable = isEditable
            it.isFocusableInTouchMode = isEditable
            it.isClickable = isEditable
            it.isCursorVisible = isEditable
        }

        binding.buttonMenu.isVisible = !isImporting && !isDeleted
        binding.buttonGroupBottom.isVisible = isDeleted && !isImporting
        binding.buttonGroupMenu.isVisible = !isDeleted && !isImporting

        if (isEditable) {
            if (animateEditTransition && binding.buttonEditNote.isVisible) {
                binding.buttonEditNote.hide()
            } else {
                binding.buttonEditNote.isVisible = false
            }
            showFloatingToolbar()
        } else {
            binding.buttonEditNote.isVisible = !isDeleted && !isImporting
            binding.floatingToolbar.animate().cancel()
            binding.floatingToolbar.isVisible = false
        }

        isTitleVisible = PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getBoolean(PreferenceAppearanceKeys.IS_SHOW_TITLE, true)

        if (!isTitleVisible) {
            binding.noteTitle.isVisible = false
            binding.noteContentSpacing.isVisible = true
        }

        val isShowKeyboardPreference = PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getBoolean(PreferenceAppearanceKeys.IS_SHOW_KEYBOARD, true)

        if (isEditable && isShowKeyboard && isShowKeyboardPreference) {
            showKeyboard()
        }

        if (isImporting) {
            if (args.title.isNullOrBlank()) {
                binding.noteTitle.hint = getString(R.string.empty_title)
            }
            if (args.content.isNullOrBlank()) {
                binding.noteContent.hint = getString(R.string.empty_content)
            }
            binding.buttonGroupImport.isVisible = true
            binding.topAppBar.navigationIcon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.close_24px
            )
        } else {
            binding.buttonGroupImport.isVisible = false
            binding.topAppBar.navigationIcon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.arrow_back_24px
            )
        }
    }
    private fun saveNote() {
        val title = getNormalizedHtml(binding.noteTitle.getText(RTFormat.HTML))
        val content = getNormalizedHtml(binding.noteContent.getText(RTFormat.HTML))
        val time = System.currentTimeMillis()

        note?.let { existing ->
            if (title != savedTitleHtml || content != savedContentHtml || existing.isPinned != isPinned) {
                val updated = existing.copy(
                    title = title,
                    content = content,
                    time = time,
                    isPinned = isPinned
                )
                note = updated
                savedTitleHtml = title
                savedContentHtml = content
                currentNoteTime = time
                updateMetadata()
                viewModel.updateNote(updated)
            }
        } ?: run {
            if (!isInsertingNote && (title.isNotEmpty() || content.isNotEmpty())) {
                isInsertingNote = true
                savedTitleHtml = title
                savedContentHtml = content
                currentNoteTime = time
                updateMetadata()
                viewModel.addNote(
                    title = title,
                    content = content,
                    time = time,
                    isPinned = isPinned
                ) { insertedNote ->
                    isInsertingNote = false
                    note = insertedNote
                    if (_binding != null && !isDeleting && !isImporting && hasUnsavedChanges()) {
                        saveNote()
                    }
                }
            }
        }
    }

    private fun showKeyboard() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val focusOnContent = prefs.getBoolean(PreferenceAppearanceKeys.IS_FOCUS_ON_CONTENT, false)

        val focusableView = if (!isTitleVisible || focusOnContent) {
            binding.noteContent
        } else {
            binding.noteTitle
        }

        focusableView.requestFocus()
        WindowCompat.getInsetsController(requireActivity().window, focusableView)
            .show(WindowInsetsCompat.Type.ime())
    }

    private fun setupFilePicker(): ActivityResultLauncher<String?> = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            autoSaveHandler.removeCallbacks(autoSaveRunnable)
            saveNote()

            val title = binding.noteTitle.getText(RTFormat.HTML).orEmpty()
            val content = binding.noteContent.getText(RTFormat.HTML).orEmpty()

            lifecycleScope.launch {
                FileUtils.writeNoteToFile(
                    context = requireContext(),
                    uri = it,
                    title = title,
                    content = content
                ).onSuccess {
                    setupExportSnackbar(null)
                }.onFailure { e ->
                    setupExportSnackbar(e.localizedMessage)
                }
            }
        }
    }

    private fun exportNote() {
        val title = binding.noteTitle.getText(RTFormat.HTML).toPlainText()
        val content = binding.noteContent.getText(RTFormat.HTML).toPlainText()
        val defaultFileName = when {
            title.isNotBlank() -> "${title.take(20)}.json"
            content.isNotBlank() -> "${content.take(20)}.json"
            else -> "note.json"
        }

        AppLockManager.suppressNextLockOnce()
        filePicker.launch(defaultFileName)
    }

    private fun closeNoteFragment() {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().popBackStack()
    }

    override fun onStop() {
        super.onStop()
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
        if (!isDeleting && !isImporting && (hasUnsavedChanges() || (note != null && note?.isPinned != isPinned))) {
            saveNote()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        rtManager.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoSaveHandler.removeCallbacksAndMessages(null)
        metadataUpdateHandler.removeCallbacksAndMessages(null)
        metadataHoldHandler.removeCallbacksAndMessages(null)
        metadataAnimator?.cancel()
        metadataAnimator = null
        binding.floatingToolbar.animate().cancel()
        rtManager.onDestroy(requireActivity().isFinishing)
        _binding = null
    }

    private companion object {
        const val METADATA_UPDATE_DEBOUNCE_MS = 150L
    }
}
