package ru.dzhaparidze.mykct.data

import android.content.Context

/** Что выбрал студент — это же параметры GET /api/v1/schedule, кроме дат. null = «все». */
data class Selection(
    val group: String = Groups.all.first(),
    val subgroup: String? = null,
    val englishGroup: String? = null,
    val profileSubgroup: String? = null,
) {
    /** Год набора у групп разный, поэтому смена группы обнуляет остальной выбор. */
    fun withGroup(group: String) = Selection(group)

    fun withSubgroup(subgroup: String?) = copy(
        subgroup = subgroup,
        profileSubgroup = profileSubgroup.takeIf { Groups.profileSubgroups(group, subgroup).isNotEmpty() },
    )
}

/** Выбор переживает перезапуск. SharedPreferences: четыре строки, DataStore тут не за что. */
class SelectionStore(context: Context) {

    private val prefs = context.getSharedPreferences("selection", Context.MODE_PRIVATE)

    fun load() = Selection(
        // группа могла выпуститься и исчезнуть из справочника — тогда дефолт
        group = prefs.getString(GROUP, null)?.takeIf { it in Groups.all } ?: Selection().group,
        subgroup = prefs.getString(SUBGROUP, null),
        englishGroup = prefs.getString(ENGLISH, null),
        profileSubgroup = prefs.getString(PROFILE_SUBGROUP, null),
    )

    fun save(selection: Selection) = prefs.edit()
        .putString(GROUP, selection.group)
        .putString(SUBGROUP, selection.subgroup)
        .putString(ENGLISH, selection.englishGroup)
        .putString(PROFILE_SUBGROUP, selection.profileSubgroup)
        .apply()

    private companion object {
        const val GROUP = "group"
        const val SUBGROUP = "subgroup"
        const val ENGLISH = "english_group"
        const val PROFILE_SUBGROUP = "profile_subgroup"
    }
}
