package data

import android.content.Context

class AppContainer(context: Context) {
    private val sessionManager = SessionManager(context.applicationContext)
    private val api = ApiFactory(sessionManager).createApi()
    private val wsClient = WsClient(sessionManager)
    val repository = ChatRepository(api, wsClient, sessionManager)
    val session = sessionManager
}


