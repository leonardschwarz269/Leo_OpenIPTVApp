package com.kraftplay.openiptv.service

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kraftplay.openiptv.model.IptvChannel
import com.kraftplay.openiptv.model.Playlist
import com.kraftplay.openiptv.parser.M3uParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
open class PlaybackService : MediaLibraryService() {
    protected var mediaLibrarySession: MediaLibrarySession? = null
    protected val executor = Executors.newSingleThreadExecutor()
    protected val client = OkHttpClient()
    protected var cachedChannels = listOf<IptvChannel>()
    protected var favoriteUrls = setOf<String>()
    protected val mainHandler = Handler(Looper.getMainLooper())
    private var retryCount = 0

    override fun onCreate() {
        super.onCreate()

        // Настройка аппаратного звукового тракта под мультимедийный канал машины
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .build()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()

        attachPlaybackRecovery(player)
        loadData()
    }

    protected fun loadData() {
        val prefs = getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
        favoriteUrls = prefs.getStringSet("favorites", emptySet()) ?: emptySet()

        val cacheFile = File(cacheDir, "channels_cache.json")
        if (cacheFile.exists()) {
            try {
                val jsonText = cacheFile.readText()
                if (jsonText.isNotBlank()) {
                    cachedChannels = Json.decodeFromString<List<IptvChannel>>(jsonText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (cachedChannels.isEmpty()) {
            executor.execute {
                val jsonStr = prefs.getString("playlists_json", "[]") ?: "[]"
                val playlists = try {
                    Json.decodeFromString<List<Playlist>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
                val selected = playlists.find { it.isSelected } ?: playlists.firstOrNull()

                selected?.let { playlist ->
                    try {
                        val request = Request.Builder().url(playlist.url).build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body?.let { body ->
                                    val parsed = M3uParser.parse(body.byteStream())
                                    cachedChannels = parsed
                                    try {
                                        cacheFile.writeText(Json.encodeToString(parsed))
                                    } catch (_: Exception) {}

                                    mainHandler.post {
                                        mediaLibrarySession?.notifyChildrenChanged("ROOT", 0, null)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            mainHandler.post {
                mediaLibrarySession?.notifyChildrenChanged("ROOT", 0, null)
            }
        }
    }

    fun currentPlayer(): ExoPlayer? = mediaLibrarySession?.player as? ExoPlayer

    private fun attachPlaybackRecovery(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) retryCount = 0
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.takeIf { it.isNotEmpty() }?.let { recordRecent(it) }
            }

            override fun onPlayerError(error: PlaybackException) {
                retryCount++
                if (retryCount <= 5) {
                    mainHandler.postDelayed({
                        runCatching {
                            player.prepare()
                            player.playWhenReady = true
                        }
                    }, 1000L * retryCount)
                }
            }
        })
    }

    private fun recordRecent(url: String) {
        val recents = LinkedHashSet<String>().apply { add(url) }
        recents.addAll(recentUrls().filter { it != url })
        getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("recent_channels", recents.take(20).joinToString("\n"))
            .apply()
    }

    private fun recentUrls(): List<String> =
        getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
            .getString("recent_channels", "").orEmpty()
            .split("\n").filter { it.isNotEmpty() }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: android.content.Intent
        ): Boolean {
            val keyEvent = intent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                val prefs = getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
                val nextAction = prefs.getString("wheel_next_action", "NextChannel") ?: "NextChannel"
                val prevAction = prefs.getString("wheel_prev_action", "PrevChannel") ?: "PrevChannel"

                val player = session.player
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        when (nextAction) {
                            "NextChannel" -> player.seekToNextMediaItem()
                            "Skip10" -> player.seekTo((player.currentPosition + 10000L).coerceAtMost(player.duration.coerceAtLeast(0L)))
                            "Skip30" -> player.seekTo((player.currentPosition + 30000L).coerceAtMost(player.duration.coerceAtLeast(0L)))
                            "Skip60" -> player.seekTo((player.currentPosition + 60000L).coerceAtMost(player.duration.coerceAtLeast(0L)))
                            else -> player.seekToNextMediaItem()
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        when (prevAction) {
                            "PrevChannel" -> player.seekToPreviousMediaItem()
                            "Rewind10" -> player.seekTo((player.currentPosition - 10000L).coerceAtLeast(0L))
                            "Rewind30" -> player.seekTo((player.currentPosition - 30000L).coerceAtLeast(0L))
                            "Rewind60" -> player.seekTo((player.currentPosition - 60000L).coerceAtLeast(0L))
                            else -> player.seekToPreviousMediaItem()
                        }
                        return true
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("ROOT")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = mutableListOf<MediaItem>()
            when {
                parentId == "ROOT" -> {
                    items.add(createBrowsableItem("CAT_Favorites", "Favorites"))
                    items.add(createBrowsableItem("CAT_Recent", "Recent"))

                    val categories = cachedChannels.map { it.category ?: "Other" }.distinct().sorted()
                    categories.forEach { category ->
                        items.add(createBrowsableItem("CAT_$category", category))
                    }
                }
                parentId == "CAT_Favorites" -> {
                    cachedChannels.filter { favoriteUrls.contains(it.url) }.forEach { channel ->
                        items.add(createPlayableItem(channel))
                    }
                }
                parentId == "CAT_Recent" -> {
                    val recents = recentUrls()
                    cachedChannels
                        .filter { recents.contains(it.url) }
                        .sortedBy { recents.indexOf(it.url) }
                        .forEach { channel -> items.add(createPlayableItem(channel)) }
                }
                parentId.startsWith("CAT_") -> {
                    val category = parentId.substring(4)
                    cachedChannels.filter { (it.category ?: "Other") == category }.forEach { channel ->
                        items.add(createPlayableItem(channel))
                    }
                }
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> =
            Futures.immediateFuture(LibraryResult.ofVoid(params))

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val q = query.trim().lowercase()
            val result = cachedChannels
                .filter {
                    it.name.lowercase().contains(q) ||
                            (it.tvgName ?: "").lowercase().contains(q)
                }
                .map { createPlayableItem(it) }
            return Futures.immediateFuture(LibraryResult.ofItemList(result, params))
        }

        private fun createBrowsableItem(id: String, title: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

        private fun createPlayableItem(channel: IptvChannel): MediaItem {
            return MediaItem.Builder()
                .setMediaId(channel.url)
                .setUri(channel.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(channel.name)
                        .setSubtitle(channel.tvgName ?: channel.category)
                        .setArtworkUri(channel.logoUrl?.let { Uri.parse(it) })
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedItems = mediaItems.map { item ->
                if (item.localConfiguration == null) {
                    MediaItem.Builder()
                        .setUri(item.mediaId)
                        .setMediaMetadata(item.mediaMetadata)
                        .build()
                } else item
            }.toMutableList()
            return Futures.immediateFuture(updatedItems)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        executor.shutdown()
        super.onDestroy()
    }
}