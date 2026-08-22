package com.example.financeflow.ui.theme

import androidx.compose.ui.graphics.Color

// Backgrounds — deep muted slate-navy rather than flat black, so glass cards have somewhere to float
val Backdrop = Color(0xFF0B0E14)
val Surface = Color(0xFF1B212B)
val SurfaceVariant = Color(0xFF232A36)

// Text
val OnBackground = Color(0xFFEDE8DD)
val OnSurfaceMuted = Color(0xFF9AA2B1)

// Accent — muted gold reads as "value" without doubling as an income/expense signal
val Accent = Color(0xFFC9A24B)
val OnAccent = Color(0xFF1B140A)

// Money semantics — muted sage vs muted terracotta rather than stoplight red/green
val Income = Color(0xFF7FB88F)
val IncomeContainer = Color(0xFF20302A)
val Expense = Color(0xFFD9825F)
val ExpenseContainer = Color(0xFF352420)

// Glassmorphism tokens — semi-transparent fill, a brighter top edge to fake light catching glass,
// and a soft dark shadow to lift the card off the backdrop
val GlassFill = Color(0x24FFFFFF)
val GlassGlow = Color(0x33FFFFFF)
val GlassBorderTop = Color(0x5CFFFFFF)
val GlassBorderBottom = Color(0x0DFFFFFF)
val GlassShadow = Color(0x73000000)
