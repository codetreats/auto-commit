package net.codetreats.autocommit.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class Commits(val commits: List<Commit>) {
    fun toJson(): String = adapter.toJson(this)

    companion object {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(Commits::class.java)

        fun from(json: String): Commits = adapter.fromJson(json)!!
    }
}

data class Commit(
    val category: String,
    val repo: String,
    val message: String?
) {
    fun exists(): Boolean = !message.isNullOrBlank()

    fun message() = message!!
}