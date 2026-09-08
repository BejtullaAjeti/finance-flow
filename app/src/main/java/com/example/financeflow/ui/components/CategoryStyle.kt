package com.example.financeflow.ui.components

import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Airplane
import com.adamglin.phosphoricons.regular.Barbell
import com.adamglin.phosphoricons.regular.Briefcase
import com.adamglin.phosphoricons.regular.Car
import com.adamglin.phosphoricons.regular.Coffee
import com.adamglin.phosphoricons.regular.FilmSlate
import com.adamglin.phosphoricons.regular.FirstAid
import com.adamglin.phosphoricons.regular.ForkKnife
import com.adamglin.phosphoricons.regular.GraduationCap
import com.adamglin.phosphoricons.regular.House
import com.adamglin.phosphoricons.regular.Money
import com.adamglin.phosphoricons.regular.PawPrint
import com.adamglin.phosphoricons.regular.Receipt
import com.adamglin.phosphoricons.regular.ShoppingCart
import com.adamglin.phosphoricons.regular.Tag
import com.adamglin.phosphoricons.regular.User
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.financeflow.R
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryType
import com.example.financeflow.data.TransactionType

object CategoryIcons {
    val Catalog: List<Pair<String, ImageVector>> = listOf(
        "food" to PhosphorIcons.Regular.ForkKnife,
        "car" to PhosphorIcons.Regular.Car,
        "home" to PhosphorIcons.Regular.House,
        "shopping" to PhosphorIcons.Regular.ShoppingCart,
        "health" to PhosphorIcons.Regular.FirstAid,
        "school" to PhosphorIcons.Regular.GraduationCap,
        "travel" to PhosphorIcons.Regular.Airplane,
        "movie" to PhosphorIcons.Regular.FilmSlate,
        "bill" to PhosphorIcons.Regular.Receipt,
        "work" to PhosphorIcons.Regular.Briefcase,
        "income" to PhosphorIcons.Regular.Money,
        "coffee" to PhosphorIcons.Regular.Coffee,
        "pets" to PhosphorIcons.Regular.PawPrint,
        "fitness" to PhosphorIcons.Regular.Barbell
    )

    private val byKey = Catalog.toMap()
    val Fallback: ImageVector = PhosphorIcons.Regular.Tag

    fun resolve(key: String?): ImageVector = byKey[key] ?: Fallback
}

val CategoryColorPalette = listOf(
    "#7FB88F", "#D9825F", "#C9A24B", "#6B9BD1",
    "#B07CC6", "#E0A458", "#5FA9A8", "#D16C8F",
    "#C77B7B", "#8FA85F"
)

@Composable
fun String?.toCategoryColor(): Color =
    this?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: IllegalArgumentException) {
            null
        }
    } ?: MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun categoryTypeLabel(type: CategoryType): String = when (type) {
    CategoryType.PERSONAL -> stringResource(R.string.type_personal)
    CategoryType.BUSINESS -> stringResource(R.string.type_business)
    CategoryType.BOTH -> stringResource(R.string.type_both)
}

/**
 * The name to show for [Category] — resolved through the current locale for a seeded default
 * (non-null [Category.nameKey]) instead of its frozen-at-seed-time [Category.name], and shown
 * verbatim for a user-created category (nameKey null), whose name is their own words and must
 * never be run through a resource lookup. Every screen that displays a category name must call
 * this instead of reading `.name` directly, or it silently reintroduces the "seeded categories
 * don't re-translate" bug this exists to fix.
 */
@Composable
fun Category.displayName(): String = when (nameKey) {
    "food" -> stringResource(R.string.category_default_food)
    "transport" -> stringResource(R.string.category_default_transport)
    "bills" -> stringResource(R.string.category_default_bills)
    "salary" -> stringResource(R.string.category_default_salary)
    "business_income" -> stringResource(R.string.category_default_business_income)
    "business_expenses" -> stringResource(R.string.category_default_business_expenses)
    "others" -> stringResource(R.string.category_default_others)
    else -> name
}

// Same Personal/Business icons as TransactionTypeToggle, reused here as a small in-Combined-mode
// marker on category cards and transaction rows. Null for BOTH categories — they aren't one or
// the other, so no single icon would be accurate.
fun CategoryType.indicatorIcon(): ImageVector? = when (this) {
    CategoryType.PERSONAL -> PhosphorIcons.Regular.User
    CategoryType.BUSINESS -> PhosphorIcons.Regular.Briefcase
    CategoryType.BOTH -> null
}

fun TransactionType.indicatorIcon(): ImageVector = when (this) {
    TransactionType.PERSONAL -> PhosphorIcons.Regular.User
    TransactionType.BUSINESS -> PhosphorIcons.Regular.Briefcase
}

@Composable
fun TypeIndicatorIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(14.dp)
    )
}
