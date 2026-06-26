package com.example.expncetracker.exptkr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expncetracker.exptkr.core.common.formatAsCurrency
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import java.math.BigDecimal

@Composable
fun DistributionSection(
    distribution: Map<String, BigDecimal>,
    allCategories: List<CategoryEntity>,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    showCard: Boolean = true,
    onCategoryClick: ((String) -> Unit)? = null
) {
    val distributionData = remember(distribution, allCategories) {
        allCategories.mapNotNull { entity ->
            val amount = distribution[entity.name] ?: return@mapNotNull null
            if (amount <= BigDecimal.ZERO) return@mapNotNull null

            DistributionItem(
                name = entity.name,
                amount = amount,
                iconName = entity.iconName,
                color = Color(entity.color)
            )
        }.sortedByDescending { it.amount }
    }

    if (distributionData.isEmpty()) return

    val maxAmount = remember(distributionData) {
        distributionData.maxOf { it.amount }
            .coerceAtLeast(BigDecimal.valueOf(1.0))
    }

    Column(modifier = modifier) {

        if (showTitle) {
            Text(
                text = "Spending Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))
        }

        if (showCard) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                DistributionList(
                    distributionData = distributionData,
                    maxAmount = maxAmount,
                    onCategoryClick = onCategoryClick
                )
            }
        } else {
            DistributionList(
                distributionData = distributionData,
                maxAmount = maxAmount,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Composable
private fun DistributionList(
    distributionData: List<DistributionItem>,
    maxAmount: BigDecimal,
    onCategoryClick: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        distributionData.forEach { item ->

            val progress =
                (item.amount.toDouble() / maxAmount.toDouble())
                    .toFloat()
                    .coerceIn(0f, 1f)

            // Guarantees text visibility
            val minBarWidth = 180.dp
            val extraWidth = 180.dp

            val barWidth =
                minBarWidth + (extraWidth * progress)

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(18.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = onCategoryClick != null) {
                        onCategoryClick?.invoke(item.name)
                    }
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.15f)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = MaterialTheme.shapes.small
                    )
            ) {

                // Main liquid color layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    item.color.copy(alpha = 0.95f),
                                    item.color.copy(alpha = 0.75f),
                                    item.color.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // Glass highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom shadow
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.12f)
                                )
                            )
                        )
                )

                // Content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = item.amount.formatAsCurrency(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

private data class DistributionItem(
    val name: String,
    val amount: BigDecimal,
    val iconName: String,
    val color: Color
)
