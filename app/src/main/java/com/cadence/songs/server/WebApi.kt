package com.cadence.songs.server

import com.cadence.songs.music.ChordPro
import java.net.URLDecoder

data class SongDto(
    val id: Long = 0,
    val title: String = "",
    val artist: String = "",
    val key: String = "",
    val tempo: Int = 0,
    val timeSig: String = "4/4",
    val capo: Int = 0,
    val tags: String = "",
    val notes: String = "",
    val content: String = "",
    val favorite: Boolean = false,
    val hasPdf: Boolean = false
)

/** Accès aux morceaux pour le serveur Wi-Fi (appels bloquants : le serveur tourne sur ses propres threads). */
interface SongStore {
    fun list(): List<SongDto>
    fun get(id: Long): SongDto?
    /** id = 0 : création. Renvoie l'id du morceau enregistré. */
    fun save(song: SongDto): Long
    fun delete(id: Long)
    fun savePdf(id: Long, fileName: String, bytes: ByteArray): Boolean
    fun readPdf(id: Long): ByteArray?
    fun removePdf(id: Long)
}

/** Routes de la page web « Cadence sur PC ». Protégées par un code à 4 chiffres affiché sur l'appareil. */
class WebApi(private val store: SongStore, private val pin: () -> String) {

    fun handle(req: HttpRequest): HttpResponse {
        val path = req.path.trimEnd('/').ifEmpty { "/" }
        if (req.method == "GET" && (path == "/" || path == "/index.html")) return HttpResponse.html(WebUi.HTML)
        if (!path.startsWith("/api/")) return HttpResponse.text(404, "Introuvable")

        val given = req.headers["x-pin"] ?: req.query["pin"] ?: ""
        if (given != pin()) {
            Thread.sleep(400) // ralentit les essais au hasard
            return error(401, "Code incorrect")
        }
        return try {
            route(req, path)
        } catch (e: Json.JsonException) {
            error(400, "JSON invalide : ${e.message}")
        } catch (e: Exception) {
            error(500, e.message ?: "Erreur")
        }
    }

    private fun route(req: HttpRequest, path: String): HttpResponse {
        val seg = path.removePrefix("/api/").split('/')
        when (seg[0]) {
            "ping" -> return HttpResponse.json(200, """{"ok":true}""")
            "convert" -> if (req.method == "POST") {
                val text = Json.parseObject(req.bodyText())["text"] as? String ?: ""
                val imp = ChordPro.importText(text, "")
                return HttpResponse.json(200, Json.stringify(mapOf("content" to imp.content, "key" to imp.key)))
            }
            "import" -> if (req.method == "POST") {
                val o = Json.parseObject(req.bodyText())
                val text = o["text"] as? String ?: ""
                val fallback = (o["title"] as? String).orEmpty().ifBlank { "Sans titre" }
                val imp = ChordPro.importText(text, fallback)
                val id = store.save(
                    SongDto(
                        title = imp.title, artist = imp.artist, key = imp.key, capo = imp.capo,
                        tempo = imp.tempo, timeSig = imp.timeSig, content = imp.content
                    )
                )
                return HttpResponse.json(201, toJson(store.get(id)!!, withContent = false))
            }
            "songs" -> return songs(req, seg)
        }
        return error(404, "Route inconnue")
    }

    private fun songs(req: HttpRequest, seg: List<String>): HttpResponse {
        if (seg.size == 1) {
            return when (req.method) {
                "GET" -> HttpResponse.json(200, Json.stringify(store.list().map { toMap(it, withContent = false) }))
                "POST" -> {
                    val dto = fromJson(Json.parseObject(req.bodyText()), 0)
                    val id = store.save(dto)
                    HttpResponse.json(201, toJson(store.get(id)!!, withContent = true))
                }
                else -> error(405, "Méthode non permise")
            }
        }
        val id = seg[1].toLongOrNull() ?: return error(400, "Identifiant invalide")
        if (seg.size == 2) {
            return when (req.method) {
                "GET" -> store.get(id)?.let { HttpResponse.json(200, toJson(it, true)) } ?: error(404, "Morceau introuvable")
                "PUT" -> {
                    val old = store.get(id) ?: return error(404, "Morceau introuvable")
                    val dto = fromJson(Json.parseObject(req.bodyText()), id).copy(favorite = old.favorite)
                    store.save(dto)
                    HttpResponse.json(200, toJson(store.get(id)!!, true))
                }
                "DELETE" -> { store.delete(id); HttpResponse.json(200, """{"ok":true}""") }
                else -> error(405, "Méthode non permise")
            }
        }
        if (seg.size == 3 && seg[2] == "pdf") {
            if (store.get(id) == null) return error(404, "Morceau introuvable")
            return when (req.method) {
                "GET" -> {
                    val bytes = store.readPdf(id) ?: return error(404, "Pas de PDF")
                    HttpResponse(200, "application/pdf", bytes, mapOf("Content-Disposition" to "inline; filename=\"partition.pdf\""))
                }
                "POST" -> {
                    val name = req.headers["x-filename"]?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() } ?: "partition.pdf"
                    if (req.body.size < 5 || String(req.body, 0, 5, Charsets.ISO_8859_1) != "%PDF-") {
                        return error(400, "Ce fichier n'est pas un PDF")
                    }
                    if (!store.savePdf(id, name, req.body)) error(500, "Enregistrement impossible")
                    else HttpResponse.json(200, toJson(store.get(id)!!, false))
                }
                "DELETE" -> { store.removePdf(id); HttpResponse.json(200, """{"ok":true}""") }
                else -> error(405, "Méthode non permise")
            }
        }
        return error(404, "Route inconnue")
    }

    private fun error(status: Int, message: String) =
        HttpResponse.json(status, Json.stringify(mapOf("error" to message)))

    private fun toMap(s: SongDto, withContent: Boolean): Map<String, Any?> {
        val m = linkedMapOf<String, Any?>(
            "id" to s.id, "title" to s.title, "artist" to s.artist, "key" to s.key, "tempo" to s.tempo,
            "timeSig" to s.timeSig, "capo" to s.capo, "tags" to s.tags, "notes" to s.notes,
            "favorite" to s.favorite, "hasPdf" to s.hasPdf
        )
        if (withContent) m["content"] = s.content
        return m
    }

    private fun toJson(s: SongDto, withContent: Boolean) = Json.stringify(toMap(s, withContent))

    private fun fromJson(o: Map<String, Any?>, id: Long): SongDto {
        fun str(k: String) = (o[k] as? String).orEmpty()
        fun int(k: String) = (o[k] as? Number)?.toInt() ?: (o[k] as? String)?.toIntOrNull() ?: 0
        return SongDto(
            id = id,
            title = str("title").trim(),
            artist = str("artist").trim(),
            key = str("key").trim(),
            tempo = int("tempo").coerceIn(0, 400),
            timeSig = str("timeSig").trim().ifBlank { "4/4" },
            capo = int("capo").coerceIn(0, 11),
            tags = str("tags").trim(),
            notes = str("notes"),
            content = str("content").replace("\r\n", "\n")
        )
    }
}
