package dev.bwt

class NativeBitcoinWalletTracker {
    init {
        System.loadLibrary("bwt")
    }

    external fun helloWorld(x: String): String;
    external fun start(json_config: String, callback: CallbackNotifier): Long;
    external fun shutdown(shutdown_ptr: Long);
}

interface CallbackNotifier {
    fun booting();
    fun syncProgress(progress: Float, tip: Int);
    fun scanProgress(progress: Float, eta: Int);
    fun electrumReady(addr: String);
    fun httpReady(addr: String);
    fun ready();
}