package com.cadence.songs.server

import android.content.Context
import com.cadence.songs.data.Repository
import com.cadence.songs.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.random.Random

/** Passerelle entre le serveur web (qui ne connaît pas Android) et la base de données de l'appli. */
private class AndroidSongStore(private val repo: Repository) : SongStore {
    private fun Song.toDto() = SongDto(
        id = id, title = title, artist = artist, key = songKey, tempo = tempo, timeSig = timeSig,
        capo = capo, tags = tags, notes = notes, content = content, favorite = favorite,
        hasPdf = pdfPath != null
    )

    override fun list(): List<SongDto> = runBlocking { repo.allSongs().map { it.toDto() } }

    override fun get(id: Long): SongDto? = runBlocking { repo.getSong(id)?.toDto() }

    override fun save(song: SongDto): Long = runBlocking {
        if (song.id == 0L) {
            repo.saveSong(
                Song(
                    title = song.title, artist = song.artist, songKey = song.key, tempo = song.tempo,
                    timeSig = song.timeSig, capo = song.capo, tags = song.tags, notes = song.notes,
                    content = song.content
                )
            )
        } else {
            val old = repo.getSong(song.id) ?: return@runBlocking 0L
            repo.saveSong(
                old.copy(
                    title = song.title, artist = song.artist, songKey = song.key, tempo = song.tempo,
                    timeSig = song.timeSig, capo = song.capo, tags = song.tags, notes = song.notes,
                    content = song.content
                )
            )
        }
    }

    override fun delete(id: Long) = runBlocking {
        repo.getSong(id)?.let { repo.deleteSong(it) }
        Unit
    }

    override fun savePdf(id: Long, fileName: String, bytes: ByteArray): Boolean =
        runBlocking { repo.savePdfBytes(id, fileName, bytes) }

    override fun readPdf(id: Long): ByteArray? = runBlocking { repo.readPdfBytes(id) }

    override fun removePdf(id: Long) = runBlocking { repo.removePdf(id) }
}

/**
 * « Transfert Wi-Fi » : ouvre une page web sur le réseau local pour ajouter ou modifier des morceaux
 * depuis un ordinateur. Protégé par un code à 4 chiffres ; tourne tant que l'appli reste ouverte.
 */
class WifiTransfer(context: Context, repo: Repository) {
    data class State(
        val running: Boolean = false,
        val addresses: List<String> = emptyList(),
        val pin: String = "",
        val error: String? = null
    )

    private val prefs = context.applicationContext.getSharedPreferences("cadence", Context.MODE_PRIVATE)
    private val api = WebApi(AndroidSongStore(repo)) { currentPin() }
    private var server: MiniHttpServer? = null

    private val _state = MutableStateFlow(State(pin = currentPin()))
    val state: StateFlow<State> = _state

    private fun currentPin(): String {
        val existing = prefs.getString("wifi_pin", null)
        if (existing != null && existing.length == 4) return existing
        return newPinValue()
    }

    private fun newPinValue(): String {
        val pin = Random.nextInt(0, 10000).toString().padStart(4, '0')
        prefs.edit().putString("wifi_pin", pin).apply()
        return pin
    }

    fun newPin() {
        _state.value = _state.value.copy(pin = newPinValue())
    }

    fun refreshAddresses() {
        val s = server
        if (s != null && s.isRunning) {
            _state.value = _state.value.copy(addresses = localUrls(s.port))
        }
    }

    @Synchronized
    fun start() {
        if (server?.isRunning == true) return
        try {
            val s = MiniHttpServer(8080) { api.handle(it) }
            val port = s.start()
            server = s
            _state.value = State(running = true, addresses = localUrls(port), pin = currentPin())
        } catch (e: Exception) {
            _state.value = State(running = false, pin = currentPin(), error = "Impossible de démarrer : ${e.message}")
        }
    }

    @Synchronized
    fun stop() {
        server?.stop()
        server = null
        _state.value = State(running = false, pin = currentPin())
    }

    private fun localUrls(port: Int): List<String> {
        val found = mutableListOf<Pair<Boolean, String>>()
        runCatching {
            for (nif in NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        val wifi = nif.name.startsWith("wlan") || nif.name.startsWith("eth")
                        found.add(wifi to "http://${addr.hostAddress}:$port")
                    }
                }
            }
        }
        return found.sortedByDescending { it.first }.map { it.second }
    }
}
