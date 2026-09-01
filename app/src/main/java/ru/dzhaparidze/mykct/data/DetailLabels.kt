package ru.dzhaparidze.mykct.data

/**
 * Подписи к деталям пары. Схемы у /classdetails нет (см. `flattenDetails`), ключи
 * приходят как есть — портальные и английские. Известные переводятся и встают в свой
 * порядок, незнакомый показывается как пришёл: он всё равно полезнее пустоты.
 */
private val KNOWN = listOf(
    "teacher" to "Преподаватель",
    "topicdescr" to "Тема",
    "topictitle" to "Занятие",
    "descr" to "Описание",
    "building" to "Корпус",
    "comment" to "Комментарий",
)

fun detailLabel(key: String): String =
    KNOWN.firstOrNull { it.first == key.lowercase() }?.second ?: key

/** Знакомые ключи вперёд, остальные — в порядке ответа (sortedBy стабильна). */
fun List<Pair<String, String>>.sortedByLabel(): List<Pair<String, String>> =
    sortedBy { (key, _) ->
        KNOWN.indexOfFirst { it.first == key.lowercase() }.takeIf { it >= 0 } ?: KNOWN.size
    }
