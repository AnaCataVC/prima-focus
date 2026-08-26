package com.ancata.prima_focus.desktop

import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager
import com.ancata.prima_focus.desktop.sync.DesktopSyncServer
import com.ancata.prima_focus.desktop.ui.DesktopMainView
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

fun main() {
    val dbManager = DesktopDatabaseManager()
    val viewRef = AtomicReference<DesktopMainView>()

    val syncServer = DesktopSyncServer(
        dbManager = dbManager,
        port = 8765,
        onSyncCompleted = { changesCount ->
            println("[SyncServer] Sync completed successfully. $changesCount entities processed.")
            viewRef.get()?.refreshTasks()
        }
    )

    syncServer.start()
    println("[DesktopApp] Prima-Focus LAN server started on port 8765.")
    println("[DesktopApp] Pairing PIN: ${syncServer.currentPin}")


    SwingUtilities.invokeLater {
        val view = DesktopMainView(dbManager, syncServer)
        viewRef.set(view)
        view.show()
    }
}
