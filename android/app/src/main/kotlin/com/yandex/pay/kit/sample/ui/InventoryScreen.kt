package com.yandex.pay.kit.sample.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.pay.inventory.api.badge.YPayBadgeView
import com.yandex.pay.inventory.api.badge.renderdata.BadgeRenderData
import com.yandex.pay.inventory.api.badge.renderdata.bnpl.BnplBadgeColor
import com.yandex.pay.inventory.api.badge.renderdata.bnpl.BnplBadgeVariant
import com.yandex.pay.inventory.api.badge.renderdata.cashback.CashbackBadgeColor
import com.yandex.pay.inventory.api.badge.renderdata.cashback.CashbackBadgeVariant
import com.yandex.pay.inventory.api.badge.renderdata.common.BadgeAlign
import com.yandex.pay.inventory.api.badge.renderdata.common.BadgeTheme
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InventoryScreen(onBack: () -> Unit) {
    var previewHeight by remember { mutableIntStateOf(24) }
    var amount by remember { mutableStateOf("1000") }
    var align by remember { mutableStateOf(BadgeAlign.CENTER) }

    var cashbackColor by remember { mutableStateOf(CashbackBadgeColor.PRIMARY) }
    var cashbackVariant by remember { mutableStateOf(CashbackBadgeVariant.DETAILED) }

    var bnplColor by remember { mutableStateOf(BnplBadgeColor.PRIMARY) }
    var bnplVariant by remember { mutableStateOf(BnplBadgeVariant.SIMPLE) }

    val cashbackRenderData = remember(align, cashbackColor, cashbackVariant) {
        BadgeRenderData.Cashback(
            align = align,
            color = cashbackColor,
            theme = BadgeTheme.SYSTEM,
            variant = cashbackVariant,
        )
    }

    val bnplRenderData = remember(align, bnplColor, bnplVariant) {
        BadgeRenderData.Bnpl(
            align = align,
            color = bnplColor,
            theme = BadgeTheme.SYSTEM,
            variant = bnplVariant,
        )
    }

    val amountDecimal = remember(amount) {
        runCatching { BigDecimal(amount) }.getOrDefault(BigDecimal("1000"))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inventory (Badges)",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewSection(
                previewHeight = previewHeight,
                amount = amountDecimal,
                cashbackRenderData = cashbackRenderData,
                bnplRenderData = bnplRenderData,
            )

            GeneralSection(
                amount = amount,
                onAmountChange = { amount = it },
                align = align,
                onAlignChange = { align = it },
            )

            CashbackBadgeSection(
                color = cashbackColor,
                onColorChange = { cashbackColor = it },
                variant = cashbackVariant,
                onVariantChange = { cashbackVariant = it },
            )

            SplitBadgeSection(
                color = bnplColor,
                onColorChange = { bnplColor = it },
                variant = bnplVariant,
                onVariantChange = { bnplVariant = it },
            )

            PreviewSizeSection(
                previewHeight = previewHeight,
                onHeightChange = { previewHeight = it },
            )
        }
    }
}

@Composable
private fun PreviewSection(
    previewHeight: Int,
    amount: BigDecimal,
    cashbackRenderData: BadgeRenderData,
    bnplRenderData: BadgeRenderData,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Preview",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BadgeViewHost(
                    renderData = cashbackRenderData,
                    amount = amount,
                    heightDp = previewHeight,
                )
                BadgeViewHost(
                    renderData = bnplRenderData,
                    amount = amount,
                    heightDp = previewHeight,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewSizeSection(
    previewHeight: Int,
    onHeightChange: (Int) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(previewHeight.toFloat()) }

    SectionCard(title = "Preview Size") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Height",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onHeightChange(it.toInt())
                },
                valueRange = 16f..48f,
                steps = 31,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            )
            Text(
                text = "$previewHeight",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    align: BadgeAlign,
    onAlignChange: (BadgeAlign) -> Unit,
) {
    SectionCard(title = "General") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { new -> onAmountChange(new.filter(Char::isDigit)) },
                label = { Text("Amount (RUB)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
            DropdownPicker(
                label = "Align",
                options = BadgeAlign.entries,
                selected = align,
                onSelect = onAlignChange,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashbackBadgeSection(
    modifier: Modifier = Modifier,
    color: CashbackBadgeColor,
    onColorChange: (CashbackBadgeColor) -> Unit,
    variant: CashbackBadgeVariant,
    onVariantChange: (CashbackBadgeVariant) -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "Cashback Badge",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownPicker(
                label = "Color",
                options = CashbackBadgeColor.entries,
                selected = color,
                onSelect = onColorChange,
            )
            DropdownPicker(
                label = "Variant",
                options = CashbackBadgeVariant.entries,
                selected = variant,
                onSelect = onVariantChange,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitBadgeSection(
    modifier: Modifier = Modifier,
    color: BnplBadgeColor,
    onColorChange: (BnplBadgeColor) -> Unit,
    variant: BnplBadgeVariant,
    onVariantChange: (BnplBadgeVariant) -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "Split Badge",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownPicker(
                label = "Color",
                options = BnplBadgeColor.entries,
                selected = color,
                onSelect = onColorChange,
            )
            DropdownPicker(
                label = "Variant",
                options = BnplBadgeVariant.entries,
                selected = variant,
                onSelect = onVariantChange,
            )
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    title: String,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (trailingContent != null) {
                    trailingContent()
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> DropdownPicker(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = MaterialTheme.shapes.medium,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BadgeViewHost(
    renderData: BadgeRenderData,
    amount: BigDecimal,
    heightDp: Int,
) {
    AndroidView(
        factory = { ctx ->
            YPayBadgeView(ctx).apply {
                setSum(amount)
                setRenderData(renderData)
            }
        },
        update = { view ->
            view.setSum(amount)
            view.setRenderData(renderData)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
    )
}
