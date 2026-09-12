package com.lifemate.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lifemate.domain.Kind
import java.io.File
import java.time.LocalDate

/** Only visible image cells decode; videos never preload or autoplay in the grid. */
@Composable fun MemoryGridScreen(state: LifeState,navigate: (String)->Unit) {
    var query by rememberSaveable {mutableStateOf("")}
    var sort by rememberSaveable {mutableStateOf("Newest")}
    var filter by rememberSaveable {mutableStateOf("All")}
    val records=remember(state.items,query,filter,sort) {state.items.filter {it.kind==Kind.MEMORY && (if(filter=="Archived") it.archived else !it.archived) && (filter!="Pinned" || it.pinned) && (query.isBlank() || listOf(it.title,it.description,it.notes,it.tags).any {value->value.contains(query,true)})}.let { rows -> when(sort) { "Name"->rows.sortedBy {it.title.lowercase()}; "Date"->rows.sortedBy {it.date}; else->rows.sortedWith(compareByDescending<com.lifemate.database.LifeItem> {it.pinned}.thenByDescending {it.createdAt}) } }}
    LazyVerticalGrid(columns=GridCells.Adaptive(150.dp),contentPadding=PaddingValues(20.dp,16.dp,20.dp,100.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item(span={GridItemSpan(maxLineSpan)}) {PageHeading("Memories","")}
        item(span={GridItemSpan(maxLineSpan)}) {Field(query,{query=it},"Search memories")}
        item(span={GridItemSpan(maxLineSpan)}) {ChoiceChips(listOf("All","Pinned","Archived"),filter) {filter=it}}
        item(span={GridItemSpan(maxLineSpan)}) {ChoiceChips(listOf("Newest","Date","Name"),sort) {sort=it}}
        if(records.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {EmptyState(Kind.MEMORY,"No memories","","Add memory") {navigate("edit/MEMORY/new")}}
        items(records,key={it.id}) { memory ->
            val media=state.media(memory)
            val image=media.firstOrNull {it.mime.startsWith("image/")}
            Surface(onClick={navigate("detail/${memory.id}")},shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center) {
                        if(image!=null) AsyncImage(File(image.path),memory.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        else Icon(if(media.any {it.mime.startsWith("video/")}) Icons.Outlined.PlayCircle else Icons.Outlined.PhotoLibrary,null,Modifier.size(36.dp))
                        if(media.any {it.mime.startsWith("video/")}) Icon(Icons.Outlined.PlayCircle,"Contains video",Modifier.align(Alignment.BottomEnd).padding(8.dp),tint=MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(memory.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(LocalDate.parse(memory.date).format(dateFormat),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
