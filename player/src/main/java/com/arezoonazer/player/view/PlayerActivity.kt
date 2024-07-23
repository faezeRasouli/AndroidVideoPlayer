package com.arezoonazer.player.view

import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import com.arezoonazer.player.R
import com.arezoonazer.player.databinding.ActivityPlayerBinding
import com.arezoonazer.player.databinding.ExoPlayerViewBinding
import com.arezoonazer.player.extension.gone
import com.arezoonazer.player.extension.hideSystemUI
import com.arezoonazer.player.extension.resolveSystemGestureConflict
import com.arezoonazer.player.extension.setImageButtonTintColor
import com.arezoonazer.player.extension.visible
import com.arezoonazer.player.util.CustomPlaybackState
import com.arezoonazer.player.util.track.TrackEntity
import com.arezoonazer.player.view.track.TrackSelectionDialog
import com.arezoonazer.player.viewmodel.PlayerViewModel
import com.arezoonazer.player.viewmodel.QualityViewModel
import com.arezoonazer.player.viewmodel.SubtitleViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private val binding: ActivityPlayerBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    private val exoBinding: ExoPlayerViewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ExoPlayerViewBinding.bind(binding.root)
    }

    private val viewModel: PlayerViewModel by viewModels()

    private val subtitleViewModel: SubtitleViewModel by viewModels()

    private val qualityViewModel: QualityViewModel by viewModels()

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewModel.onActivityCreate(this)
        qualityViewModel.onActivityCreated()
        subtitleViewModel.onActivityCrated()
        resolveSystemGestureConflict()
        initClickListeners()

        exoBinding.exoControllerPlaceholder.exoProgress.addListener(object : TimeBar.OnScrubListener {
            @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                updateThumbnailPosition(timeBar as DefaultTimeBar , position)

            }

            @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                updateThumbnailPosition(timeBar as DefaultTimeBar , position)
                exoBinding.exoControllerPlaceholder.thumbnailPreview.visibility = ImageView.VISIBLE
                Glide.with(this@PlayerActivity)
                    .load("https://static.cdn.asset.filimo.com//filimo-video/158168-thumb-t01.webp")
                    .apply(
                        RequestOptions()
                            .override(100, 200)
                            .transform(GlideThumbnailTransformations(position))
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                    )
                    .into(exoBinding.exoControllerPlaceholder.thumbnailPreview)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                exoBinding.exoControllerPlaceholder.thumbnailPreview.visibility = ImageView.GONE
            }
        })


        with(viewModel) {
            playerLiveData.observe(this@PlayerActivity) { exoPlayer ->
                binding.exoPlayerView.player = exoPlayer
            }

            playbackStateLiveData.observe(this@PlayerActivity) { playbackState ->
                setProgressbarVisibility(playbackState)
                setVideoControllerVisibility(playbackState)
            }

            isMuteLiveData.observe(
                this@PlayerActivity,
                exoBinding.exoControllerPlaceholder.muteButton::setMuteState
            )
        }

        with(qualityViewModel) {
            qualityEntitiesLiveData.observe(this@PlayerActivity, ::setupQualityButton)
            onQualitySelectedLiveData.observe(this@PlayerActivity) {
                dismissTrackSelectionDialogIfExist()
            }
        }

        with(subtitleViewModel) {
            subtitleEntitiesLiveData.observe(this@PlayerActivity, ::setupSubtitleButton)
            onSubtitleSelectedLiveData.observe(this@PlayerActivity) {
                dismissTrackSelectionDialogIfExist()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    @OptIn(UnstableApi::class)
    private fun updateThumbnailPosition(timeBar: DefaultTimeBar, position: Long) {
        val duration = binding.exoPlayerView.player?.duration ?: 0

        if (duration > 0) {
            val scrubberPositionRatio = position.toFloat() / duration.toFloat()
            val scrubberPositionPx = scrubberPositionRatio * timeBar.width
            val thumbnailWidth = exoBinding.exoControllerPlaceholder.thumbnailPreview.width
            val thumbnailHalfWidth = thumbnailWidth / 2
            val screenWidth = resources.displayMetrics.widthPixels
            val thumbnailX = scrubberPositionPx - thumbnailHalfWidth
            when {
                thumbnailX < 0 -> exoBinding.exoControllerPlaceholder.thumbnailPreview.x = 0f
                thumbnailX + thumbnailWidth > screenWidth -> exoBinding.exoControllerPlaceholder.thumbnailPreview.x = (screenWidth - thumbnailWidth).toFloat()
                else -> exoBinding.exoControllerPlaceholder.thumbnailPreview.x = thumbnailX
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayerView()
    }

    private fun initClickListeners() {
        with(exoBinding.exoControllerPlaceholder) {
            exoBackButton.setOnClickListener { onBackPressed() }
            playPauseButton.setOnClickListener { viewModel.onPlayButtonClicked() }
            muteButton.setOnClickListener { viewModel.onMuteClicked() }
            replayButton.setOnClickListener { viewModel.onReplayClicked() }
        }
    }

    private fun setProgressbarVisibility(playbackState: CustomPlaybackState) {
        binding.progressBar.isVisible = playbackState == CustomPlaybackState.LOADING
    }

    private fun setVideoControllerVisibility(playbackState: CustomPlaybackState) {
        exoBinding.exoControllerPlaceholder.run {
            playPauseButton.setState(playbackState)
            when (playbackState) {
                CustomPlaybackState.PLAYING,
                CustomPlaybackState.PAUSED,
                -> {
                    root.visible()
                    replayButton.gone()
                }

                CustomPlaybackState.ERROR,
                CustomPlaybackState.ENDED,
                -> {
                    replayButton.visible()
                }

                else -> {
                    replayButton.gone()
                }
            }
        }
    }

    private fun setupQualityButton(qualities: List<TrackEntity>) {
        exoBinding.exoControllerPlaceholder.qualityButton.apply {
            if (qualities.isNotEmpty()) {
                setImageButtonTintColor(R.color.white)
                setOnClickListener { openTrackSelectionDialog(qualities) }
            }
        }
    }

    private fun setupSubtitleButton(subtitles: List<TrackEntity>) {
        exoBinding.exoControllerPlaceholder.subtitleButton.apply {
            if (subtitles.isNotEmpty()) {
                setImageButtonTintColor(R.color.white)
                setOnClickListener { openTrackSelectionDialog(subtitles) }
            }
        }
    }

    private fun openTrackSelectionDialog(items: List<TrackEntity>) {
        dismissTrackSelectionDialogIfExist()
        TrackSelectionDialog.newInstance(items).show(supportFragmentManager, DIALOG_TAG)
    }

    private fun dismissTrackSelectionDialogIfExist() {
        with(supportFragmentManager) {
            val previousDialog = supportFragmentManager.findFragmentByTag(DIALOG_TAG)
            if (previousDialog != null) {
                (previousDialog as TrackSelectionDialog).dismiss()
                beginTransaction().remove(previousDialog)
            }
        }
    }

    private fun releasePlayerView() {
        with(binding.exoPlayerView) {
            removeAllViews()
            player = null
        }
    }

    companion object {
        const val PLAYER_PARAMS_EXTRA = "playerParamsExtra"
        private const val DIALOG_TAG = "dialogTag"
    }
}
