package com.ancata.prima_focus.desktop.ui

import com.ancata.prima_focus.core.engine.SharedPriorityEngine
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager

import com.ancata.prima_focus.desktop.sync.DesktopSyncServer
import java.awt.*
import java.awt.event.KeyEvent
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.swing.*
import javax.swing.border.EmptyBorder

class DesktopMainView(
    private val dbManager: DesktopDatabaseManager,
    private val syncServer: DesktopSyncServer
) {
    private val priorityEngine = SharedPriorityEngine()
    private val frame = JFrame("Prima-Focus — Deep Work & Priority Focus")
    private val taskListModel = DefaultListModel<Task>()
    private val taskList = JList(taskListModel)
    
    private val titleLabel = JLabel("Top Focus Tasks")
    private val statusLabel = JLabel("Servidor de Sincronización activo en puerto 8765")
    private val pinLabel = JLabel("PIN de Emparejamiento: ${syncServer.currentPin}")

    fun show() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(980, 680)
        frame.minimumSize = Dimension(720, 480)
        frame.setLocationRelativeTo(null)

        val mainPanel = JPanel(BorderLayout(16, 16)).apply {
            background = Color(0x0F, 0x08, 0x12)
            border = EmptyBorder(16, 16, 16, 16)
        }

        // Header Panel
        val headerPanel = JPanel(BorderLayout()).apply {
            background = Color(0x19, 0x0E, 0x1D)
            border = EmptyBorder(12, 16, 12, 16)
        }

        titleLabel.apply {
            foreground = Color(0xF4, 0x72, 0xB6)
            font = Font("Segoe UI", Font.BOLD, 18)
        }

        pinLabel.apply {
            foreground = Color.WHITE
            font = Font("Segoe UI", Font.BOLD, 14)
        }

        headerPanel.add(titleLabel, BorderLayout.WEST)
        headerPanel.add(pinLabel, BorderLayout.EAST)

        // Center Task List Panel
        taskList.apply {
            background = Color(0x18, 0x10, 0x1E)
            foreground = Color.WHITE
            font = Font("Segoe UI", Font.PLAIN, 14)
            selectionBackground = Color(0xEC, 0x48, 0x99)
            selectionForeground = Color.WHITE
            setCellRenderer(TaskCellRenderer())
        }

        val scrollPane = JScrollPane(taskList).apply {
            border = BorderFactory.createLineBorder(Color(0x33, 0xFB, 0xC5), 1)
        }

        // Bottom Action Toolbar
        val bottomPanel = JPanel(BorderLayout()).apply {
            background = Color(0x19, 0x0E, 0x1D)
            border = EmptyBorder(8, 12, 8, 12)
        }

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            background = Color(0x19, 0x0E, 0x1D)
        }

        val btnNew = JButton("+ Nueva Tarea (Ctrl+N)").apply {
            addActionListener { openNewTaskDialog() }
        }
        val btnComplete = JButton("✓ Completar (Ctrl+Enter)").apply {
            addActionListener { completeSelectedTask() }
        }
        val btnBoost = JButton("▲ Boost (+10)").apply {
            addActionListener { boostSelectedTask(10.0) }
        }
        val btnDemote = JButton("▼ Demote (-10)").apply {
            addActionListener { boostSelectedTask(-10.0) }
        }
        val btnExport = JButton("Exportar Backup").apply {
            addActionListener { exportBackup() }
        }
        val btnImport = JButton("Importar Backup").apply {
            addActionListener { importBackup() }
        }

        btnPanel.add(btnNew)
        btnPanel.add(btnComplete)
        btnPanel.add(btnBoost)
        btnPanel.add(btnDemote)
        btnPanel.add(btnExport)
        btnPanel.add(btnImport)

        statusLabel.apply {
            foreground = Color.LIGHT_GRAY
            font = Font("Segoe UI", Font.ITALIC, 12)
        }

        bottomPanel.add(btnPanel, BorderLayout.WEST)
        bottomPanel.add(statusLabel, BorderLayout.EAST)

        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        frame.contentPane.add(mainPanel)

        // Setup Keyboard Shortcuts
        setupKeyboardShortcuts()

        refreshTasks()
        frame.pack()
        frame.size = Dimension(980, 680)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        frame.toFront()
        frame.requestFocus()
    }

    private fun setupKeyboardShortcuts() {
        val rootPane = frame.rootPane
        rootPane.registerKeyboardAction(
            { openNewTaskDialog() },
            KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )
        rootPane.registerKeyboardAction(
            { completeSelectedTask() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )
    }

    fun refreshTasks() {
        // P0: Load tasks on a background daemon thread — never block the EDT with DB calls
        Thread {
            try {
                val tasks = dbManager.getPendingActiveTasks()
                SwingUtilities.invokeLater {
                    taskListModel.clear()
                    tasks.forEach { taskListModel.addElement(it) }
                    titleLabel.text = "Tareas Priorizadas (${tasks.size} pendientes)"
                }
            } catch (ex: Exception) {
                System.err.println("[ERROR] Failed to refresh tasks from DB: ${ex.message}")
                SwingUtilities.invokeLater {
                    statusLabel.text = "Error cargando tareas: ${ex.message}"
                }
            }
        }.also { it.isDaemon = true; it.name = "PrimaFocus-DBRefresh" }.start()
    }


    private fun openNewTaskDialog() {
        val title = JOptionPane.showInputDialog(frame, "Título de la tarea:", "Nueva Tarea", JOptionPane.PLAIN_MESSAGE)
        if (!title.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val newTask = Task(
                taskId = UUID.randomUUID().toString(),
                title = title.trim(),
                category = "trabajo",
                categoryWeight = 3.0,
                date = "Hoy",
                createdAt = now,
                updatedAt = now,
                syncVersion = 1L
            )
            val prioritized = priorityEngine.calculatePriority(newTask, now)
            dbManager.insertOrUpdateTask(prioritized)
            refreshTasks()
        }
    }

    private fun completeSelectedTask() {
        val selected = taskList.selectedValue ?: return
        val now = System.currentTimeMillis()
        val completed = selected.copy(
            status = "completed",
            updatedAt = now,
            syncVersion = selected.syncVersion + 1
        )
        dbManager.insertOrUpdateTask(completed)
        refreshTasks()
        JOptionPane.showMessageDialog(frame, "¡Tarea '${selected.title}' completada!", "Completada", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun boostSelectedTask(amount: Double) {
        val selected = taskList.selectedValue ?: return
        val now = System.currentTimeMillis()
        val boosted = selected.copy(
            manualBoost = selected.manualBoost + amount,
            updatedAt = now,
            syncVersion = selected.syncVersion + 1
        )
        val calculated = priorityEngine.calculatePriority(boosted, now)
        dbManager.insertOrUpdateTask(calculated)
        refreshTasks()
    }

    private fun exportBackup() {
        val fileChooser = JFileChooser()
        fileChooser.selectedFile = File("primafocus_backup_${LocalDate.now()}.json")
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            val json = syncServer.exportBackupJson()
            fileChooser.selectedFile.writeText(json, Charsets.UTF_8)
            JOptionPane.showMessageDialog(frame, "Copia de respaldo exportada exitosamente.", "Exportar", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    private fun importBackup() {
        val fileChooser = JFileChooser()
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            val json = fileChooser.selectedFile.readText(Charsets.UTF_8)
            val merged = syncServer.importBackupJson(json)
            refreshTasks()
            JOptionPane.showMessageDialog(frame, "Respaldo importado: $merged cambios integrados.", "Importar", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    private class TaskCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            if (value is Task) {
                val rank = index + 1
                val boostStr = if (value.manualBoost != 0.0) " [Boost: ${value.manualBoost.toInt()}]" else ""
                val scoreStr = "%.1f".format(value.priorityScore)
                label.text = " #$rank  |  ${value.title}  (${value.category.uppercase()})  —  Score: $scoreStr$boostStr"
                label.border = EmptyBorder(8, 12, 8, 12)
            }
            return label
        }
    }
}
