package com.wordlock.app

data class Word(
    val picture: String,
    val english: String,
    val korean: String,
    val alternatives: List<String> = emptyList(),
) {
    /** All accepted answers, letters only (spaces removed), lowercase. */
    val accepted: List<String> get() = (listOf(english) + alternatives).map { it.lettersOnly() }
    val letters: String get() = english.lettersOnly()
}

fun String.lettersOnly() = lowercase().replace(" ", "")

enum class Level(val label: String, val help: String) {
    EASY("쉬움", "3–5글자 · 일상 사물과 동물"),
    MID("보통", "6–8글자 · 조금 긴 단어"),
    HARD("어려움", "8글자 이상 · 과학, 자연, 두 단어"),
}

object Words {
    val easy = listOf(
        Word("🍎", "apple", "사과"), Word("🐶", "dog", "개"), Word("🐱", "cat", "고양이"),
        Word("🚗", "car", "자동차"), Word("☀️", "sun", "태양"), Word("🌙", "moon", "달"),
        Word("🐟", "fish", "물고기"), Word("🏠", "house", "집", listOf("home")),
        Word("🔑", "key", "열쇠"), Word("📚", "book", "책", listOf("books")),
        Word("⚽", "ball", "공"), Word("🐻", "bear", "곰"), Word("🍰", "cake", "케이크"),
        Word("🦆", "duck", "오리"), Word("🌳", "tree", "나무"),
        Word("👟", "shoe", "신발", listOf("shoes", "sneaker")), Word("🐸", "frog", "개구리"),
        Word("🥚", "egg", "달걀"), Word("🦉", "owl", "부엉이"),
    )
    val mid = listOf(
        Word("🦒", "giraffe", "기린"), Word("🐧", "penguin", "펭귄"),
        Word("🚲", "bicycle", "자전거", listOf("bike")), Word("🌂", "umbrella", "우산"),
        Word("🎸", "guitar", "기타"), Word("🐢", "turtle", "거북이", listOf("tortoise")),
        Word("✈️", "airplane", "비행기", listOf("plane")), Word("🧲", "magnet", "자석"),
        Word("🦋", "butterfly", "나비"), Word("🥕", "carrot", "당근"),
        Word("🍌", "banana", "바나나"), Word("🐘", "elephant", "코끼리"),
        Word("🍉", "watermelon", "수박"),
    )
    val hard = listOf(
        Word("🔭", "telescope", "망원경"), Word("🌋", "volcano", "화산"),
        Word("🦖", "dinosaur", "공룡"), Word("🧭", "compass", "나침반"),
        Word("🚁", "helicopter", "헬리콥터"), Word("🐙", "octopus", "문어"),
        Word("🦔", "hedgehog", "고슴도치"), Word("🥦", "broccoli", "브로콜리"),
        Word("🔬", "microscope", "현미경"), Word("🎡", "ferris wheel", "대관람차"),
        Word("🍓", "strawberry", "딸기"), Word("🦩", "flamingo", "플라밍고"),
    )

    fun of(level: Level) = when (level) {
        Level.EASY -> easy
        Level.MID -> mid
        Level.HARD -> hard
    }

    fun random(level: Level, except: Word? = null): Word {
        val list = of(level)
        if (list.size < 2) return list.first()
        var w: Word
        do { w = list.random() } while (w == except)
        return w
    }
}
