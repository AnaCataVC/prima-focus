package com.ancata.prima_focus.desktop

import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager
import com.ancata.prima_focus.desktop.sync.DesktopSyncServer
import com.ancata.prima_focus.desktop.ui.DesktopMainView
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

fun main() {
    try {
        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        // Fallback to default L&F
    }

    val dbManager = DesktopDatabaseManager()
    val viewRef = AtomicReference<DesktopMainView>()

    val syncServer = DesktopSyncServer(
        dbManager = dbManager,
        initialPort = 8765,
        onSyncCompleted = { changesCount ->
            println("[SyncServer] Sync completed successfully. $changesCount entities processed.")
            viewRef.get()?.refreshTasks()
        }
    )

    syncServer.start()
    println("[DesktopApp] Prima-Focus LAN server started on port ${syncServer.activePort}.")
    println("[DesktopApp] Pairing PIN: ${syncServer.currentPin}")

    SwingUtilities.invokeLater {
        val view = DesktopMainView(dbManager, syncServer)
        viewRef.set(view)
        view.show()
    }
}

