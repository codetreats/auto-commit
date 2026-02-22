
package net.codetreats.autocommit.scan.model

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class RepoStatus(
    val status: Status,
    val category: String,
    val repo: String,
    val path: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val changes: List<Change> = emptyList(),
    val suggestedCommitMessage: String? = null
) {
    fun toJson(): String {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(LocalDateTime::class.java, LocalDateTimeAdapter())
            .build()
        val adapter = moshi.adapter(RepoStatus::class.java).indent("  ")
        return adapter.toJson(this)
    }
}

enum class Status {
    UPTODATE, CHANGED, UNTRACKED, UNPUSHED
}

data class Change(
    val path: String,
    val type: ChangeType,
    val oldFile: String? = null,
    val newFile: String? = null
)

enum class ChangeType {
    MODIFIED, ADDED, DELETED
}


class LocalDateTimeAdapter : JsonAdapter<LocalDateTime>() {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @FromJson
    override fun fromJson(reader: JsonReader): LocalDateTime? {
        return LocalDateTime.parse(reader.nextString(), formatter)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, date: LocalDateTime?) {
        writer.value(date!!.format(formatter))
    }
}

