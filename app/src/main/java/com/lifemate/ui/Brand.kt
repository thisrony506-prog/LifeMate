package com.lifemate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifemate.R

/** Bundled offline brand asset, shared by onboarding, navigation and the launcher. */
@Composable fun BrandMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.lifemate_brand),
        contentDescription = "LifeMate logo",
        modifier = modifier.size(size).clip(RoundedCornerShape(size * .28f)),
        contentScale = ContentScale.Crop
    )
}

@Composable fun BrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BrandMark(30.dp)
        Text("LifeMate", style = MaterialTheme.typography.titleMedium)
    }
}
