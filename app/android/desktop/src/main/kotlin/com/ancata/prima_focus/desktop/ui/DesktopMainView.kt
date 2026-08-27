package com.ancata.prima_focus.desktop.ui

import com.ancata.prima_focus.core.engine.SharedPriorityEngine
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager
import com.ancata.prima_focus.desktop.sync.DesktopSyncServer
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder

class DesktopMainView(
    private val dbManager: DesktopDatabaseManager,
    private val syncServer: DesktopSyncServer
) {
    private val priorityEngine = SharedPriorityEngine()
    private val frame = JFrame("Prima-Focus — Deep Work & Priority Companion")
    private val taskListModel = DefaultListModel<Task>()
    private val taskList = JList(taskListModel)

    private val titleLabel = JLabel("Top Focus Tasks")
    private val statusLabel = JLabel("LAN Sync Activo (Puerto ${syncServer.activePort})")
    private val pinBadge = JLabel("PIN: ${syncServer.currentPin}")

    // Theme Colors
    private val bgDark = Color(0x12, 0x11, 0x18)
    private val cardBg = Color(0x1C, 0x1A, 0x24)
    private val cardHover = Color(0x28, 0x25, 0x33)
    private val cardSelected = Color(0x35, 0x2A, 0x44)
    private val primaryAccent = Color(0xEC, 0x48, 0x99) // Pink accent
    private val secondaryAccent = Color(0x8B, 0x5C, 0xF6) // Purple accent
    private val textPrimary = Color(0xF8, 0xFA, 0xFC)
    private val textSecondary = Color(0x94, 0xA3, 0xB8)
    private val textMuted = Color(0x64, 0x74, 0x8B)
    private val borderDim = Color(0x2E, 0x2A, 0x3B)
    private val successGreen = Color(0x10, 0xB9, 0x81)

    fun show() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.minimumSize = Dimension(820, 560)
        frame.preferredSize = Dimension(1040, 700)

        // Set App Icon
        try {
            val iconStream = javaClass.getResourceAsStream("/icon.png")
            if (iconStream != null) {
                val iconImg = ImageIO.read(iconStream)
                frame.iconImage = iconImg
                if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    Taskbar.getTaskbar().iconImage = iconImg
                }
            }
        } catch (_: Exception) {}

        val mainContainer = JPanel(BorderLayout(0, 0)).apply {
            background = bgDark
        }

        // --- TOP HEADER BAR ---
        val headerPanel = JPanel(BorderLayout()).apply {
            background = cardBg
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderDim),
                EmptyBorder(18, 24, 18, 24)
            )
        }

        val headerLeft = JPanel(FlowLayout(FlowLayout.LEFT, 12, 0)).apply {
            isOpaque = false
        }

        val brandDot = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = primaryAccent
                g2.fillOval(0, 4, 14, 14)
            }
            override fun getPreferredSize(): Dimension = Dimension(14, 22)
        }.apply { isOpaque = false }

        titleLabel.apply {
            foreground = textPrimary
            font = Font("Segoe UI", Font.BOLD, 20)
        }

        val subtitleLabel = JLabel("Companion de Escritorio").apply {
            foreground = textSecondary
            font = Font("Segoe UI", Font.PLAIN, 13)
        }

        val titlesBox = Box.createVerticalBox().apply {
            add(titleLabel)
            add(Box.createVerticalStrut(2))
            add(subtitleLabel)
        }

        headerLeft.add(brandDot)
        headerLeft.add(titlesBox)

        val headerRight = JPanel(FlowLayout(FlowLayout.RIGHT, 12, 0)).apply {
            isOpaque = false
        }

        pinBadge.apply {
            foreground = Color.WHITE
            font = Font("Segoe UI", Font.BOLD, 13)
            isOpaque = true
            background = Color(0x3B, 0x2D, 0x54)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(secondaryAccent, 1, true),
                EmptyBorder(6, 14, 6, 14)
            )
        }

        val btnNewHeader = createStyledButton("+ Nueva Tarea (Ctrl+N)", primaryAccent, Color.WHITE) {
            openNewTaskDialog()
        }

        headerRight.add(pinBadge)
        headerRight.add(btnNewHeader)

        headerPanel.add(headerLeft, BorderLayout.WEST)
        headerPanel.add(headerRight, BorderLayout.EAST)

        // --- CENTER TASK LIST ---
        taskList.apply {
            background = bgDark
            foreground = textPrimary
            selectionBackground = cardSelected
            selectionForeground = textPrimary
            border = EmptyBorder(12, 16, 12, 16)
            setCellRenderer(ModernTaskRenderer())
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        val scrollPane = JScrollPane(taskList).apply {
            border = BorderFactory.createEmptyBorder()
            background = bgDark
            viewport.background = bgDark
            verticalScrollBar.unitIncrement = 16
        }

        // --- BOTTOM ACTION TOOLBAR ---
        val footerPanel = JPanel(BorderLayout()).apply {
            background = cardBg
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderDim),
                EmptyBorder(12, 20, 12, 20)
            )
        }

        val actionButtons = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            isOpaque = false
        }

        val btnComplete = createStyledButton("✓ Completar (Ctrl+Enter)", successGreen, Color.WHITE) {
            completeSelectedTask()
        }
        val btnBoost = createStyledButton("▲ Priorizar (+10)", Color(0x2D, 0x37, 0x48), textPrimary) {
            boostSelectedTask(10.0)
        }
        val btnDemote = createStyledButton("▼ Posponer (-10)", Color(0x2D, 0x37, 0x48), textPrimary) {
            boostSelectedTask(-10.0)
        }
        val btnExport = createStyledButton("Exportar Backup", Color(0x1E, 0x29, 0x3B), textSecondary) {
            exportBackup()
        }
        val btnImport = createStyledButton("Importar Backup", Color(0x1E, 0x29, 0x3B), textSecondary) {
            importBackup()
        }

        actionButtons.add(btnComplete)
        actionButtons.add(btnBoost)
        actionButtons.add(btnDemote)
        actionButtons.add(btnExport)
        actionButtons.add(btnImport)

        statusLabel.apply {
            foreground = textMuted
            font = Font("Segoe UI", Font.PLAIN, 12)
        }

        val footerRight = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 4)).apply {
            isOpaque = false
            add(statusLabel)
        }

        footerPanel.add(actionButtons, BorderLayout.WEST)
        footerPanel.add(footerRight, BorderLayout.EAST)

        mainContainer.add(headerPanel, BorderLayout.NORTH)
        mainContainer.add(scrollPane, BorderLayout.CENTER)
        mainContainer.add(footerPanel, BorderLayout.SOUTH)

        frame.contentPane.add(mainContainer)

        setupKeyboardShortcuts()
        refreshTasks()

        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        frame.toFront()
        frame.requestFocus()
    }

    private fun createStyledButton(text: String, bg: Color, fg: Color, onClick: () -> Unit): JButton {
        return object : JButton(text) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                if (model.isPressed) {
                    g2.color = bg.darker()
                } else if (model.isRollover) {
                    g2.color = bg.brighter()
                } else {
                    g2.color = bg
                }
                g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 10f, 10f))
                super.paintComponent(g)
            }
        }.apply {
            foreground = fg
            font = Font("Segoe UI", Font.BOLD, 12)
            isContentAreaFilled = false
            isFocusPainted = false
            border = EmptyBorder(8, 16, 8, 16)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { onClick() }
        }
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
        Thread {
            try {
                val tasks = dbManager.getPendingActiveTasks()
                SwingUtilities.invokeLater {
                    taskListModel.clear()
                    tasks.forEach { taskListModel.addElement(it) }
                    titleLabel.text = "Tareas de Enfoque (${tasks.size})"
                }
            } catch (ex: Exception) {
                System.err.println("[ERROR] Failed to refresh tasks: ${ex.message}")
                SwingUtilities.invokeLater {
                    statusLabel.text = "Error al actualizar: ${ex.message}"
                }
            }
        }.also { it.isDaemon = true; it.name = "PrimaFocus-DBRefresh" }.start()
    }

    private fun openNewTaskDialog() {
        val dialogPanel = JPanel(GridLayout(0, 1, 6, 6)).apply {
            border = EmptyBorder(8, 8, 8, 8)
        }
        val txtTitle = JTextField()
        val comboCategory = JComboBox(arrayOf("Trabajo", "Salud", "Hogar", "Personal", "Estudio"))

        dialogPanel.add(JLabel("Título de la tarea:"))
        dialogPanel.add(txtTitle)
        dialogPanel.add(JLabel("Categoría:"))
        dialogPanel.add(comboCategory)

        val result = JOptionPane.showConfirmDialog(
            frame,
            dialogPanel,
            "Nueva Tarea de Enfoque",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result == JOptionPane.OK_OPTION && txtTitle.text.isNotBlank()) {
            val now = System.currentTimeMillis()
            val cat = comboCategory.selectedItem?.toString()?.lowercase() ?: "trabajo"
            val weight = when (cat) {
                "trabajo" -> 3.0
                "salud" -> 2.5
                "estudio" -> 2.0
                else -> 1.5
            }
            val newTask = Task(
                taskId = UUID.randomUUID().toString(),
                title = txtTitle.text.trim(),
                category = cat,
                categoryWeight = weight,
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

    private inner class ModernTaskRenderer : ListCellRenderer<Task> {
        override fun getListCellRendererComponent(
            list: JList<out Task>?,
            value: Task?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val panel = object : JPanel(BorderLayout(12, 0)) {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = if (isSelected) cardSelected else cardBg
                    g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 12f, 12f))
                    if (isSelected) {
                        g2.color = primaryAccent
                        g2.stroke = BasicStroke(1.5f)
                        g2.draw(RoundRectangle2D.Float(1f, 1f, width - 2f, height - 2f, 12f, 12f))
                    } else {
                        g2.color = borderDim
                        g2.stroke = BasicStroke(1f)
                        g2.draw(RoundRectangle2D.Float(0f, 0f, width - 1f, height - 1f, 12f, 12f))
                    }
                }
            }.apply {
                isOpaque = false
                border = EmptyBorder(12, 16, 12, 16)
            }

            if (value == null) return panel

            val rank = index + 1
            val leftBox = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply { isOpaque = false }

            val rankLabel = JLabel("#$rank").apply {
                foreground = if (rank <= 3) primaryAccent else textMuted
                font = Font("Segoe UI", Font.BOLD, 15)
                preferredSize = Dimension(32, 24)
            }

            val titleText = JLabel(value.title).apply {
                foreground = textPrimary
                font = Font("Segoe UI", Font.BOLD, 14)
            }

            leftBox.add(rankLabel)
            leftBox.add(titleText)

            val rightBox = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply { isOpaque = false }

            // Category badge
            val (catBg, catFg) = when (value.category.lowercase()) {
                "trabajo" -> Color(0x31, 0x2E, 0x81) to Color(0xA5, 0xB4, 0xFC)
                "salud" -> Color(0x06, 0x4E, 0x3B) to Color(0x6E, 0xE7, 0xB7)
                "estudio" -> Color(0x70, 0x1A, 0x75) to Color(0xF4, 0x72, 0xB6)
                else -> Color(0x33, 0x41, 0x55) to Color(0xCB, 0xD5, 0xE1)
            }

            val catBadge = JLabel(value.category.uppercase()).apply {
                foreground = catFg
                background = catBg
                font = Font("Segoe UI", Font.BOLD, 11)
                isOpaque = true
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(catFg.darker(), 1, true),
                    EmptyBorder(3, 8, 3, 8)
                )
            }

            // Priority Score
            val scoreLabel = JLabel("Score: %.1f".format(value.priorityScore)).apply {
                foreground = if (value.priorityScore >= 80) primaryAccent else textSecondary
                font = Font("Segoe UI", Font.BOLD, 12)
            }

            rightBox.add(catBadge)
            if (value.manualBoost != 0.0) {
                val boostBadge = JLabel("${if (value.manualBoost > 0) "+" else ""}${value.manualBoost.toInt()}").apply {
                    foreground = if (value.manualBoost > 0) successGreen else Color(0xEF, 0x44, 0x44)
                    font = Font("Segoe UI", Font.BOLD, 11)
                }
                rightBox.add(boostBadge)
            }
            rightBox.add(scoreLabel)

            panel.add(leftBox, BorderLayout.WEST)
            panel.add(rightBox, BorderLayout.EAST)

            val wrapper = JPanel(BorderLayout()).apply {
                isOpaque = false
                border = EmptyBorder(3, 0, 3, 0)
                add(panel, BorderLayout.CENTER)
            }

            return wrapper
        }
    }
}

