package com.quicklist.app.data

import androidx.annotation.StringRes
import com.quicklist.app.R

/** Shopping categories with simple keyword auto-detection (English + Russian). */
enum class Category(@StringRes val label: Int, val emoji: String, val keywords: List<String>) {
    OTHER(R.string.cat_other, "📦", emptyList()),
    PRODUCE(
        R.string.cat_produce, "🥦",
        listOf(
            "apple", "banana", "tomato", "potato", "onion", "carrot", "cucumber", "lemon", "orange", "salad",
            "lettuce", "garlic", "pepper", "fruit", "vegetable", "berr", "grape", "cabbage", "avocado", "herb",
            "яблок", "банан", "помидор", "томат", "картоф", "лук", "морков", "огур", "лимон", "апельсин",
            "салат", "чеснок", "перец", "фрукт", "овощ", "ягод", "зелен", "капуст", "виноград", "авокадо"
        )
    ),
    DAIRY(
        R.string.cat_dairy, "🥛",
        listOf(
            "milk", "cheese", "yogurt", "yoghurt", "butter", "cream", "egg", "kefir",
            "молок", "сыр", "йогурт", "сливочн", "сливк", "яйц", "кефир", "творог", "сметан", "ряженк"
        )
    ),
    MEAT(
        R.string.cat_meat, "🥩",
        listOf(
            "meat", "chicken", "beef", "pork", "fish", "sausage", "ham", "bacon", "salmon", "turkey", "shrimp",
            "мяс", "куриц", "курин", "говядин", "свинин", "рыб", "колбас", "ветчин", "бекон", "лосос", "фарш",
            "индейк", "креветк", "сосиск"
        )
    ),
    BAKERY(
        R.string.cat_bakery, "🍞",
        listOf(
            "bread", "bun", "baguette", "croissant", "cake", "flour", "toast",
            "хлеб", "булк", "батон", "багет", "круассан", "торт", "мук", "лаваш", "тост"
        )
    ),
    PANTRY(
        R.string.cat_pantry, "🍝",
        listOf(
            "rice", "pasta", "cereal", "oil", "sugar", "salt", "coffee", "tea", "noodle", "honey", "oat", "beans",
            "рис", "макарон", "круп", "греч", "масло", "сахар", "соль", "кофе", "чай", "консерв", "мёд", "мед",
            "овсян", "фасол", "спагетти"
        )
    ),
    DRINKS(
        R.string.cat_drinks, "🥤",
        listOf(
            "water", "juice", "beer", "wine", "cola", "soda", "lemonade",
            "вод", "сок", "пиво", "вино", "кола", "лимонад", "минералк"
        )
    ),
    SNACKS(
        R.string.cat_snacks, "🍫",
        listOf(
            "chocolate", "candy", "chips", "cookie", "ice", "snack", "nuts",
            "шоколад", "конфет", "чипс", "печень", "мороженое", "орех", "сухарик", "вафл"
        )
    ),
    HOUSEHOLD(
        R.string.cat_household, "🧴",
        listOf(
            "soap", "shampoo", "toothpaste", "paper", "detergent", "sponge", "napkin", "towel", "battery",
            "мыло", "шампун", "зубн", "бумаг", "порошок", "средств", "губк", "салфет", "полотенц", "батарей",
            "гель", "пакеты"
        )
    );

    companion object {
        fun fromOrdinal(i: Int): Category = entries.getOrElse(i) { OTHER }

        fun detect(title: String): Category {
            val words = title.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
            for (c in entries) {
                if (c.keywords.isEmpty()) continue
                if (words.any { w -> c.keywords.any { k -> w.startsWith(k) } }) return c
            }
            return OTHER
        }
    }
}
