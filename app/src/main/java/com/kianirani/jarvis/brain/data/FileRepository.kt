package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.server.BrainException
import java.io.File

class FileRepository(private val root: File) {
    private fun resolve(rel: String): File {
        val f = File(root, rel).canonicalFile
        if (!f.path.startsWith(root.canonicalFile.path)) throw BrainException.validation("path escapes storage root")
        return f
    }
    fun read(rel: String): String {
        val f = resolve(rel)
        if (!f.exists()) throw BrainException.notFound("file $rel")
        return f.readText()
    }
    fun write(rel: String, content: String) {
        val f = resolve(rel); f.parentFile?.mkdirs(); f.writeText(content)
    }
    fun list(rel: String): List<String> {
        val dir = resolve(rel)
        if (!dir.isDirectory) return emptyList()
        return dir.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(root).path.replace(File.separatorChar, '/') }.sorted().toList()
    }
}
