package com.lifemate.ui

import androidx.compose.runtime.Composable
import com.lifemate.database.LifeItem

@Composable fun WishScreen(item: LifeItem, vm: LifeViewModel) { CardStudioScreen(vm, item) }
