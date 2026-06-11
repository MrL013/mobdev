package data

import android.content.Context

class AppContainer(context: Context) {
    private val sessionManager = SessionManager(context.applicationContext)
    private val cache = ChatCache(context.applicationContext)
    private val networkMonitor = NetworkMonitor(context.applicationContext)
    private val api = ChatApiFactory(sessionManager).createApi()
    private val wsClient = WsClient(sessionManager)
    val repository = ChatRepository(api, wsClient, sessionManager, cache, networkMonitor)
    val session = sessionManager
}


