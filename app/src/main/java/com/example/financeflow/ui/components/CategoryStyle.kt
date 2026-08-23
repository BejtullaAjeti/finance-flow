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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.financeflow.R
import com.example.financeflow.data.CategoryType

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
