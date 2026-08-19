package ru.dzhaparidze.mykct.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.dzhaparidze.mykct.data.auth.User

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

/**
 * Выбор из профиля студента — «использовать мою группу». Правила те же, что в iOS:
 * у первокурсников деление на «Подгр», у старших курсов подгруппа — это профиль,
 * а «Подгр» у них попадает в profile_subgroup (только для FE и CD).
 * null, если группы студента нет в справочнике — подставлять чужую группу хуже, чем ничего.
 */
fun selectionOf(user: User): Selection? {
    val group = user.academicGroup?.takeIf { it in Groups.all } ?: return null
    val firstYear = Groups.subgroups(group).any { it.id.startsWith("Подгр") }
    val subgroup = (if (firstYear) user.subgroup else user.profile)
        ?.takeIf { id -> Groups.subgroups(group).any { it.id == id } }
    val profileSubgroup = user.subgroup
        ?.takeIf { id -> Groups.profileSubgroups(group, subgroup).any { it.id == id } }
    return Selection(
        group = group,
        subgroup = subgroup,
        englishGroup = user.englishGroup?.takeIf { it in Groups.englishGroups(group) },
        profileSubgroup = profileSubgroup,
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

    fun save(selection: Selection) {
        write(selection)
        changed.tryEmit(selection)
    }

    private fun write(selection: Selection) = prefs.edit()
        .putString(GROUP, selection.group)
        .putString(SUBGROUP, selection.subgroup)
        .putString(ENGLISH, selection.englishGroup)
        .putString(PROFILE_SUBGROUP, selection.profileSubgroup)
        .apply()

    companion object {
        /**
         * Выбор правят из двух мест: шит групп на расписании и «использовать мою группу»
         * в настройках. ViewModel расписания живёт дольше обоих экранов и сама бы про
         * чужую правку не узнала — поэтому изменения приезжают сюда.
         */
        val changed = MutableSharedFlow<Selection>(extraBufferCapacity = 1)

        private const val GROUP = "group"
        private const val SUBGROUP = "subgroup"
        private const val ENGLISH = "english_group"
        private const val PROFILE_SUBGROUP = "profile_subgroup"
    }
}
