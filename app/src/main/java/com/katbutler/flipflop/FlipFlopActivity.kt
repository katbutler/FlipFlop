package com.katbutler.flipflop

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.opengl.Visibility
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
import com.spotify.sdk.android.player.*
import com.spotify.sdk.android.player.Spotify
import kotlinx.android.synthetic.main.activity_flip_flop.*
import kotlinx.android.synthetic.main.activity_flip_flop.view.*
import kotlinx.android.synthetic.main.playlist_item.view.*
import android.view.MenuInflater
import android.text.style.ForegroundColorSpan
import android.text.SpannableString
import com.katbutler.flipflop.R.color.*
import com.katbutler.flipflop.spotifynet.UnauthorizedException


class FlipFlopActivity : AppCompatActivity() {

    companion object {
        const val TAG = "FlipFlopActivity"
    }

    private val spotifyNet by lazy {
        SpotifyNet(this)
    }

    private lateinit var playlistsRecyclerView: RecyclerView
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val selectedPlaylists: MutableList<Playlist> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_flop)

        viewManager = LinearLayoutManager(this)

        playlistsRecyclerView = playlists_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
        }

        selected_playlist_left_name.ellipsize = TextUtils.TruncateAt.END
        selected_playlist_right_name.ellipsize = TextUtils.TruncateAt.END
        selected_playlist_left_name.setSingleLine()
        selected_playlist_right_name.setSingleLine()

        initSpotify()
    }

    private fun initSpotify() {
        SpotifyPrefs.getAccessToken(this) ?: return LoginActivity.showLoginActivity(this)

        fetchSpotifyData()
    }

    private fun fetchSpotifyData() {
        spotifyNet.getCurrentUserProfile({ userProfile ->
            SpotifyPrefs.saveCurrentUserID(this, userProfile.id)
        })

        spotifyNet.getPlaylistsForCurrentUser(
                { playlists ->
                    populatePlaylistsRecyclerView(playlists)
                },
                { throwable ->
                    if (throwable is UnauthorizedException) LoginActivity.showLoginActivity(this)
                    else throwable?.let { throw it }
                })
    }

    private fun populatePlaylistsRecyclerView(playlists: Playlists) {
        playlistsAdapter = PlaylistsAdapter(playlists) { playlist ->
            val selectedCount = playlists.items.filter { it.selected }.size

            if (playlist.selected) {
                removePlaylistFromPanel(playlist)
                return@PlaylistsAdapter true
            } else if (selectedCount < 2) {
                addPlaylistToPanel(playlist)
                return@PlaylistsAdapter true
            }
            return@PlaylistsAdapter false
        }
        playlistsRecyclerView.adapter = playlistsAdapter
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

    //region Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.playlists_menu, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(0)?.let {
            val s = SpannableString("start")
            val disabled = selectedPlaylists.count() != 2

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
        R.id.action_start -> {
            showPlayer()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }
    //endregion
}
