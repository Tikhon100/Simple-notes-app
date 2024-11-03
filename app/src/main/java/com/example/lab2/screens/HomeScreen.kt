package com.example.lab2.screens

import SharedPrefsHelper
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val notes = viewModel.notes

    // Состояния для отслеживания развернутых/свернутых секций
    var isTodayExpanded by remember { mutableStateOf(true) }
    var isTomorrowExpanded by remember { mutableStateOf(true) }
    var isThisWeekExpanded by remember { mutableStateOf(true) }
    var isThisMonthExpanded by remember { mutableStateOf(true) }
    var isLaterExpanded by remember { mutableStateOf(true) }


    // профиль
    var showProfilePanel by remember { mutableStateOf(false) }
    val offsetX = remember { mutableStateOf(0f) }
    val isDragging = remember { mutableStateOf(false) }

    // выполненные задания
    var showCompletedTasks by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.fetchNotes()
    }

    Scaffold(
        topBar = {
            Text(
                text = "Текущие задачи",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor
            ) {
                IconButton(onClick = { showCompletedTasks = true }) {
                    Icon(Icons.Filled.List, contentDescription = "Выполненные задачи")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showProfilePanel = true }) {
                    Icon(Icons.Filled.Person, contentDescription = "Открыть профиль")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Сегодняшние задачи
            val today = notes.filter { isToday(it.date) && it.status==0 }
            if (today.isNotEmpty()) {
                item {
                    ExpandableSection(
                        title = "Сегодня",
                        isExpanded = isTodayExpanded,
                        onToggle = { isTodayExpanded = !isTodayExpanded },
                        itemsCount = today.size
                    )
                }
                if (isTodayExpanded) {
                    items(today) { note ->
                        NoteItem(
                            note = note,
                            onStatusChange = { updatedNote ->
                                viewModel.updateNoteStatus(
                                    updatedNote,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error updating note status", exception)
                                    }
                                )
                            },
                            onDeleteClick = { noteToDelete ->
                                viewModel.deleteNote(
                                    noteToDelete,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error deleting note", exception)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Завтрашние задачи
            val tomorrow = notes.filter { (isTomorrow(it.date) && it.status == 0)}
            if (tomorrow.isNotEmpty()) {
                item {
                    ExpandableSection(
                        title = "Завтра",
                        isExpanded = isTomorrowExpanded,
                        onToggle = { isTomorrowExpanded = !isTomorrowExpanded },
                        itemsCount = tomorrow.size
                    )
                }
                if (isTomorrowExpanded) {
                    items(tomorrow) { note ->
                        NoteItem(
                            note = note,
                            onStatusChange = { updatedNote ->
                                viewModel.updateNoteStatus(
                                    updatedNote,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error updating note status", exception)
                                    }
                                )
                            },
                            onDeleteClick = { noteToDelete ->
                                viewModel.deleteNote(
                                    noteToDelete,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error deleting note", exception)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Задачи на этой неделе
            val thisWeek = notes.filter { isThisWeek(it.date) && it.status == 0}
            if (thisWeek.isNotEmpty()) {
                item {
                    ExpandableSection(
                        title = "На этой неделе",
                        isExpanded = isThisWeekExpanded,
                        onToggle = { isThisWeekExpanded = !isThisWeekExpanded },
                        itemsCount = thisWeek.size
                    )
                }
                if (isThisWeekExpanded) {
                    items(thisWeek) { note ->
                        NoteItem(
                            note = note,
                            onStatusChange = { updatedNote ->
                                viewModel.updateNoteStatus(
                                    updatedNote,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error updating note status", exception)
                                    }
                                )
                            },
                            onDeleteClick = { noteToDelete ->
                                viewModel.deleteNote(
                                    noteToDelete,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error deleting note", exception)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Задачи в этом месяце
            val thisMonth = notes.filter { isThisMonth(it.date) && it.status==0 }
            if (thisMonth.isNotEmpty()) {
                item {
                    ExpandableSection(
                        title = "В этом месяце",
                        isExpanded = isThisMonthExpanded,
                        onToggle = { isThisMonthExpanded = !isThisMonthExpanded },
                        itemsCount = thisMonth.size
                    )
                }
                if (isThisMonthExpanded) {
                    items(thisMonth) { note ->
                        NoteItem(
                            note = note,
                            onStatusChange = { updatedNote ->
                                viewModel.updateNoteStatus(
                                    updatedNote,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error updating note status", exception)
                                    }
                                )
                            },
                            onDeleteClick = { noteToDelete ->
                                viewModel.deleteNote(
                                    noteToDelete,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error deleting note", exception)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Более поздние задачи
            val later = notes.filter { isLater(it.date) && it.status == 0}
            if (later.isNotEmpty()) {
                item {
                    ExpandableSection(
                        title = "Позже",
                        isExpanded = isLaterExpanded,
                        onToggle = { isLaterExpanded = !isLaterExpanded },
                        itemsCount = later.size
                    )
                }
                if (isLaterExpanded) {
                    items(later) { note ->
                        NoteItem(
                            note = note,
                            onStatusChange = { updatedNote ->
                                viewModel.updateNoteStatus(
                                    updatedNote,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error updating note status", exception)
                                    }
                                )
                            },
                            onDeleteClick = { noteToDelete ->
                                viewModel.deleteNote(
                                    noteToDelete,
                                    onSuccess = {
                                        // Можно добавить Toast или Snackbar с сообщением об успехе
                                    },
                                    onError = { exception ->
                                        // Обработка ошибки, например показ сообщения пользователю
                                        Log.e("HomeScreen", "Error deleting note", exception)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState,
            windowInsets = WindowInsets(0),
            modifier = Modifier.fillMaxSize()
        ) {
            CreateNoteBottomSheet(
                onDismiss = { showAddDialog = false },
                viewModel = viewModel()
            )
        }
    }
    // Выдвижная панель профиля
    if (showProfilePanel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { showProfilePanel = false }
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging.value = true },
                            onDragEnd = {
                                isDragging.value = false
                                if (offsetX.value > 100) {
                                    showProfilePanel = false
                                }
                                offsetX.value = 0f
                            },
                            onDragCancel = {
                                isDragging.value = false
                                offsetX.value = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) { // Только вправо
                                    offsetX.value = (offsetX.value + dragAmount).coerceAtLeast(0f)
                                }
                            }
                        )
                    }
                    .clickable(enabled = false) { },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Профиль",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showProfilePanel = false }) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Закрыть панель"
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 16.dp),
                        shape = CircleShape,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(24.dp)
                                .size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = SharedPrefsHelper.getLogin() ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Окно выполненных задач
    if (showCompletedTasks) {
        Dialog(
            onDismissRequest = { showCompletedTasks = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    // Заголовок
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Выполненные задачи",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showCompletedTasks = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    // Список выполненных задач
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        val completedThisMonth = notes.filter { it.status == 1 }
                        if (completedThisMonth.isNotEmpty()) {
                            item {
                                ExpandableSection(
                                    title = "Выполнено за все время",
                                    isExpanded = isThisMonthExpanded,
                                    onToggle = { isThisMonthExpanded = !isThisMonthExpanded },
                                    itemsCount = completedThisMonth.size
                                )
                            }
                            if (isThisMonthExpanded) {
                                items(completedThisMonth) { note ->
                                    NoteItem(
                                        note = note,
                                        onStatusChange = { updatedNote ->
                                            viewModel.updateNoteStatus(
                                                updatedNote,
                                                onSuccess = {
                                                    // Можно добавить Toast или Snackbar с сообщением об успехе
                                                },
                                                onError = { exception ->
                                                    Log.e("HomeScreen", "Error updating note status", exception)
                                                }
                                            )
                                        },
                                        onDeleteClick = { noteToDelete ->
                                            viewModel.deleteNote(
                                                noteToDelete,
                                                onSuccess = {
                                                    // Можно добавить Toast или Snackbar с сообщением об успехе
                                                },
                                                onError = { exception ->
                                                    Log.e("HomeScreen", "Error deleting note", exception)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    itemsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onToggle() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title ($itemsCount)",
            style = MaterialTheme.typography.titleMedium
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Свернуть" else "Развернуть"
        )
    }
}

// Вспомогательные функции для проверки дат
private fun isToday(date: Date): Boolean {
    val noteCalendar = Calendar.getInstance().apply { time = date }
    val todayCalendar = Calendar.getInstance()

    return noteCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
            noteCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
}

private fun isTomorrow(date: Date): Boolean {
    val noteCalendar = Calendar.getInstance().apply { time = date }
    val tomorrowCalendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
    }

    return noteCalendar.get(Calendar.YEAR) == tomorrowCalendar.get(Calendar.YEAR) &&
            noteCalendar.get(Calendar.DAY_OF_YEAR) == tomorrowCalendar.get(Calendar.DAY_OF_YEAR)
}

private fun isThisWeek(date: Date): Boolean {
    val noteCalendar = Calendar.getInstance().apply { time = date }
    val todayCalendar = Calendar.getInstance()
    val weekFromNowCalendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 7)
    }

    return !isToday(date) &&
            !isTomorrow(date) &&
            noteCalendar.after(todayCalendar) &&
            noteCalendar.before(weekFromNowCalendar)
}

private fun isThisMonth(date: Date): Boolean {
    val noteCalendar = Calendar.getInstance().apply { time = date }
    val todayCalendar = Calendar.getInstance()

    return noteCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
            noteCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
            !isToday(date) &&
            !isTomorrow(date) &&
            !isThisWeek(date)
}

private fun isLater(date: Date): Boolean {
    return !isToday(date) &&
            !isTomorrow(date) &&
            !isThisWeek(date) &&
            !isThisMonth(date)
}

