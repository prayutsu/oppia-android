package org.oppia.app.application

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.multidex.MultiDexApplication
import org.oppia.app.activity.ActivityComponent
import org.oppia.domain.oppialogger.exceptions.ExceptionsController
import org.oppia.domain.oppialogger.exceptions.OppiaUncaughtExceptionHandler
import org.oppia.util.system.OppiaClock
import javax.inject.Inject

/** The root [Application] of the Oppia app. */
class OppiaApplication : MultiDexApplication(), ActivityComponentFactory {
  /** The root [ApplicationComponent]. */

  @Inject lateinit var exceptionsController: ExceptionsController
  @Inject lateinit var oppiaClock: OppiaClock

  private val component: ApplicationComponent by lazy {
    DaggerApplicationComponent.builder()
      .setApplication(this)
      .build()
  }

  override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
    setOppiaUncaughtExceptionHandler()
    return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
  }

  /** Sets the uncaughtExceptionHandler of the current thread to [OppiaUncaughtExceptionHandler]. */
  private fun setOppiaUncaughtExceptionHandler() {
    try {
      Thread.currentThread().uncaughtExceptionHandler =
        OppiaUncaughtExceptionHandler(exceptionsController, oppiaClock)
    } catch (exception: Exception) { }
    finally {
      Thread.currentThread().uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    }
  }
}
