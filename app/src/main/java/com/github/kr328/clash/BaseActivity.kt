package com.github.kr328.clash

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.common.compat.isAllowForceDarkCompat
import com.github.kr328.clash.common.compat.isLightNavigationBarCompat
import com.github.kr328.clash.common.compat.isLightStatusBarsCompat
import com.github.kr328.clash.common.compat.isSystemBarsTranslucentCompat
import com.github.kr328.clash.core.bridge.ClashException
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.ui.DayNight
import com.github.kr328.clash.design.util.resolveThemedBoolean
import com.github.kr328.clash.design.util.resolveThemedColor
import com.github.kr328.clash.design.util.showExceptionToast
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.util.ActivityResultLifecycle
import com.github.kr328.clash.util.ApplicationObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BaseActivity<D : Design<*>> : AppCompatActivity(),
    CoroutineScope by MainScope(),
    Broadcasts.Observer {
    
    protected val uiStore by lazy { UiStore(this) }
    protected val events = Channel<Event>(Channel.UNLIMITED)
    protected var activityStarted: Boolean = false
    protected val clashRunning: Boolean
        get() = Remote.broadcasts.clashRunning
    protected var design: D? = null
        set(value) {
            field = value
            if (value != null) {
                setContentView(value.root)
            } else {
                setContentView(View(this))
            }
        }

    private var defer: suspend () -> Unit = {}
    private var deferRunning = false
    private val nextRequestKey = AtomicInteger(0)
    private var dayNight: DayNight = DayNight.Day

    protected abstract suspend fun main()

    fun defer(operation: suspend () -> Unit) {
        this.defer = operation
    }

    suspend fun <I, O> startActivityForResult(
        contracts: ActivityResultContract<I, O>,
        input: I,
    ): O = withContext(Dispatchers.Main) {
        val requestKey = nextRequestKey.getAndIncrement().toString()

        ActivityResultLifecycle().use { lifecycle, start ->
            suspendCoroutine { c ->
                activityResultRegistry.register(requestKey, lifecycle, contracts) {
                    c.resume(it)
                }.apply { start() }.launch(input)
            }
        }
    }

    suspend fun setContentDesign(design: D) {
        suspendCoroutine<Unit> {
            window.decorView.post {
                this.design = design
                it.resume(Unit)
            }
        }
    }

        override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (com.github.kr328.clash.design.PreferenceManager.darkMode == "dark") {
            ThemeHelper.apply(window.decorView, true)
        }
    }

    override fun setContentView(view: android.view.View?) {
        super.setContentView(view)
        if (com.github.kr328.clash.design.PreferenceManager.darkMode == "dark") {
            ThemeHelper.apply(window.decorView, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDayNight()
        super.onCreate(savedInstanceState)
        applySystemBars()

        launch {
            try {
                main()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                com.github.kr328.clash.common.log.Log.e("BaseActivity main crash", e)
                try {
                    design?.showExceptionToast(e.message ?: "操作失败，请重试")
                } catch (_: Throwable) {}
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activityStarted = true
        Remote.broadcasts.addObserver(this)
        events.trySend(Event.ActivityStart)
    }

    override fun onStop() {
        super.onStop()
        activityStarted = false
        Remote.broadcasts.removeObserver(this)
        events.trySend(Event.ActivityStop)
    }

    override fun onDestroy() {
        design?.cancel()
        cancel()
        super.onDestroy()
    }

    override fun finish() {
        if (deferRunning) return
        deferRunning = true

        launch {
            try {
                defer()
            } finally {
                withContext(NonCancellable) {
                    super.finish()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (queryDayNight(newConfig) != dayNight) {
            ApplicationObserver.createdActivities.forEach {
                it.recreate()
            }
        }
    }

    open fun shouldDisplayHomeAsUpEnabled(): Boolean {
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        this.onBackPressed()
        return true
    }

    override fun onProfileChanged() {
        events.trySend(Event.ProfileChanged)
    }

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        events.trySend(Event.ProfileUpdateCompleted)
    }

    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        events.trySend(Event.ProfileUpdateFailed)
    }

    override fun onProfileLoaded() {
        events.trySend(Event.ProfileLoaded)
    }

    override fun onServiceRecreated() {
        events.trySend(Event.ServiceRecreated)
    }

    override fun onStarted() {
        events.trySend(Event.ClashStart)
    }

    override fun onStopped(cause: String?) {
        events.trySend(Event.ClashStop)

        if (cause != null && activityStarted) {
            launch {
                design?.showExceptionToast(ClashException(cause))
            }
        }
    }

    override fun onConnectionStateChanged(state: com.github.kr328.clash.design.state.ConnectionState) {
        events.trySend(Event.ConnectionStateChanged(state))
    }

    private fun queryDayNight(config: Configuration = resources.configuration): DayNight {
        return when (uiStore.darkMode) {
            DarkMode.Auto -> if (config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) DayNight.Night else DayNight.Day
            DarkMode.ForceLight -> DayNight.Day
            DarkMode.ForceDark -> DayNight.Night
        }
    }

    private fun applyDayNight(config: Configuration = resources.configuration) {
        // 主题样式部分（必须在 super.onCreate 之前调用）
        val mode = com.github.kr328.clash.design.PreferenceManager.darkMode
        val dayNight = when (mode) {
            "dark" -> DayNight.Night
            "auto" -> if (config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) DayNight.Night else DayNight.Day
            else -> DayNight.Day
        }
        try {
            when (dayNight) {
                DayNight.Night -> theme.applyStyle(R.style.AppThemeDark, true)
                DayNight.Day -> theme.applyStyle(R.style.AppThemeLight, true)
            }
        } catch (_: Throwable) {}
        this.dayNight = dayNight
    }

    private fun applySystemBars() {
        window.isAllowForceDarkCompat = false
        window.isSystemBarsTranslucentCompat = true
        try {
            window.statusBarColor = resolveThemedColor(android.R.attr.statusBarColor)
            window.navigationBarColor = resolveThemedColor(android.R.attr.navigationBarColor)
        } catch (_: Throwable) {}
        if (Build.VERSION.SDK_INT >= 23) {
            try { window.isLightStatusBarsCompat = resolveThemedBoolean(android.R.attr.windowLightStatusBar) } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= 27) {
            try { window.isLightNavigationBarCompat = resolveThemedBoolean(android.R.attr.windowLightNavigationBar) } catch (_: Throwable) {}
        }
    }


    sealed class Event {
        object ServiceRecreated : Event()
        object ActivityStart : Event()
        object ActivityStop : Event()
        object ClashStop : Event()
        object ClashStart : Event()
        object ProfileLoaded : Event()
        object ProfileChanged : Event()
        object ProfileUpdateCompleted : Event()
        object ProfileUpdateFailed : Event()
        data class ConnectionStateChanged(
            val state: com.github.kr328.clash.design.state.ConnectionState
        ) : Event()
    }
}
