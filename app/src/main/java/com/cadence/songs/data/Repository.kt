package com.cadence.songs.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.cadence.songs.music.ChordPro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections

class Repository(private val context: Context, db: AppDatabase) {
    private val songDao = db.songDao()
    private val setDao = db.setListDao()

    // ---------- Morceaux ----------

    val songs: Flow<List<Song>> = songDao.observeAll()

    fun observeSong(id: Long): Flow<Song?> = songDao.observe(id)

    suspend fun getSong(id: Long): Song? = songDao.get(id)

    suspend fun saveSong(song: Song): Long {
        return if (song.id == 0L) {
            songDao.insert(song)
        } else {
            songDao.update(song.copy(updatedAt = System.currentTimeMillis()))
            song.id
        }
    }

    /** Mise à jour des réglages de lecture (transposition, capo, vitesse…) sans toucher à la date de modif. */
    suspend fun updateSong(song: Song) = songDao.update(song)

    suspend fun deleteSong(song: Song) {
        songDao.delete(song)
        song.pdfPath?.let { path -> runCatching { File(path).delete() } }
    }

    suspend fun seedIfEmpty(sample: Song) {
        if (songDao.count() == 0) songDao.insert(sample)
    }

    // ---------- Sets ----------

    val setLists: Flow<List<SetList>> = setDao.observeAll()
    val setCounts: Flow<List<SetCount>> = setDao.observeCounts()

    fun observeSetList(id: Long): Flow<SetList?> = setDao.observe(id)
    fun observeEntries(id: Long): Flow<List<EntryWithSong>> = setDao.observeEntries(id)

    suspend fun createSetList(name: String): Long = setDao.insert(SetList(name = name))

    suspend fun renameSetList(setList: SetList, name: String) = setDao.update(setList.copy(name = name))

    suspend fun deleteSetList(setList: SetList) = setDao.delete(setList)

    suspend fun addToSetList(setId: Long, songIds: List<Long>) {
        var position = setDao.maxPosition(setId) + 1
        val entries = songIds.map { SetEntry(setListId = setId, songId = it, position = position++) }
        setDao.insertEntries(entries)
    }

    suspend fun removeEntry(setId: Long, entryId: Long) {
        setDao.deleteEntry(entryId)
        setDao.entries(setId).forEachIndexed { index, e ->
            if (e.position != index) setDao.setPosition(e.id, index)
        }
    }

    suspend fun moveEntry(setId: Long, entryId: Long, delta: Int) {
        val list = setDao.entries(setId).toMutableList()
        val i = list.indexOfFirst { it.id == entryId }
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return
        Collections.swap(list, i, j)
        list.forEachIndexed { index, e -> setDao.setPosition(e.id, index) }
    }

    // ---------- Accès pour le serveur Wi-Fi ----------

    suspend fun allSongs(): List<Song> = songDao.all()

