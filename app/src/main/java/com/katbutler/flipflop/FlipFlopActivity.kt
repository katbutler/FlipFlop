package com.katbutler.flipflop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.katbutler.flipflop.adapters.PlaylistsAdapter
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.katbutler.flipflop.spotifynet.SpotifyNet
import com.katbutler.flipflop.spotifynet.models.Playlist
import com.katbutler.flipflop.spotifynet.models.Playlists
import kotlinx.android.synthetic.main.activity_flip_flop.*
import android.text.style.ForegroundColorSpan
import android.text.SpannableString
import android.widget.Toast
import com.katbutler.flipflop.R.color.*
import com.katbutler.flipflop.spotifynet.UnauthorizedException
import java.net.UnknownHostException


class FlipFlopActivity : AppCompatActivity() {

    companion object {
        const val TAG = "FlipFlopActivity"
    }

    private val spotifyNet by lazy {
        SpotifyNet(this)
    }

    private lateinit var playlistsRecyclerView: RecyclerView
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var playlistsAdapter: PlaylistsAdapter? = null

    private val selectedPlaylists: MutableList<Playlist> = arrayListOf()

    private val connectivityManager by lazy { getSystemService(ConnectivityManager::class.java) }

    private val connectivityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> connectivityChanged(intent)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_flop)

        registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        viewManager = LinearLayoutManager(this)

        playlistsRecyclerView = playlists_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
        }

        fab.setOnClickListener {
            showPlayer()
        }

        selected_playlist_left_name.ellipsize = TextUtils.TruncateAt.END
        selected_playlist_right_name.ellipsize = TextUtils.TruncateAt.END
        selected_playlist_left_name.setSingleLine()
        selected_playlist_right_name.setSingleLine()

        initSpotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectivityReceiver)
    }

    private fun initSpotify() {
        SpotifyPrefs.getAccessToken(this) ?: return LoginActivity.showLoginActivity(this)

        fetchSpotifyData()
    }

    private fun fetchSpotifyData() {
        spotifyNet.getCurrentUserProfile({ userProfile ->
            SpotifyPrefs.saveCurrentUserID(this, userProfile.id)
            if (userProfile.product != "premium") {
                showError(R.string.need_premium_account, R.drawable.ic_need_premium_grey_60dp)
                try {
                    unregisterReceiver(connectivityReceiver)
                } catch (e: Exception) {
                }

                return@getCurrentUserProfile
            }
            updatePlaylists()
        }, this::handleNetworkError)
    }

    private fun handleNetworkError(throwable: Throwable?) {
        when (throwable) {
            is UnauthorizedException -> LoginActivity.showLoginActivity(this)

            is UnknownHostException -> showError()

            else -> {
                showError()
                Toast.makeText(this, throwable?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showError(errorStringID: Int = R.string.could_not_fetch_playlists, errorImageID: Int = R.drawable.ic_cloud_off_grey_60dp) {
        error_text_view.text = getString(errorStringID)
        error_icon_image.setImageResource(errorImageID)
        no_connection_layout.visibility = View.VISIBLE
        selected_playlists_panel.visibility = View.GONE
        playlistsRecyclerView.visibility = View.GONE
        fab.visibility = View.GONE
    }

    private fun hideNetworkError() {
        no_connection_layout.visibility = View.GONE
        selected_playlists_panel.visibility = View.VISIBLE
        playlistsRecyclerView.visibility = View.VISIBLE
        maybeShowFab()
    }

    private fun updatePlaylists() {
        spotifyNet.getPlaylistsForCurrentUser(
                { playlists ->
                    populatePlaylistsRecyclerView(playlists)
                },
                this::handleNetworkError)
    }

    private fun connectivityChanged(intent: Intent) {
        Log.d(TAG, "connectivityChanged")
        val network = connectivityManager.activeNetworkInfo
        if (network?.isAvailable == true) {
            hideNetworkError()
            if (playlistsAdapter?.playlists == null || playlistsAdapter?.playlists?.total == 0) {
                fetchSpotifyData()
            }
        } else {
            showError()
        }
    }

    private fun populatePlaylistsRecyclerView(playlists: Playlists) {
        playlistsAdapter = PlaylistsAdapter(playlists) { playlist ->
            try {
                val selectedCount = playlists.items.filter { it.selected }.size

                if (playlist.selected) {
                    removePlaylistFromPanel(playlist)
                    return@PlaylistsAdapter true
                } else if (selectedCount < 2) {
                    addPlaylistToPanel(playlist)
                    return@PlaylistsAdapter true
                }
                return@PlaylistsAdapter false
            } finally {
                maybeShowFab()
            }
        }
        playlistsRecyclerView.adapter = playlistsAdapter
    }

    private fun maybeShowFab() {
        if (selectedPlaylists.count() == 2) fab.show() else fab.hide()
    }

    private fun addPlaylistToPanel(playlist: Playlist) {
        selectedPlaylists.add(playlist)
        drawPanel()
    }

    private fun removePlaylistFromPanel(playlist: Playlist) {
        selectedPlaylists.remove(playlist)
        drawPanel()
    }

    private fun drawPanel() {
        setLayoutVisibilities()
        invalidateOptionsMenu() //needed to disable/enable start option

        when (selectedPlaylists.count()) {
            1 -> {
                val selectedPlaylist = selectedPlaylists[0]
                bindPlaylistPanel(selectedPlaylist, selected_playlist_left_name, selected_playlist_left_image)
            }
            2 -> {
                val selectedPlaylist = selectedPlaylists[0]
                val selectedRightPlaylist = selectedPlaylists[1]
                bindPlaylistPanel(selectedPlaylist, selected_playlist_left_name, selected_playlist_left_image)
                bindPlaylistPanel(selectedRightPlaylist, selected_playlist_right_name, selected_playlist_right_image)
            }
            else -> {
            }
        }
    }

    private fun setLayoutVisibilities() {
        val count = selectedPlaylists.count()

        playlist_placeholder_left.visibility = if (count == 0) View.VISIBLE else View.GONE
        playlist_placeholder_right.visibility = if (count == 2) View.GONE else View.VISIBLE

        selected_playlist_left.visibility = if (count > 0) View.VISIBLE else View.GONE
        selected_playlist_right.visibility = if (count == 2) View.VISIBLE else View.GONE
    }

    private fun bindPlaylistPanel(playlist: Playlist, playlistTextView: TextView, playlistImageView: ImageView) {
        playlistTextView.text = playlist.name

        val smallImg = playlist.images.firstOrNull { it.height > 60 } ?: playlist.images.firstOrNull()
        smallImg?.let {
            val options = RequestOptions().placeholder(R.drawable.flipflop_icon_grey)

            Glide.with(this)
                    .setDefaultRequestOptions(options)
                    .load(it.url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(playlistImageView)
        }
    }

    private fun showPlayer() {
        val playerIntent = Intent(this, PlayerActivity::class.java)
        playerIntent.apply {
            putExtra("playlist1", selectedPlaylists[0].id)
            putExtra("playlist2", selectedPlaylists[1].id)
            putExtra("playlist1uri", selectedPlaylists[0].uri)
            putExtra("playlist2uri", selectedPlaylists[1].uri)
        }

        startActivity(playerIntent)
    }

    private fun clearSelectedPlaylists() {
        selectedPlaylists.clear()
        playlistsAdapter?.playlists?.items?.forEach { it.selected = false }
        drawPanel()
        playlistsAdapter?.notifyDataSetChanged()
        fab.hide()
    }

    //region Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.playlists_menu, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(0)?.let {
            val s = SpannableString("clear")
            val disabled = selectedPlaylists.count() < 1

            if (disabled) {
                s.setSpan(ForegroundColorSpan(resources.getColor(lightGreenTextColor)), 0, s.length, 0)
            } else {
                s.setSpan(ForegroundColorSpan(Color.WHITE), 0, s.length, 0)
            }

            it.isEnabled = !disabled
            it.title = s
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_clear -> {
            clearSelectedPlaylists()
            true
        }

        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }
    //endregion
}
