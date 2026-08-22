package com.example.financeflow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.financeflow.R
import com.example.financeflow.data.CategoryType
import com.example.financeflow.ui.theme.OnSurfaceMuted

object CategoryIcons {
    val Catalog: List<Pair<String, ImageVector>> = listOf(
        "food" to Icons.Rounded.Fastfood,
        "car" to Icons.Rounded.DirectionsCar,
        "home" to Icons.Rounded.Home,
        "shopping" to Icons.Rounded.ShoppingCart,
        "health" to Icons.Rounded.LocalHospital,
        "school" to Icons.Rounded.School,
        "travel" to Icons.Rounded.Flight,
        "movie" to Icons.Rounded.Movie,
        "bill" to Icons.Rounded.Receipt,
        "work" to Icons.Rounded.Work,
        "income" to Icons.Rounded.AttachMoney,
        "coffee" to Icons.Rounded.Coffee,
        "pets" to Icons.Rounded.Pets,
        "fitness" to Icons.Rounded.FitnessCenter
    )

    private val byKey = Catalog.toMap()
    val Fallback: ImageVector = Icons.Rounded.Category

    fun resolve(key: String?): ImageVector = byKey[key] ?: Fallback
}

val CategoryColorPalette = listOf(
    "#7FB88F", "#D9825F", "#C9A24B", "#6B9BD1",
    "#B07CC6", "#E0A458", "#5FA9A8", "#D16C8F"
)

fun String?.toCategoryColor(): Color =
    this?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: IllegalArgumentException) {
            null
        }
    } ?: OnSurfaceMuted

@Composable
fun categoryTypeLabel(type: CategoryType): String = when (type) {
    CategoryType.PERSONAL -> stringResource(R.string.type_personal)
    CategoryType.BUSINESS -> stringResource(R.string.type_business)
    CategoryType.BOTH -> stringResource(R.string.type_both)
}