    /** Enregistre un PDF reçu (par Wi-Fi) et le rattache au morceau ; l'ancien PDF est supprimé. */
    suspend fun savePdfBytes(songId: Long, name: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val song = songDao.get(songId) ?: return@runCatching false
            val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "partition.pdf" }
            val dest = File(pdfDir(), "${System.currentTimeMillis()}_$safe")
            dest.writeBytes(bytes)
            song.pdfPath?.let { old -> runCatching { File(old).delete() } }
            songDao.update(song.copy(pdfPath = dest.absolutePath, updatedAt = System.currentTimeMillis()))
            true
        }.getOrDefault(false)
    }

    suspend fun readPdfBytes(songId: Long): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val path = songDao.get(songId)?.pdfPath ?: return@runCatching null
            File(path).takeIf { it.exists() }?.readBytes()
        }.getOrNull()
    }

    suspend fun removePdf(songId: Long) = withContext(Dispatchers.IO) {
        val song = songDao.get(songId) ?: return@withContext
        song.pdfPath?.let { old -> runCatching { File(old).delete() } }
        songDao.update(song.copy(pdfPath = null, updatedAt = System.currentTimeMillis()))
    }

    // ---------- Import de fichiers ----------

    private fun queryName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    private fun pdfDir(): File = File(context.filesDir, "pdfs").apply { mkdirs() }

    /** Copie un PDF dans le stockage privé de l'appli et renvoie son chemin. */
    suspend fun copyPdf(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val name = (queryName(uri) ?: "partition.pdf").replace(Regex("[^A-Za-z0-9._-]"), "_")
            val dest = File(pdfDir(), "${System.currentTimeMillis()}_$name")
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            input.use { src -> dest.outputStream().use { out -> src.copyTo(out) } }
            dest.absolutePath
        }.getOrNull()
    }

    /** Importe un fichier ChordPro / texte / PDF. Renvoie l'id du morceau créé ou null en cas d'échec. */
    suspend fun importFile(uri: Uri): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val name = queryName(uri) ?: "Morceau"
            val mime = context.contentResolver.getType(uri).orEmpty()
            val baseName = name.substringBeforeLast('.')
            if (mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)) {
                val path = copyPdf(uri) ?: return@runCatching null
                songDao.insert(Song(title = baseName, pdfPath = path))
            } else {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: return@runCatching null
                val imp = ChordPro.importText(text, baseName)
                songDao.insert(
                    Song(
                        title = imp.title, artist = imp.artist, songKey = imp.key, capo = imp.capo,
                        tempo = imp.tempo, timeSig = imp.timeSig, content = imp.content
                    )
                )
            }
        }.getOrNull()
    }

    fun exportText(song: Song): String =
        ChordPro.export(song.title, song.artist, song.songKey, song.capo, song.tempo, song.timeSig, song.content)

    // ---------- Sauvegarde / restauration (JSON) ----------

    suspend fun exportBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("version", 1)
            val songsJson = JSONArray()
            for (s in songDao.all()) {
                songsJson.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("title", s.title)
                        .put("artist", s.artist)
                        .put("key", s.songKey)
                        .put("tempo", s.tempo)
                        .put("timeSig", s.timeSig)
                        .put("capo", s.capo)
                        .put("transpose", s.transpose)
                        .put("scrollSpeed", s.scrollSpeed)
                        .put("content", s.content)
                        .put("tags", s.tags)
                        .put("favorite", s.favorite)
                        .put("notes", s.notes)
                )
            }
            root.put("songs", songsJson)
            val setsJson = JSONArray()
            for (sl in setDao.allSetLists()) {
                val ids = JSONArray()
                setDao.entries(sl.id).forEach { ids.put(it.songId) }
                setsJson.put(JSONObject().put("name", sl.name).put("songs", ids))
            }
            root.put("setlists", setsJson)
            val out = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
            out.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            true
        }.getOrDefault(false)
    }

    /** Restaure une sauvegarde en ajoutant les morceaux et sets (rien n'est écrasé). */
    suspend fun importBackup(uri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: return@runCatching 0
            val root = JSONObject(text)
            val idMap = HashMap<Long, Long>()
            val songsJson = root.optJSONArray("songs") ?: JSONArray()
            for (i in 0 until songsJson.length()) {
                val o = songsJson.getJSONObject(i)
                val newId = songDao.insert(
                    Song(
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        songKey = o.optString("key"),
                        tempo = o.optInt("tempo"),
                        timeSig = o.optString("timeSig", "4/4"),
                        capo = o.optInt("capo"),
                        transpose = o.optInt("transpose"),
                        scrollSpeed = o.optInt("scrollSpeed", 6),
                        content = o.optString("content"),
                        tags = o.optString("tags"),
                        favorite = o.optBoolean("favorite"),
                        notes = o.optString("notes")
                    )
                )
                idMap[o.optLong("id")] = newId
            }
            val setsJson = root.optJSONArray("setlists") ?: JSONArray()
            for (i in 0 until setsJson.length()) {
                val o = setsJson.getJSONObject(i)
                val setId = setDao.insert(SetList(name = o.optString("name", "Set")))
                val ids = o.optJSONArray("songs") ?: JSONArray()
                val mapped = (0 until ids.length()).mapNotNull { idMap[ids.getLong(it)] }
                if (mapped.isNotEmpty()) addToSetList(setId, mapped)
            }
            songsJson.length()
        }.getOrDefault(-1)
    }
}
