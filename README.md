<div align="center">

<img src="screenshots/promo.jpg" alt="МойКЦТ" width="820"/>

# МойКЦТ

**Расписание, посещаемость и оценки Колледжа Цифровых Технологий — в одном приложении**

[![RuStore](https://img.shields.io/badge/RuStore-установить-0077FF?style=for-the-badge)](https://www.rustore.ru/catalog/app/ru.dzhaparidze.collegeapp)
[![Google Play](https://img.shields.io/badge/Google_Play-установить-34A853?style=for-the-badge&logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=ru.dzhaparidze.collegeapp)

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

</div>

---

## Что умеет

| | |
|---|---|
| **Расписание** | Неделя целиком, три дня или один — на выбор. Пары нарисованы на временной сетке: высота карточки равна длительности, идущая пара подсвечена и показывает, сколько осталось |
| **Своя группа** | Группа, подгруппа, профиль и английская группа — расписание сразу подстраивается под конкретного студента |
| **Детали пары** | Тема, преподаватель, кабинет и подгруппы — по нажатию на карточку |
| **Посещаемость** | Кольцо за неделю, разбивка «был / уважительная / неуважительная» и список пар по дням |
| **Стрик** | Счётчик недель без пропусков — прямо в шапке расписания |
| **Успеваемость** | Оценки по предметам за выбранный период |
| **Темы** | Светлая, тёмная и системная. Свой шрифт, свои иконки предметов, никакого dynamic color |

## Экраны

<div align="center">
<img src="screenshots/schedule.png" width="260" alt="Расписание"/>
<img src="screenshots/home.png" width="260" alt="Посещаемость"/>
<img src="screenshots/settings.png" width="260" alt="Настройки"/>
</div>

## Стек

- **Kotlin 2.2** + **Jetpack Compose** (Material 3, Compose BOM)
- **MVVM** без DI- и navigation-библиотек: зависимости собираются руками, навигация — на `when` по enum
- **Ktor Client 3.5** + **kotlinx.serialization** для сети
- **core library desugaring** — `java.time` работает начиная с Android 7
- Юнит-тесты на JUnit 4 + `kotlinx-coroutines-test`, сеть в тестах подменяется `MockEngine`

## Лицензия

MIT — см. [LICENSE](LICENSE). Лицензии сторонних библиотек: [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

<div align="center">
<br/>
Сделано для студентов КЦТ
</div>
