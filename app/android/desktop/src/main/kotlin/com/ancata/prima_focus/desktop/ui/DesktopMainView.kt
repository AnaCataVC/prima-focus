package com.ancata.prima_focus.desktop.ui

import com.ancata.prima_focus.core.engine.SharedPriorityEngine
import com.ancata.prima_focus.core.model.PriorityBand
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.core.sync.LANSyncPacket
import com.ancata.prima_focus.core.sync.LanSyncClient
import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager
import com.ancata.prima_focus.desktop.sync.DesktopSyncServer
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlinx.coroutines.runBlocking

class DesktopMainView(
    private val dbManager: DesktopDatabaseManager,
    private val syncServer: DesktopSyncServer
) {
    private val priorityEngine = SharedPriorityEngine()
    private val frame = JFrame("Prima-Focus")
    private val bgExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "PrimaFocus-DBRefresh").apply { isDaemon = true }
    }
    
    private val pendingTasksModel = DefaultListModel<Task>()
    private val completedTasksModel = DefaultListModel<Task>()
    
    private val pendingTaskList = JList(pendingTasksModel)
    private val completedTaskList = JList(completedTasksModel)

    private var currentTab = 0

    // Mobile Brand Colors (Dark Feminine Glassmorphism)
    private val primaryRose = Color(0xF4, 0x72, 0xB6)
    private val roseAccent = Color(0xEC, 0x48, 0x99)
    private val roseGlow = Color(0xFB, 0xC5, 0xDB)
    private val accentSage = Color(0x4A, 0xDE, 0x80)
    private val errorRose = Color(0xFB, 0x71, 0x85)
    
    private val bgDarkEdge = Color(0x0F, 0x08, 0x12) // Deep warm dark plum
    private val bgPlumCenter = Color(0x23, 0x11, 0x27) // Plum radial center
    private val glassSurface = Color(0xFF, 0xFF, 0xFF, 0x14) // Frosted glass card
    private val glassSurfaceHover = Color(0xFF, 0xFF, 0xFF, 0x20)
    private val glassSurfaceSelected = Color(0xEC, 0x48, 0x99, 0x30)
    private val glassBorder = Color(0xFB, 0xC5, 0xDB, 0x33) // 20% soft rose border
    
    private val textPrimary = Color.WHITE
    private val textMuted = Color(0xFF, 0xFF, 0xFF, 0x99)

    // UI Components Header
    private val tabPendingBtn = JButton("Pendientes")
    private val tabHistoryBtn = JButton("Historial")
    private val mainCardsCardLayout = CardLayout()
    private val cardsContainer = JPanel(mainCardsCardLayout)

    fun show() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.minimumSize = Dimension(860, 600)
        frame.preferredSize = Dimension(1060, 720)

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

        // Main Background Panel with Radial Glow matching Android Canvas
        val rootPanel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                val center = Point2D.Float(width / 2f, 0f)
                val radius = height * 0.85f
                val p = RadialGradientPaint(
                    center,
                    radius,
                    floatArrayOf(0.0f, 1.0f),
                    arrayOf(bgPlumCenter, bgDarkEdge)
                )
                g2.paint = p
                g2.fillRect(0, 0, width, height)
            }
        }

        // --- TOP MOBILE-STYLE HEADER ---
        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = EmptyBorder(24, 28, 12, 28)
        }

        val titleBox = JPanel(BorderLayout(0, 4)).apply { isOpaque = false }
        val headerTitle = JLabel("Lista de Tareas").apply {
            foreground = textPrimary
            font = Font("Segoe UI", Font.BOLD, 26)
        }
        val headerSubtitle = JLabel("Companion de Escritorio  •  Sincronización P2P activa").apply {
            foreground = textMuted
            font = Font("Segoe UI", Font.PLAIN, 13)
        }
        titleBox.add(headerTitle, BorderLayout.NORTH)
        titleBox.add(headerSubtitle, BorderLayout.SOUTH)

        val headerRight = JPanel(FlowLayout(FlowLayout.RIGHT, 12, 0)).apply { isOpaque = false }
        val btnSync = createGlassButton("📡 Sincronización LAN", Color(0x3B, 0x1D, 0x40), roseGlow, isPill = true) {
            openSyncDialog()
        }

        val btnNewTask = createGlassButton("+ Nueva Tarea", roseAccent, textPrimary, isPill = true) {
            openNewTaskDialog()
        }

        headerRight.add(btnSync)
        headerRight.add(btnNewTask)

        headerPanel.add(titleBox, BorderLayout.WEST)
        headerPanel.add(headerRight, BorderLayout.EAST)

        // --- SEGMENTED TABS (Pendientes / Historial) ---
        val tabsWrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            border = EmptyBorder(8, 28, 16, 28)
        }

        val segmentedTabsBar = object : JPanel(GridLayout(1, 2, 6, 0)) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = glassSurface
                g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 16f, 16f))
                g2.color = glassBorder
                g2.draw(RoundRectangle2D.Float(0f, 0f, width - 1f, height - 1f, 16f, 16f))
            }
        }.apply {
            isOpaque = false
            border = EmptyBorder(4, 4, 4, 4)
            preferredSize = Dimension(380, 44)
        }

        styleTabButton(tabPendingBtn, isSelected = true)
        styleTabButton(tabHistoryBtn, isSelected = false)

        tabPendingBtn.addActionListener {
            currentTab = 0
            styleTabButton(tabPendingBtn, isSelected = true)
            styleTabButton(tabHistoryBtn, isSelected = false)
            mainCardsCardLayout.show(cardsContainer, "PENDING")
            refreshTasks()
        }

        tabHistoryBtn.addActionListener {
            currentTab = 1
            styleTabButton(tabPendingBtn, isSelected = false)
            styleTabButton(tabHistoryBtn, isSelected = true)
            mainCardsCardLayout.show(cardsContainer, "HISTORY")
            refreshTasks()
        }

        segmentedTabsBar.add(tabPendingBtn)
        segmentedTabsBar.add(tabHistoryBtn)
        tabsWrapper.add(segmentedTabsBar)

        // --- PENDING LIST VIEW ---
        pendingTaskList.apply {
            isOpaque = false
            foreground = textPrimary
            selectionBackground = glassSurfaceSelected
            selectionForeground = textPrimary
            border = EmptyBorder(4, 28, 16, 28)
            setCellRenderer(MobileTaskRenderer(isHistory = false))
        }

        val scrollPending = JScrollPane(pendingTaskList).apply {
            isOpaque = false
            viewport.isOpaque = false
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }

        // --- COMPLETED (HISTORY) LIST VIEW ---
        completedTaskList.apply {
            isOpaque = false
            foreground = textPrimary
            selectionBackground = glassSurfaceSelected
            selectionForeground = textPrimary
            border = EmptyBorder(4, 28, 16, 28)
            setCellRenderer(MobileTaskRenderer(isHistory = true))
        }

        val scrollHistory = JScrollPane(completedTaskList).apply {
            isOpaque = false
            viewport.isOpaque = false
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }

        cardsContainer.isOpaque = false
        cardsContainer.add(scrollPending, "PENDING")
        cardsContainer.add(scrollHistory, "HISTORY")

        // --- BOTTOM QUICK ACTIONS BAR ---
        val footerPanel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.color = Color(0x18, 0x0D, 0x1C, 0xE0) // BottomNavBackground
                g2.fillRect(0, 0, width, height)
                g2.color = glassBorder
                g2.drawLine(0, 0, width, 0)
            }
        }.apply {
            isOpaque = false
            border = EmptyBorder(14, 28, 14, 28)
        }

        val footerActions = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply { isOpaque = false }

        val btnComplete = createGlassButton("✓ Completar (Ctrl+Enter)", accentSage, Color.WHITE) {
            completeSelectedTask()
        }
        val btnBoost = createGlassButton("▲ Priorizar (+10)", roseAccent, Color.WHITE) {
            boostSelectedTask(10.0)
        }
        val btnDemote = createGlassButton("▼ Posponer (-10)", Color(0x64, 0x74, 0x8B), textPrimary) {
            boostSelectedTask(-10.0)
        }
        val btnDelete = createGlassButton("🗑 Eliminar", errorRose, Color.WHITE) {
            deleteSelectedTask()
        }
        val btnExport = createGlassButton("Exportar JSON", Color(0xFF, 0xFF, 0xFF, 0x18), textMuted) {
            exportBackup()
        }
        val btnImport = createGlassButton("Restaurar JSON", Color(0xFF, 0xFF, 0xFF, 0x18), textMuted) {
            importBackup()
        }

        footerActions.add(btnComplete)
        footerActions.add(btnBoost)
        footerActions.add(btnDemote)
        footerActions.add(btnDelete)
        footerActions.add(btnExport)
        footerActions.add(btnImport)

        val statusText = JLabel("LAN: ${syncServer.getLocalIpAddress()}:${syncServer.activePort}  •  PIN: ${syncServer.currentPin}").apply {
            foreground = textMuted
            font = Font("Segoe UI", Font.PLAIN, 12)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Haga clic para abrir la configuración de sincronización LAN"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    openSyncDialog()
                }
            })
        }
        val footerRight = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 4)).apply {
            isOpaque = false
            add(statusText)
        }

        footerPanel.add(footerActions, BorderLayout.WEST)
        footerPanel.add(footerRight, BorderLayout.EAST)

        val centerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(tabsWrapper, BorderLayout.NORTH)
            add(cardsContainer, BorderLayout.CENTER)
        }

        rootPanel.add(headerPanel, BorderLayout.NORTH)
        rootPanel.add(centerPanel, BorderLayout.CENTER)
        rootPanel.add(footerPanel, BorderLayout.SOUTH)

        frame.contentPane.add(rootPanel)

        setupKeyboardShortcuts()
        refreshTasks()

        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        frame.toFront()
        frame.requestFocus()
    }

    private fun styleTabButton(btn: JButton, isSelected: Boolean) {
        btn.apply {
            font = Font("Segoe UI", Font.BOLD, 13)
            foreground = if (isSelected) Color.WHITE else textMuted
            isContentAreaFilled = false
            isFocusPainted = false
            border = EmptyBorder(6, 12, 6, 12)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            // Custom pill background when selected
            setUI(object : javax.swing.plaf.basic.BasicButtonUI() {
                override fun paint(g: Graphics, c: JComponent) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    if (isSelected) {
                        g2.color = roseAccent
                        g2.fill(RoundRectangle2D.Float(2f, 2f, c.width - 4f, c.height - 4f, 12f, 12f))
                    }
                    super.paint(g, c)
                }
            })
        }
    }

    private fun createGlassButton(
        text: String,
        bg: Color,
        fg: Color,
        isPill: Boolean = false,
        onClick: () -> Unit
    ): JButton {
        return object : JButton(text) {
            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val c = if (model.isPressed) bg.darker() else if (model.isRollover) bg.brighter() else bg
                g2.color = c
                val arc = if (isPill) 20f else 12f
                g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), arc, arc))
                g2.color = glassBorder
                g2.draw(RoundRectangle2D.Float(0f, 0f, width - 1f, height - 1f, arc, arc))
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
        bgExecutor.execute {
            try {
                val pending = dbManager.getPendingActiveTasks()
                val all = dbManager.getAllTasks()
                val completed = all.filter { it.status == "completed" && !it.isDeleted }
                    .sortedByDescending { it.updatedAt }

                SwingUtilities.invokeLater {
                    pendingTasksModel.clear()
                    pending.forEach { pendingTasksModel.addElement(it) }

                    completedTasksModel.clear()
                    completed.forEach { completedTasksModel.addElement(it) }

                    tabPendingBtn.text = "Pendientes (${pending.size})"
                    tabHistoryBtn.text = "Historial (${completed.size})"
                }
            } catch (ex: Exception) {
                System.err.println("[ERROR] Failed to refresh tasks: ${ex.message}")
            }
        }
    }

    private fun openNewTaskDialog() {
        val dialog = JDialog(frame, "Nueva Tarea", true)
        dialog.layout = BorderLayout()
        dialog.setSize(440, 320)
        dialog.setLocationRelativeTo(frame)

        val form = JPanel(GridLayout(0, 1, 8, 8)).apply {
            background = bgPlumCenter
            border = EmptyBorder(20, 24, 20, 24)
        }

        val lblTitle = JLabel("Título de la tarea:").apply { foreground = textPrimary; font = Font("Segoe UI", Font.BOLD, 13) }
        val txtTitle = JTextField().apply {
            background = Color(0x19, 0x0E, 0x1D)
            foreground = textPrimary
            caretColor = primaryRose
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(glassBorder, 1, true),
                EmptyBorder(8, 10, 8, 10)
            )
            font = Font("Segoe UI", Font.PLAIN, 14)
        }

        val lblCat = JLabel("Categoría:").apply { foreground = textPrimary; font = Font("Segoe UI", Font.BOLD, 13) }
        val comboCategory = JComboBox(arrayOf("Trabajo", "Salud", "Hogar", "Personal", "Estudio")).apply {
            background = Color(0x19, 0x0E, 0x1D)
            foreground = textPrimary
            font = Font("Segoe UI", Font.PLAIN, 13)
        }

        form.add(lblTitle)
        form.add(txtTitle)
        form.add(lblCat)
        form.add(comboCategory)

        val btnActions = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 14)).apply {
            background = bgDarkEdge
            border = EmptyBorder(0, 16, 8, 16)
        }

        val btnCancel = createGlassButton("Cancelar", Color(0xFF, 0xFF, 0xFF, 0x20), textMuted) {
            dialog.dispose()
        }
        val btnSave = createGlassButton("Guardar Tarea", roseAccent, Color.WHITE) {
            if (txtTitle.text.isNotBlank()) {
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
                dialog.dispose()
            }
        }

        btnActions.add(btnCancel)
        btnActions.add(btnSave)

        dialog.add(form, BorderLayout.CENTER)
        dialog.add(btnActions, BorderLayout.SOUTH)
        dialog.isVisible = true
    }

    private fun completeSelectedTask() {
        val list = if (currentTab == 0) pendingTaskList else completedTaskList
        val selected = list.selectedValue ?: return
        val now = System.currentTimeMillis()
        
        if (currentTab == 0) {
            val completed = selected.copy(
                status = "completed",
                updatedAt = now,
                syncVersion = selected.syncVersion + 1
            )
            dbManager.insertOrUpdateTask(completed)
        } else {
            // Reabrir tarea
            val reopened = selected.copy(
                status = "pending",
                updatedAt = now,
                syncVersion = selected.syncVersion + 1
            )
            val recalculated = priorityEngine.calculatePriority(reopened, now)
            dbManager.insertOrUpdateTask(recalculated)
        }
        refreshTasks()
    }

    private fun boostSelectedTask(amount: Double) {
        val selected = pendingTaskList.selectedValue ?: return
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

    private fun deleteSelectedTask() {
        val list = if (currentTab == 0) pendingTaskList else completedTaskList
        val selected = list.selectedValue ?: return
        val now = System.currentTimeMillis()
        val deleted = selected.copy(
            isDeleted = true,
            deletedAt = now,
            updatedAt = now,
            syncVersion = selected.syncVersion + 1
        )
        dbManager.insertOrUpdateTask(deleted)
        refreshTasks()
    }

    private fun exportBackup() {
        val fileChooser = JFileChooser()
        fileChooser.selectedFile = File("primafocus_backup_${LocalDate.now()}.json")
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            val json = syncServer.exportBackupJson()
            fileChooser.selectedFile.writeText(json, Charsets.UTF_8)
            JOptionPane.showMessageDialog(frame, "Copia de respaldo exportada exitosamente.", "Exportar JSON", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    private fun importBackup() {
        val fileChooser = JFileChooser()
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            val json = fileChooser.selectedFile.readText(Charsets.UTF_8)
            val merged = syncServer.importBackupJson(json)
            refreshTasks()
            JOptionPane.showMessageDialog(frame, "Respaldo importado: $merged cambios integrados.", "Restaurar JSON", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    private fun openSyncDialog() {
        val dialog = JDialog(frame, "Sincronización LAN — Modo Anfitrión / Cliente", true)
        dialog.layout = BorderLayout()
        dialog.setSize(540, 520)
        dialog.setLocationRelativeTo(frame)

        val mainPanel = JPanel(BorderLayout(0, 16)).apply {
            background = bgPlumCenter
            border = EmptyBorder(20, 24, 20, 24)
        }

        // Mode Switcher Header (Anfitrión / Cliente)
        val modeCardsLayout = CardLayout()
        val modeCardsPanel = JPanel(modeCardsLayout).apply { isOpaque = false }

        val modeSelectorPanel = JPanel(GridLayout(1, 2, 8, 0)).apply {
            isOpaque = false
            preferredSize = Dimension(480, 42)
        }

        val btnModeHost = JButton("Modo Anfitrión (Servidor)").apply {
            font = Font("Segoe UI", Font.BOLD, 13)
        }
        val btnModeClient = JButton("Modo Cliente (Conectar)").apply {
            font = Font("Segoe UI", Font.BOLD, 13)
        }

        styleTabButton(btnModeHost, isSelected = true)
        styleTabButton(btnModeClient, isSelected = false)

        btnModeHost.addActionListener {
            styleTabButton(btnModeHost, isSelected = true)
            styleTabButton(btnModeClient, isSelected = false)
            modeCardsLayout.show(modeCardsPanel, "HOST")
        }

        btnModeClient.addActionListener {
            styleTabButton(btnModeHost, isSelected = false)
            styleTabButton(btnModeClient, isSelected = true)
            modeCardsLayout.show(modeCardsPanel, "CLIENT")
        }

        modeSelectorPanel.add(btnModeHost)
        modeSelectorPanel.add(btnModeClient)

        // --- HOST PANEL ---
        val hostPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        val lblHostDesc = JLabel("<html>La PC actúa como servidor en la red local. Ingrese estos datos en la app de Android para sincronizar.</html>").apply {
            foreground = textMuted
            font = Font("Segoe UI", Font.PLAIN, 13)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val serverStatusLabel = JLabel(if (syncServer.isRunning) "🟢 Servidor LAN Activo" else "🔴 Servidor LAN Pausado").apply {
            foreground = if (syncServer.isRunning) accentSage else errorRose
            font = Font("Segoe UI", Font.BOLD, 14)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val currentIp = syncServer.getLocalIpAddress()
        val currentPort = syncServer.activePort

        val ipBox = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val lblIpTitle = JLabel("IP Local:").apply { foreground = textPrimary; font = Font("Segoe UI", Font.BOLD, 13) }
        val txtIpVal = JTextField("$currentIp:$currentPort").apply {
            isEditable = false
            background = Color(0x19, 0x0E, 0x1D)
            foreground = Color.WHITE
            font = Font("Segoe UI", Font.BOLD, 13)
            border = EmptyBorder(6, 10, 6, 10)
        }
        val btnCopyIp = createGlassButton("Copiar", Color(0xFF, 0xFF, 0xFF, 0x20), textPrimary) {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection("$currentIp:$currentPort"), null)
            JOptionPane.showMessageDialog(dialog, "Dirección IP copiada: $currentIp:$currentPort", "Copiado", JOptionPane.INFORMATION_MESSAGE)
        }
        ipBox.add(lblIpTitle)
        ipBox.add(txtIpVal)
        ipBox.add(btnCopyIp)

        // PIN Display
        val pinBox = JPanel(FlowLayout(FlowLayout.LEFT, 12, 6)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val lblPinTitle = JLabel("PIN de Seguridad:").apply { foreground = textPrimary; font = Font("Segoe UI", Font.BOLD, 13) }
        val lblPinValue = JLabel(syncServer.currentPin).apply {
            foreground = roseGlow
            font = Font("Segoe UI", Font.BOLD, 22)
            isOpaque = true
            background = Color(0x3B, 0x1D, 0x40)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primaryRose, 1, true),
                EmptyBorder(6, 16, 6, 16)
            )
        }
        val btnNewPin = createGlassButton("Nuevo PIN", Color(0xFF, 0xFF, 0xFF, 0x20), textPrimary) {
            val newPin = syncServer.generateNewPin()
            lblPinValue.text = newPin
        }
        pinBox.add(lblPinTitle)
        pinBox.add(lblPinValue)
        pinBox.add(btnNewPin)

        val btnToggleServer = createGlassButton(if (syncServer.isRunning) "Pausar Servidor" else "Iniciar Servidor", Color(0xFF, 0xFF, 0xFF, 0x20), textPrimary) {
            if (syncServer.isRunning) {
                syncServer.stop()
                serverStatusLabel.text = "🔴 Servidor LAN Pausado"
                serverStatusLabel.foreground = errorRose
            } else {
                syncServer.start()
                serverStatusLabel.text = "🟢 Servidor LAN Activo"
                serverStatusLabel.foreground = accentSage
            }
        }.apply { alignmentX = Component.LEFT_ALIGNMENT }

        hostPanel.add(lblHostDesc)
        hostPanel.add(Box.createVerticalStrut(12))
        hostPanel.add(serverStatusLabel)
        hostPanel.add(Box.createVerticalStrut(12))
        hostPanel.add(ipBox)
        hostPanel.add(Box.createVerticalStrut(8))
        hostPanel.add(pinBox)
        hostPanel.add(Box.createVerticalStrut(12))
        hostPanel.add(btnToggleServer)

        // --- CLIENT PANEL ---
        val clientPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        val lblClientDesc = JLabel("<html>Conecte este companion a otro nodo o servidor ingresando su dirección IP y PIN.</html>").apply {
            foreground = textMuted
            font = Font("Segoe UI", Font.PLAIN, 13)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val targetIpField = JTextField("192.168.1.").apply {
            background = Color(0x19, 0x0E, 0x1D)
            foreground = textPrimary
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(glassBorder, 1, true),
                EmptyBorder(8, 10, 8, 10)
            )
            font = Font("Segoe UI", Font.PLAIN, 13)
            maximumSize = Dimension(Integer.MAX_VALUE, 38)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val targetPortField = JTextField("8765").apply {
            background = Color(0x19, 0x0E, 0x1D)
            foreground = textPrimary
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(glassBorder, 1, true),
                EmptyBorder(8, 10, 8, 10)
            )
            font = Font("Segoe UI", Font.PLAIN, 13)
            maximumSize = Dimension(Integer.MAX_VALUE, 38)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val targetPinField = JTextField().apply {
            background = Color(0x19, 0x0E, 0x1D)
            foreground = textPrimary
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(glassBorder, 1, true),
                EmptyBorder(8, 10, 8, 10)
            )
            font = Font("Segoe UI", Font.PLAIN, 13)
            maximumSize = Dimension(Integer.MAX_VALUE, 38)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val clientStatusLabel = JLabel("Listo para conectar.").apply {
            foreground = textMuted
            font = Font("Segoe UI", Font.PLAIN, 12)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val btnConnectAndSync = createGlassButton("Conectar y Sincronizar", roseAccent, Color.WHITE) {
            val host = targetIpField.text.trim()
            val port = targetPortField.text.trim().toIntOrNull() ?: 8765
            val pin = targetPinField.text.trim()

            if (host.isBlank() || pin.isBlank()) {
                clientStatusLabel.text = "Por favor ingrese IP y PIN válidos."
                clientStatusLabel.foreground = errorRose
                return@createGlassButton
            }

            clientStatusLabel.text = "Conectando con $host:$port..."
            clientStatusLabel.foreground = roseGlow

            Thread {
                try {
                    val client = LanSyncClient()
                    val localTasks = dbManager.getAllTasks()
                    val localSessions = dbManager.getAllSessions()
                    val packet = LANSyncPacket(
                        deviceId = UUID.randomUUID().toString(),
                        deviceName = "PrimaFocus Desktop Client",
                        tasks = localTasks,
                        sessions = localSessions
                    )

                    val result = kotlinx.coroutines.runBlocking {
                        client.executeFullSync(host, port, pin, packet)
                    }

                    result.fold(
                        onSuccess = { remotePacket ->
                            val changes = dbManager.mergeSyncPayload(remotePacket.tasks, remotePacket.sessions)
                            refreshTasks()
                            SwingUtilities.invokeLater {
                                clientStatusLabel.text = "✓ Sincronización exitosa ($changes entidades actualizadas)."
                                clientStatusLabel.foreground = accentSage
                                JOptionPane.showMessageDialog(dialog, "Sincronización exitosa. Se integraron $changes cambios.", "Éxito", JOptionPane.INFORMATION_MESSAGE)
                            }
                        },
                        onFailure = { err ->
                            SwingUtilities.invokeLater {
                                clientStatusLabel.text = "Error: ${err.message}"
                                clientStatusLabel.foreground = errorRose
                            }
                        }
                    )
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        clientStatusLabel.text = "Fallo de conexión: ${e.message}"
                        clientStatusLabel.foreground = errorRose
                    }
                }
            }.start()
        }.apply { alignmentX = Component.LEFT_ALIGNMENT }

        clientPanel.add(lblClientDesc)
        clientPanel.add(Box.createVerticalStrut(10))
        clientPanel.add(JLabel("IP de Destino (Host):").apply { foreground = textPrimary; font = Font("Segoe UI", Font.BOLD, 12); alignmentX = Component.LEFT_ALIGNMENT })
        clientPanel.add(Box.createVerticalStrut(4))
        clientPanel.add(targetIpField)
        clientPanel.add(Box.createVerticalStrut(8))
        clientPanel.add(JLabel("Puerto:").apply { foreground = textPrimary; font = Font("Segoe UI", Font.BOLD, 12); alignmentX = Component.LEFT_ALIGNMENT })
        clientPanel.add(Box.createVerticalStrut(4))
        clientPanel.add(targetPortField)
        clientPanel.add(Box.createVerticalStrut(8))
        clientPanel.add(JLabel("PIN de 6 dígitos:").apply { foreground = textPrimary; font = Font("Segoe UI", Font.BOLD, 12); alignmentX = Component.LEFT_ALIGNMENT })
        clientPanel.add(Box.createVerticalStrut(4))
        clientPanel.add(targetPinField)
        clientPanel.add(Box.createVerticalStrut(12))
        clientPanel.add(btnConnectAndSync)
        clientPanel.add(Box.createVerticalStrut(8))
        clientPanel.add(clientStatusLabel)

        modeCardsPanel.add(hostPanel, "HOST")
        modeCardsPanel.add(clientPanel, "CLIENT")

        mainPanel.add(modeSelectorPanel, BorderLayout.NORTH)
        mainPanel.add(modeCardsPanel, BorderLayout.CENTER)

        val bottomBar = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            background = bgDarkEdge
            border = EmptyBorder(8, 16, 8, 16)
        }
        val btnClose = createGlassButton("Cerrar", Color(0xFF, 0xFF, 0xFF, 0x20), textMuted) {
            dialog.dispose()
        }
        bottomBar.add(btnClose)

        dialog.add(mainPanel, BorderLayout.CENTER)
        dialog.add(bottomBar, BorderLayout.SOUTH)
        dialog.isVisible = true
    }

    // --- CELL RENDERER IDENTICO A MOBILE TASKLISTITEM ---
    private inner class MobileTaskRenderer(private val isHistory: Boolean) : ListCellRenderer<Task> {
        override fun getListCellRendererComponent(
            list: JList<out Task>?,
            value: Task?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val card = object : JPanel(BorderLayout(14, 0)) {
                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = if (isSelected) glassSurfaceSelected else glassSurface
                    g2.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 16f, 16f))
                    
                    if (isSelected) {
                        g2.color = roseAccent
                        g2.stroke = BasicStroke(1.5f)
                    } else {
                        g2.color = glassBorder
                        g2.stroke = BasicStroke(1f)
                    }
                    g2.draw(RoundRectangle2D.Float(0f, 0f, width - 1f, height - 1f, 16f, 16f))
                }
            }.apply {
                isOpaque = false
                border = EmptyBorder(14, 18, 14, 18)
            }

            if (value == null) return card

            val rank = index + 1
            val leftBox = JPanel(FlowLayout(FlowLayout.LEFT, 12, 0)).apply { isOpaque = false }

            val band = PriorityBand.fromScore(value.priorityScore)
            val priorityText = band.label
            val priorityColor = when (band) {
                PriorityBand.URGENT -> errorRose
                PriorityBand.HIGH -> primaryRose
                PriorityBand.LOW -> textMuted
                PriorityBand.NORMAL -> Color(0xFF, 0xFF, 0xFF, 0xB0)
            }

            val checkIcon = JLabel(if (isHistory) "✓" else "○").apply {
                foreground = if (isHistory) accentSage else primaryRose
                font = Font("Segoe UI", Font.BOLD, 18)
            }

            val titleBox = JPanel(GridLayout(0, 1, 0, 2)).apply { isOpaque = false }
            val titleText = JLabel(value.title).apply {
                foreground = textPrimary
                font = Font("Segoe UI", Font.BOLD, 15)
            }
            val subtitleInfo = JLabel("Prioridad $priorityText  •  ${value.category.replaceFirstChar { it.uppercase() }}").apply {
                foreground = priorityColor
                font = Font("Segoe UI", Font.PLAIN, 12)
            }
            titleBox.add(titleText)
            titleBox.add(subtitleInfo)

            leftBox.add(checkIcon)
            leftBox.add(titleBox)

            val rightBox = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0)).apply { isOpaque = false }

            // Category Badge
            val catBadge = JLabel(value.category.uppercase()).apply {
                foreground = primaryRose
                background = Color(0x3B, 0x1D, 0x40)
                font = Font("Segoe UI", Font.BOLD, 11)
                isOpaque = true
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primaryRose.darker(), 1, true),
                    EmptyBorder(4, 10, 4, 10)
                )
            }

            // Score Badge
            val scoreBadge = JLabel("%.1f".format(value.priorityScore)).apply {
                foreground = if (value.priorityScore >= 40) roseGlow else textMuted
                font = Font("Segoe UI", Font.BOLD, 13)
            }

            rightBox.add(catBadge)
            if (value.manualBoost != 0.0) {
                val boostBadge = JLabel("${if (value.manualBoost > 0) "+" else ""}${value.manualBoost.toInt()}").apply {
                    foreground = if (value.manualBoost > 0) accentSage else errorRose
                    font = Font("Segoe UI", Font.BOLD, 12)
                }
                rightBox.add(boostBadge)
            }
            rightBox.add(scoreBadge)

            card.add(leftBox, BorderLayout.WEST)
            card.add(rightBox, BorderLayout.EAST)

            val wrapper = JPanel(BorderLayout()).apply {
                isOpaque = false
                border = EmptyBorder(4, 0, 4, 0)
                add(card, BorderLayout.CENTER)
            }

            return wrapper
        }
    }
}

