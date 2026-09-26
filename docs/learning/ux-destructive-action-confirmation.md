# UX Pattern: Destructive Action Confirmation in Compose

**Screen:** HomeScreen (hero + secondary cards), TaskListScreen (pending + history), TimerScreen, SettingsScreen (backup restore).

## Current Rule

Single-item delete is never blocked by a confirmation dialog — it always uses an **Undo Snackbar**, including the Hero Task Card and the Completed History tab. A confirmation `AlertDialog` is reserved for actions that are either **mass** (affect more than one item) or otherwise **irreversible with no undo path**:
- Vaciar Historial (clears all completed tasks at once).
- Restaurar backup in Overwrite mode (replaces the whole local database).
- Abandonar timer session (the elapsed focus time cannot be recovered).

This replaced an earlier rule that reserved the pre-confirmation dialog for the Hero Task Card specifically, on the reasoning that it was "high-value." In practice a single task is never higher-stakes than any other single task, and the inconsistency (dialog on the hero, Undo Snackbar everywhere else) was itself a source of confusion. Undo Snackbar is now the default for every single-item delete; only genuinely irreversible or bulk actions get a dialog.

## Undo Snackbar Pattern

```kotlin
private fun deleteWithUndo(task: Task, viewModel: TaskViewModel, scope: CoroutineScope, snackbarHostState: SnackbarHostState) {
    viewModel.deleteTask(task.taskId)
    scope.launch {
        val result = snackbarHostState.showSnackbar(
            message = "Tarea eliminada",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.restoreTask(task)
        }
    }
}
```

## Confirmation Dialog Pattern (mass/irreversible only)

```kotlin
var showAbandonDialog by remember { mutableStateOf(false) }

if (showAbandonDialog) {
    AlertDialog(
        onDismissRequest = { showAbandonDialog = false },
        title = { Text("¿Abandonar sesión?") },
        text = { Text("El temporizador se detendrá y la tarea seguirá pendiente.") },
        confirmButton = {
            Button(
                onClick = { showAbandonDialog = false; onAbandon() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Abandonar") }
        },
        dismissButton = { TextButton(onClick = { showAbandonDialog = false }) { Text("Seguir") } }
    )
}
```

## When to Apply Which

Use **Undo Snackbar** (no dialog) for:
1. Deleting any single task or history entry, anywhere in the app.
2. Any action with a working `restore*()`/`uncomplete()` counterpart.

Use **pre-confirmation `AlertDialog`** only when:
1. The action affects many items at once (Vaciar Historial), or
2. There is no undo path at all (Overwrite restore, Abandonar timer).
