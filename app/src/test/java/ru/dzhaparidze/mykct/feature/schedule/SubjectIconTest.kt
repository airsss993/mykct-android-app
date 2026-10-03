package ru.dzhaparidze.mykct.feature.schedule

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.dzhaparidze.mykct.R
import ru.dzhaparidze.mykct.feature.schedule.components.subjectIcon

/**
 * Проверяется не весь справочник, а места, где правила пересекаются словами:
 * именно там перестановка строк молча меняет иконку.
 */
class SubjectIconTest {

    @Test
    fun `предмет узнаётся по ключевому слову`() {
        assertEquals(R.drawable.ic_database, subjectIcon("Базы данных"))
        assertEquals(R.drawable.ic_network, subjectIcon("Компьютерные сети"))
        assertEquals(R.drawable.ic_terminal, subjectIcon("Операционные системы и среды"))
        assertEquals(R.drawable.ic_code, subjectIcon("Разработка модулей ПО"))
    }

    @Test
    fun `частное правило стоит раньше общего`() {
        // «физическая» есть и в физкультуре, и в физике
        assertEquals(R.drawable.ic_fitness, subjectIcon("Физическая культура"))
        assertEquals(R.drawable.ic_science, subjectIcon("Физика"))
        // «язык» есть и у русского, и у иностранного, и у программирования
        assertEquals(R.drawable.ic_book, subjectIcon("Русский язык"))
        assertEquals(R.drawable.ic_translate, subjectIcon("Иностранный язык"))
        assertEquals(R.drawable.ic_code, subjectIcon("Языки программирования"))
        // «математическая статистика» — статистика, «дискретная математика» — алгоритмы
        assertEquals(R.drawable.ic_statistics, subjectIcon("Теория вероятностей и математическая статистика"))
        assertEquals(R.drawable.ic_algorithm, subjectIcon("Дискретная математика"))
        assertEquals(R.drawable.ic_math, subjectIcon("Математика"))
    }

    @Test
    fun `подстрока чужого слова не срабатывает`() {
        // «уПРАВление» не должно уходить в правоведение
        assertEquals(R.drawable.ic_assignment, subjectIcon("Управление проектами"))
        assertEquals(R.drawable.ic_law, subjectIcon("Правовое обеспечение профессиональной деятельности"))
    }

    @Test
    fun `сокращения портала берутся из справочника точных названий`() {
        // ни одно ключевое слово их не ловит — без справочника все были бы ic_school
        assertEquals(R.drawable.ic_algorithm, subjectIcon("АиСД"))
        assertEquals(R.drawable.ic_assignment, subjectIcon("ОКРиУП"))
        assertEquals(R.drawable.ic_bug, subjectIcon(" РевьюКодаGD "))
    }

    @Test
    fun `профильный вариант и другое написание получают иконку предмета`() {
        assertEquals(R.drawable.ic_bug, subjectIcon("ТестUI-FE"))
        assertEquals(R.drawable.ic_design, subjectIcon("ИПИР-UI"))
        assertEquals(R.drawable.ic_code, subjectIcon("УчПроект.PM"))
        assertEquals(R.drawable.ic_network, subjectIcon("МикросервисыFE"))
        assertEquals(R.drawable.ic_algorithm, subjectIcon("АиСД-2BE"))
        assertEquals(R.drawable.ic_practice, subjectIcon("УчПракт.UI"))
        assertEquals(R.drawable.ic_economics, subjectIcon("ИгроМаркетинг"))
        // ПМ кириллицей - часть названия, не профиль
        assertEquals(R.drawable.ic_economics, subjectIcon("МаркетингПМ"))
    }

    @Test
    fun `UI без разделителя не срезается как профиль`() {
        assertEquals(R.drawable.ic_school, subjectIcon("РазрBE"))
        assertEquals(R.drawable.ic_bug, subjectIcon("ТестUI"))
    }

    @Test
    fun `полное название получает иконку короткого`() {
        assertEquals(R.drawable.ic_math, subjectIcon("Численные методы"))
        assertEquals(R.drawable.ic_algorithm, subjectIcon("Структуры данных"))
        assertEquals(R.drawable.ic_science, subjectIcon("Физическая химия"))
        assertEquals(R.drawable.ic_translate, subjectIcon("Иностранный язык в профессиональной деятельности"))
        assertEquals(R.drawable.ic_design, subjectIcon("Инженерное мышление"))
        assertEquals(R.drawable.ic_algorithm, subjectIcon("Архитектурные паттерны"))
        assertEquals(R.drawable.ic_groups, subjectIcon("Организационное собрание"))
        assertEquals(R.drawable.ic_safety, subjectIcon("Первая медицинская помощь"))
    }

    @Test
    fun `неизвестный предмет получает общую иконку`() {
        assertEquals(R.drawable.ic_school, subjectIcon("Пара"))
    }
}
