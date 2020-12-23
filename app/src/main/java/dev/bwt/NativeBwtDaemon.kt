package dev.bwt

class NativeBwtDaemon {
    init {
        System.loadLibrary("bwt")
    }

    // Start the bwt daemon with the configured servers
    // Blocks until the initial indexing is completed and the servers are ready.
    // Returns a pointer to be used with shutdown()
    external fun start(jsonConfig: String, callback: CallbackNotifier): Long;

    // Shutdown thw bwt daemon
    external fun shutdown(shutdownPtr: Long);
}

interface CallbackNotifier {
    fun onBooting() {};
    fun onSyncProgress(progress: Float, tip: Int) {};
    fun onScanProgress(progress: Float, eta: Int) {};
    fun onElectrumReady(addr: String) {};
    fun onHttpReady(addr: String) {};
    fun onReady() {};
}