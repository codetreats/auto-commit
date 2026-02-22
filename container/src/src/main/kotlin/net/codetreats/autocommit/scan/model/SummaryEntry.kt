
package net.codetreats.autocommit.scan.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SummaryEntry(
    val category: String,
    val repo: String,
    val status: String,
    val changesCount: Int,
    val jsonFile: String,
    val suggestedCommitMessage: String? = null
) {
    fun toJson(): String {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(SummaryEntry::class.java).indent("  ")
        return adapter.toJson(this)
    }
}

fun List<SummaryEntry>.toJson(): String {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val adapter = moshi.adapter(List::class.java).indent("  ")
    return adapter.toJson(this)
}
