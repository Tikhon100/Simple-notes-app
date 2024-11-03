package com.example.lab2.screens

import android.app.DatePickerDialog
import android.widget.Toast

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale



data class Note(
    val id: String = "", // Добавляем id с пустым значением по умолчанию
    val title: String,
    val description: String,
    val date: Date,
    val status: Int // 0 - todo 1 - done
)

@Composable
fun CreateNoteBottomSheet(
    onDismiss: () -> Unit,
    viewModel: NoteViewModel
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Создаем DatePickerDialog внутри remember
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Создание заметки",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedButton(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                selectedDate?.let { dateFormatter.format(it) } ?: "Выберите дату"
            )
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Отмена")
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        isLoading = true
                        errorMessage = null

                        val currentDate = selectedDate ?: Date()
                        val note = Note(
                            title = title.trim(),
                            description = description.trim(),
                            date = currentDate,
                            status = 0
                        )

                        viewModel.addNote(
                            note = note,
                            onSuccess = {
                                isLoading = false
                                context.applicationContext?.let { ctx ->
                                    Toast.makeText(
                                        ctx,
                                        "Заметка успешно создана",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onDismiss()
                            },
                            onError = { exception ->
                                isLoading = false
                                errorMessage = "Ошибка: ${exception.message}"
                            }
                        )
                    }
                },
                enabled = title.isNotBlank() && !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Сохранить")
                }
            }
        }
    }
}

class NoteViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notesCollection = db.collection("notes")

    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> = _notes

    private val _savingState = MutableStateFlow<SaveState>(SaveState.Idle)
    val savingState: StateFlow<SaveState> = _savingState

    fun addNote(note: Note, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError(Exception("User not authenticated"))
            return
        }

        val noteMap = hashMapOf(
            "title" to note.title,
            "description" to note.description,
            "date" to note.date,
            "status" to note.status,
            "userEmail" to currentUser.email,
            "timestamp" to FieldValue.serverTimestamp()
        )

        notesCollection
            .add(noteMap)
            .addOnSuccessListener { documentReference ->
                val noteWithId = note.copy(id = documentReference.id)
                _notes.add(noteWithId)
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun fetchNotes() {
        val currentUser = auth.currentUser ?: return

        notesCollection
            .whereEqualTo("userEmail", currentUser.email)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                _notes.clear()
                snapshot?.documents?.forEach { doc ->
                    val note = Note(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getDate("date") ?: Date(),
                        status = doc.getLong("status")?.toInt() ?: 0
                    )
                    _notes.add(note)
                }
            }
    }

    // Новый метод для обновления статуса заметки
    fun updateNoteStatus(note: Note, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        if (note.id.isEmpty()) {
            onError(Exception("Note ID is empty"))
            return
        }

        notesCollection.document(note.id)
            .update("status", note.status)
            .addOnSuccessListener {
                val index = _notes.indexOfFirst { it.id == note.id }
                if (index != -1) {
                    _notes[index] = note
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Новый метод для удаления заметки
    fun deleteNote(note: Note, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        if (note.id.isEmpty()) {
            onError(Exception("Note ID is empty"))
            return
        }

        notesCollection.document(note.id)
            .delete()
            .addOnSuccessListener {
                _notes.removeAll { it.id == note.id }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

// Компонент для отображения отдельной заметки
@Composable
fun NoteItem(
    note: Note,
    onStatusChange: (Note) -> Unit = {},
    onDeleteClick: (Note) -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                        showContextMenu = true
                    },
                    onDoubleTap = {
                        showDetailsDialog = true
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormatter.format(note.date),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (showDetailsDialog) {
            Dialog(
                onDismissRequest = { showDetailsDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Детали заметки",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        DetailRow(label = "Название:", content = note.title)
                        DetailRow(label = "Описание:", content = note.description)
                        DetailRow(
                            label = "Дата:",
                            content = dateFormatter.format(note.date)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showDetailsDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = pressOffset
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        when (note.status) {
                            0 -> "Выполнено"
                            1 -> "Вернуть"
                            else -> "Неизвестно"
                        }
                    )
                },
                onClick = {
                    onStatusChange(note.copy(status = if (note.status == 0) 1 else 0))
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Отметить как выполненное"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Удалить") },
                onClick = {
                    onDeleteClick(note)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить заметку"
                    )
                }
            )
        }
    }
}
@Composable
private fun DetailRow(
    label: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge
        )
        Divider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}