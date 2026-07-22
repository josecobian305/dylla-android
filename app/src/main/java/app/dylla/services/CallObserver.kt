package app.dylla.services

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

class CallObserver {

    var onCallEnded: (() -> Unit)? = null

    private var telephonyManager: TelephonyManager? = null
    private var listener: PhoneStateListener? = null
    private var wasOffHook = false

    fun startObserving(context: Context) {
        stopObserving()

        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in API 31")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        wasOffHook = true
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (wasOffHook) {
                            wasOffHook = false
                            onCallEnded?.invoke()
                        }
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {}
                }
            }
        }

        @Suppress("DEPRECATION")
        telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun stopObserving() {
        listener?.let { l ->
            @Suppress("DEPRECATION")
            telephonyManager?.listen(l, PhoneStateListener.LISTEN_NONE)
        }
        listener = null
        telephonyManager = null
        wasOffHook = false
    }
}
