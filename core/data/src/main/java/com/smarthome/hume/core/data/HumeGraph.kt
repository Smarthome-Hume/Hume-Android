package com.smarthome.hume.core.data

import android.content.Context
import com.smarthome.hume.core.datastore.SessionStore

/**
 * Service locator don gian cho kien truc multi-module.
 * :app khoi tao 1 lan trong Application.onCreate(); cac feature lay qua get().
 * (Sau nay co the thay bang Hilt ma khong doi API cac feature.)
 */
class HumeGraph private constructor(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore: SessionStore by lazy { SessionStore(appContext) }
    val authRepository: AuthRepository by lazy { AuthRepository(sessionStore) }

    companion object {
        @Volatile
        private var instance: HumeGraph? = null

        fun init(context: Context): HumeGraph =
            instance ?: synchronized(this) {
                instance ?: HumeGraph(context).also { instance = it }
            }

        fun get(): HumeGraph =
            instance ?: error("HumeGraph.init() chua duoc goi trong Application.onCreate()")
    }
}
