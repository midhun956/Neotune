package com.example.neotune

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var isCurrentSongLiked = false

    private val likeUpdateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == "com.example.neotune.ACTION_UPDATE_LIKE") {
                isCurrentSongLiked = intent.getBooleanExtra("is_liked", false)
                updateNotificationLayout()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()

        val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()
        player.setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
        player.setHandleAudioBecomingNoisy(true)
        player.addListener(object : Player.Listener {
            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                if (playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                    player.pause()
                }
            }
        })

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Custom Skip Previous Session Command
        val prevCommand = SessionCommand("ACTION_PREVIOUS", Bundle.EMPTY)
        val prevButton = CommandButton.Builder()
                .setSessionCommand(prevCommand)
                .setDisplayName("Previous")
                .setIconResId(R.drawable.ic_skip_previous)
                .setEnabled(true)
                .build()

        // Custom Skip Next Session Command
        val nextCommand = SessionCommand("ACTION_NEXT", Bundle.EMPTY)
        val nextButton = CommandButton.Builder()
                .setSessionCommand(nextCommand)
                .setDisplayName("Next")
                .setIconResId(R.drawable.ic_skip_next)
                .setEnabled(true)
                .build()

        // Custom "Like" Session Command
        val likeCommand = SessionCommand("ACTION_LIKE", Bundle.EMPTY)
        val likeButton = CommandButton.Builder()
                .setSessionCommand(likeCommand)
                .setDisplayName("Like")
                .setIconResId(R.drawable.ic_heart_outline)
                .setEnabled(true)
                .build()

        // Build MediaSession using custom layout buttons to bypass empty-playlist disablement hacks
        mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent)
                .setCallback(object : MediaSession.Callback {
                    override fun onConnect(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): MediaSession.ConnectionResult {
                        val connectionResult = super.onConnect(session, controller)
                        val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                                .add(prevCommand)
                                .add(nextCommand)
                                .add(likeCommand)
                                .build()
                        
                        // Explicitly remove standard Previous and Next controls so the OS hides duplicate player buttons
                        val availablePlayerCommands = connectionResult.availablePlayerCommands.buildUpon()
                                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                                .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                                .remove(Player.COMMAND_SEEK_TO_NEXT)
                                .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                                .build()

                        return MediaSession.ConnectionResult.accept(
                            availableSessionCommands,
                            availablePlayerCommands
                        )
                    }

                    override fun onCustomCommand(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        customCommand: SessionCommand,
                        args: Bundle
                    ): ListenableFuture<SessionResult> {
                        when (customCommand.customAction) {
                            "ACTION_PREVIOUS" -> {
                                val prevIntent = Intent("com.example.neotune.ACTION_PREVIOUS").apply {
                                    setPackage(packageName)
                                }
                                sendBroadcast(prevIntent)
                            }
                            "ACTION_NEXT" -> {
                                val nextIntent = Intent("com.example.neotune.ACTION_NEXT").apply {
                                    setPackage(packageName)
                                }
                                sendBroadcast(nextIntent)
                            }
                            "ACTION_LIKE" -> {
                                val likeIntent = Intent("com.example.neotune.ACTION_LIKE").apply {
                                    setPackage(packageName)
                                }
                                sendBroadcast(likeIntent)
                            }
                        }
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                })
                .setCustomLayout(listOf(prevButton, nextButton, likeButton))
                .build()

        // Register internal broadcast receiver to dynamically update the Heart like icon state
        val filter = android.content.IntentFilter("com.example.neotune.ACTION_UPDATE_LIKE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(likeUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(likeUpdateReceiver, filter)
        }
    }

    private fun updateNotificationLayout() {
        val session = mediaSession ?: return

        val prevCommand = SessionCommand("ACTION_PREVIOUS", Bundle.EMPTY)
        val prevButton = CommandButton.Builder()
                .setSessionCommand(prevCommand)
                .setDisplayName("Previous")
                .setIconResId(R.drawable.ic_skip_previous)
                .setEnabled(true)
                .build()

        val nextCommand = SessionCommand("ACTION_NEXT", Bundle.EMPTY)
        val nextButton = CommandButton.Builder()
                .setSessionCommand(nextCommand)
                .setDisplayName("Next")
                .setIconResId(R.drawable.ic_skip_next)
                .setEnabled(true)
                .build()

        val likeCommand = SessionCommand("ACTION_LIKE", Bundle.EMPTY)
        val likeButton = CommandButton.Builder()
                .setSessionCommand(likeCommand)
                .setDisplayName("Like")
                .setIconResId(
                    if (isCurrentSongLiked) R.drawable.ic_heart
                    else R.drawable.ic_heart_outline
                )
                .setEnabled(true)
                .build()

        session.setCustomLayout(listOf(prevButton, nextButton, likeButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
            mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        try {
            unregisterReceiver(likeUpdateReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        super.onDestroy()
    }
}
