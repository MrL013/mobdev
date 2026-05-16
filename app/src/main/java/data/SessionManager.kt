package data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var login: String?
        get() = prefs.getString("login", null)
        set(value) = prefs.edit().putString("login", value).apply()

    var password: String?
        get() = prefs.getString("password", null)
        set(value) = prefs.edit().putString("password", value).apply()

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    fun clearAuth() {
        prefs.edit().remove("token").apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}


