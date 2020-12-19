package dev.bwt

class NativeBitcoinWalletTracker {
    init {
        System.loadLibrary("bwt")
    }

    external fun helloWorld(x: String): String;
    external fun start(jsonConfig: String, callback: CallbackNotifier): Long;
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