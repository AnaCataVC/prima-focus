package com.ancata.prima_focus.desktop

import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager
import com.ancata.prima_focus.desktop.sync.DesktopSyncServer
import com.ancata.prima_focus.desktop.ui.DesktopMainView
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    // P0: Redirect all output to a persistent log file (javaw.exe has no console)
    val appDataDir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "PrimaFocus")
    if (!appDataDir.exists()) appDataDir.mkdirs()
    val logFile = File(appDataDir, "launch.log")
    try {
        val logStream = PrintStream(logFile, "UTF-8")
        System.setOut(logStream)
        System.setErr(logStream)
    } catch (_: Exception) { /* If log file fails, continue anyway */ }

    println("[Startup] Prima-Focus Desktop starting. Log: ${logFile.absolutePath}")

    // P0: Proper uncaught exception handler -- do NOT set sun.awt.exception.handler
    Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
        System.err.println("[FATAL] Uncaught exception on thread '${thread.name}': ${ex.message}")
        ex.printStackTrace(System.err)
    }

    // Headless check -- required before any AWT/Swing call
    if (GraphicsEnvironment.isHeadless()) {
        System.err.println("[FATAL] Headless environment detected. Cannot open GUI.")
        return
    }

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        println("[Startup] Look and Feel set to system default.")
    } catch (e: Exception) {
        System.err.println("[WARN] Could not set system L&F: ${e.message}")
    }

    val dbManager: DesktopDatabaseManager
    try {
        dbManager = DesktopDatabaseManager()
        println("[Startup] Database initialized successfully.")
    } catch (ex: Throwable) {
        System.err.println("[FATAL] Database initialization failed: ${ex.message}")
        ex.printStackTrace(System.err)
        JOptionPane.showMessageDialog(
            null,
            "Error al inicializar la base de datos:\n${ex.message}\n\nRevisa el log: ${logFile.absolutePath}",
            "Error Critico -- Prima-Focus",
            JOptionPane.ERROR_MESSAGE
        )
        return
    }

    val viewRef = AtomicReference<DesktopMainView>()

    val syncServer = DesktopSyncServer(
        dbManager = dbManager,
        initialPort = 8765,
        onSyncCompleted = { changesCount ->
            println("[SyncServer] Sync completed. $changesCount entities processed.")
            viewRef.get()?.refreshTasks()
        }
    )

    try {
        syncServer.start()
        println("[Startup] LAN server started on port ${syncServer.activePort}. PIN: ${syncServer.currentPin}")
    } catch (ex: Exception) {
        System.err.println("[WARN] Could not start sync server: ${ex.message}. Continuing without sync.")
    }

    // P0: Capture EDT throwable via variable -- use Throwable, not just Exception
    var edtThrowable: Throwable? = null

    try {
        SwingUtilities.invokeAndWait {
            try {
                println("[EDT] Creating main window...")
                val view = DesktopMainView(dbManager, syncServer)
                viewRef.set(view)
                view.show()
                println("[EDT] Main window shown successfully.")
            } catch (ex: Throwable) {
                edtThrowable = ex
                System.err.println("[FATAL] EDT UI init failed: ${ex.message}")
                ex.printStackTrace(System.err)
            }
        }
    } catch (ex: java.lang.reflect.InvocationTargetException) {
        edtThrowable = ex.cause ?: ex
        System.err.println("[FATAL] invokeAndWait InvocationTargetException: ${edtThrowable?.message}")
        edtThrowable?.printStackTrace(System.err)
    } catch (ex: InterruptedException) {
        System.err.println("[FATAL] invokeAndWait interrupted: ${ex.message}")
        ex.printStackTrace(System.err)
    }

    // Show error dialog if EDT failed -- visible to the user even with javaw.exe
    edtThrowable?.let { ex ->
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                null,
                "Error al abrir Prima-Focus:\n${ex.message}\n\nRevisa el log en:\n${logFile.absolutePath}",
                "Error de Inicio -- Prima-Focus",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}
