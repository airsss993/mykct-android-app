package ru.dzhaparidze.mykct.data.net

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/** В релизе — только настоящая сеть. Заглушка живёт в debug-варианте этого файла. */
internal fun httpEngine(): HttpClientEngine = OkHttp.create()
