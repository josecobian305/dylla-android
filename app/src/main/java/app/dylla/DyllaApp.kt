package app.dylla

import android.app.Application

class DyllaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DyllaApp
            private set
    }
}
