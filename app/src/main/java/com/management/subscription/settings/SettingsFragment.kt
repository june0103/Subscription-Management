package com.management.subscription.settings

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.management.subscription.R
import com.management.subscription.data.SettingsRepository
import com.management.subscription.databinding.FragmentSettingsBinding
import com.management.subscription.notifications.NotificationPermissionHelper
import com.management.subscription.notifications.ReminderScheduler
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = checkNotNull(_binding)

    private var isRenderingState = false
    private var latestState = SettingsUiState()

    private val settingsRepository by lazy(LazyThreadSafetyMode.NONE) {
        SettingsRepository.getInstance(requireContext().applicationContext)
    }
    private val reminderScheduler by lazy(LazyThreadSafetyMode.NONE) {
        ReminderScheduler.getInstance(requireContext().applicationContext)
    }
    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            this,
            SettingsViewModel.factory(settingsRepository, reminderScheduler)
        )[SettingsViewModel::class.java]
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.setNotificationsEnabled(enabled = true, canSchedule = true)
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.settings_permission_denied,
                    Snackbar.LENGTH_SHORT
                ).show()
                binding.switchNotifications.isChecked = false
                viewModel.syncScheduling(canSchedule = false)
                renderStatus(latestState)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderStatus(latestState)
        viewModel.syncScheduling(canSchedule = NotificationPermissionHelper.hasNotificationPermission(requireContext()))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun configureListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isRenderingState) return@setOnCheckedChangeListener

            if (isChecked) {
                if (NotificationPermissionHelper.hasNotificationPermission(requireContext())) {
                    viewModel.setNotificationsEnabled(enabled = true, canSchedule = true)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setNotificationsEnabled(enabled = true, canSchedule = true)
                }
            } else {
                viewModel.setNotificationsEnabled(enabled = false, canSchedule = false)
            }
        }

        binding.layoutReminderTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(latestState.reminderHour)
            .setMinute(latestState.reminderMinute)
            .setTitleText(R.string.settings_time_picker_title)
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.updateReminderTime(
                hour = picker.hour,
                minute = picker.minute,
                canSchedule = latestState.notificationsEnabled &&
                    NotificationPermissionHelper.hasNotificationPermission(requireContext())
            )
        }
        picker.show(childFragmentManager, "reminder_time")
    }

    private fun renderState(state: SettingsUiState) {
        latestState = state
        isRenderingState = true

        if (binding.switchNotifications.isChecked != state.notificationsEnabled) {
            binding.switchNotifications.isChecked = state.notificationsEnabled
        }

        binding.tvReminderTimeValue.text =
            SubscriptionFormatters.reminderTime(state.reminderHour, state.reminderMinute)
        binding.layoutReminderTime.alpha = if (state.notificationsEnabled) 1f else 0.76f

        renderStatus(state)
        isRenderingState = false
    }

    private fun renderStatus(state: SettingsUiState) {
        if (_binding == null) return

        val formattedTime = SubscriptionFormatters.reminderTime(
            state.reminderHour,
            state.reminderMinute
        )
        val appNotificationsEnabled = NotificationManagerCompat.from(requireContext())
            .areNotificationsEnabled()

        binding.tvNotificationStatus.text = when {
            !state.notificationsEnabled -> getString(R.string.settings_status_off, formattedTime)
            !NotificationPermissionHelper.hasNotificationPermission(requireContext()) ->
                getString(R.string.settings_status_permission_required, formattedTime)
            !appNotificationsEnabled -> getString(R.string.settings_status_system_disabled)
            else -> getString(R.string.settings_status_on, formattedTime)
        }
    }
}
