package com.arezoonazer.player.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.arezoonazer.player.argument.PlayerParams
import com.arezoonazer.player.databinding.ActivityPlayerBinding
import com.arezoonazer.player.databinding.ExoPlayerViewBinding
import com.arezoonazer.player.di.AssistedFactory
import com.arezoonazer.player.extension.hideSystemUI
import com.arezoonazer.player.extension.resolveSystemGestureConflict
import com.arezoonazer.player.util.CustomPlaybackState
import com.arezoonazer.player.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private val binding: ActivityPlayerBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    private val exoBinding: ExoPlayerViewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ExoPlayerViewBinding.bind(binding.root)
    }

    private val playerParams: PlayerParams by lazy(LazyThreadSafetyMode.NONE) {
        intent.getSerializableExtra(PLAYER_PARAMS_EXTRA) as PlayerParams
    }

    @Inject
    lateinit var playerViewModelFactory: AssistedFactory

    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModel.provideFactory(playerViewModelFactory, playerParams)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel.onActivityCreate(this)
        resolveSystemGestureConflict()
        initClickListeners()

        with(viewModel) {
            playerLiveData.observe(this@PlayerActivity) { exoPlayer ->
                binding.exoPlayerView.player = exoPlayer
            }

            playbackStateLiveData.observe(this@PlayerActivity) { playbackState ->
                setProgressbarVisibility(playbackState)
                // any UI update can be done here
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI(binding.root)
    }

    private fun initClickListeners() {
        with(exoBinding) {
            exoControllerPlaceholder.exoBackButton.setOnClickListener { onBackPressed() }
        }
    }

    private fun setProgressbarVisibility(playbackState: CustomPlaybackState) {
        binding.progressBar.isVisible = playbackState == CustomPlaybackState.LOADING
    }

    companion object {
        const val PLAYER_PARAMS_EXTRA = "playerParamsExtra"
    }
}