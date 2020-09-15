package org.oppia.app.player.exploration

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import dagger.Component
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.app.R
import org.oppia.app.activity.ActivityComponent
import org.oppia.app.application.ActivityComponentFactory
import org.oppia.app.application.ApplicationComponent
import org.oppia.app.application.ApplicationInjector
import org.oppia.app.application.ApplicationInjectorProvider
import org.oppia.app.application.ApplicationModule
import org.oppia.app.application.ApplicationStartupListenerModule
import org.oppia.app.help.HelpActivity
import org.oppia.app.options.OptionsActivity
import org.oppia.app.player.state.hintsandsolution.HintsAndSolutionConfigModule
import org.oppia.app.shim.ViewBindingShimModule
import org.oppia.app.testing.ExplorationInjectionActivity
import org.oppia.app.utility.EspressoTestsMatchers.withDrawable
import org.oppia.app.utility.OrientationChangeAction.Companion.orientationLandscape
import org.oppia.domain.classify.InteractionsModule
import org.oppia.domain.classify.rules.continueinteraction.ContinueModule
import org.oppia.domain.classify.rules.dragAndDropSortInput.DragDropSortInputModule
import org.oppia.domain.classify.rules.fractioninput.FractionInputModule
import org.oppia.domain.classify.rules.imageClickInput.ImageClickInputModule
import org.oppia.domain.classify.rules.itemselectioninput.ItemSelectionInputModule
import org.oppia.domain.classify.rules.multiplechoiceinput.MultipleChoiceInputModule
import org.oppia.domain.classify.rules.numberwithunits.NumberWithUnitsRuleModule
import org.oppia.domain.classify.rules.numericinput.NumericInputRuleModule
import org.oppia.domain.classify.rules.ratioinput.RatioInputModule
import org.oppia.domain.classify.rules.textinput.TextInputRuleModule
import org.oppia.domain.exploration.ExplorationDataController
import org.oppia.domain.onboarding.ExpirationMetaDataRetrieverModule
import org.oppia.domain.oppialogger.LogStorageModule
import org.oppia.domain.oppialogger.loguploader.LogUploadWorkerModule
import org.oppia.domain.oppialogger.loguploader.WorkManagerConfigurationModule
import org.oppia.domain.question.QuestionModule
import org.oppia.domain.topic.FRACTIONS_EXPLORATION_ID_0
import org.oppia.domain.topic.FRACTIONS_STORY_ID_0
import org.oppia.domain.topic.FRACTIONS_TOPIC_ID
import org.oppia.domain.topic.PrimeTopicAssetsControllerModule
import org.oppia.domain.topic.RATIOS_EXPLORATION_ID_0
import org.oppia.domain.topic.RATIOS_STORY_ID_0
import org.oppia.domain.topic.RATIOS_TOPIC_ID
import org.oppia.domain.topic.TEST_EXPLORATION_ID_2
import org.oppia.domain.topic.TEST_STORY_ID_0
import org.oppia.domain.topic.TEST_TOPIC_ID_0
import org.oppia.testing.TestAccessibilityModule
import org.oppia.testing.TestDispatcherModule
import org.oppia.testing.TestLogReportingModule
import org.oppia.util.caching.testing.CachingTestModule
import org.oppia.util.gcsresource.GcsResourceModule
import org.oppia.util.logging.LoggerModule
import org.oppia.util.logging.firebase.FirebaseLogUploaderModule
import org.oppia.util.networking.NetworkConnectionUtil
import org.oppia.util.parser.GlideImageLoaderModule
import org.oppia.util.parser.HtmlParserEntityTypeModule
import org.oppia.util.parser.ImageParsingModule
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/** Tests for [ExplorationActivity]. */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
  application = ExplorationActivityTest.TestApplication::class,
  qualifiers = "port-xxhdpi"
)
class ExplorationActivityTest {
  private lateinit var networkConnectionUtil: NetworkConnectionUtil
  private lateinit var explorationDataController: ExplorationDataController

  @Inject
  lateinit var context: Context

  private val internalProfileId: Int = 0

  @Before
  fun setUp() {
    Intents.init()
    setUpTestApplicationComponent()
    FirebaseApp.initializeApp(context)
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  private fun getApplicationDependencies(id: String) {
    launch(ExplorationInjectionActivity::class.java).use {
      it.onActivity { activity ->
        networkConnectionUtil = activity.networkConnectionUtil
        explorationDataController = activity.explorationDataController
        explorationDataController.startPlayingExploration(id)
      }
    }
  }

  // TODO(#163): Fill in remaining tests for this activity.
  @get:Rule
  var explorationActivityTestRule: ActivityTestRule<ExplorationActivity> = ActivityTestRule(
    ExplorationActivity::class.java, /* initialTouchMode= */ true, /* launchActivity= */ false
  )

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testExploration_toolbarTitle_isDisplayedSuccessfully() {
    getApplicationDependencies(TEST_EXPLORATION_ID_2)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        TEST_TOPIC_ID_0,
        TEST_STORY_ID_0,
        TEST_EXPLORATION_ID_2
      )
    ).use {
      waitForTheView(withText("Prototype Exploration"))
      onView(withId(R.id.exploration_toolbar_title))
        .check(matches(withText("Prototype Exploration")))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testExploration_configurationChange_toolbarTitle_isDisplayedSuccessfully() {
    getApplicationDependencies(TEST_EXPLORATION_ID_2)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        TEST_TOPIC_ID_0,
        TEST_STORY_ID_0,
        TEST_EXPLORATION_ID_2
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      waitForTheView(withText("Prototype Exploration"))
      onView(withId(R.id.exploration_toolbar_title))
        .check(matches(withText("Prototype Exploration")))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testExploration_overflowMenu_isDisplayedSuccessfully() {
    getApplicationDependencies(TEST_EXPLORATION_ID_2)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        TEST_TOPIC_ID_0,
        TEST_STORY_ID_0,
        TEST_EXPLORATION_ID_2
      )
    ).use {
      openActionBarOverflowOrOptionsMenu(context)
      onView(withText(context.getString(R.string.menu_options))).check(matches(isDisplayed()))
      onView(withText(context.getString(R.string.help))).check(matches(isDisplayed()))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testExploration_openOverflowMenu_selectHelpInOverflowMenu_opensHelpActivity() {
    getApplicationDependencies(TEST_EXPLORATION_ID_2)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        TEST_TOPIC_ID_0,
        TEST_STORY_ID_0,
        TEST_EXPLORATION_ID_2
      )
    ).use {
      openActionBarOverflowOrOptionsMenu(context)
      onView(withText(context.getString(R.string.help))).perform(click())
      intended(hasComponent(HelpActivity::class.java.name))
      intended(hasExtra(HelpActivity.BOOL_IS_FROM_NAVIGATION_DRAWER_EXTRA_KEY, /* value= */ false))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testExploration_openOverflowMenu_selectOptionsInOverflowMenu_opensOptionsActivity() {
    getApplicationDependencies(TEST_EXPLORATION_ID_2)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        TEST_TOPIC_ID_0,
        TEST_STORY_ID_0,
        TEST_EXPLORATION_ID_2
      )
    ).use {
      openActionBarOverflowOrOptionsMenu(context)
      onView(withText(context.getString(R.string.menu_options))).perform(click())
      intended(hasComponent(OptionsActivity::class.java.name))
      intended(
        hasExtra(
          OptionsActivity.BOOL_IS_FROM_NAVIGATION_DRAWER_EXTRA_KEY,
          /* value= */ false
        )
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  fun testAudioWithNoVoiceover_openPrototypeExploration_checkAudioButtonIsHidden() {
    getApplicationDependencies(TEST_EXPLORATION_ID_2)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        TEST_TOPIC_ID_0,
        TEST_STORY_ID_0,
        TEST_EXPLORATION_ID_2
      )
    ).use {
      onView(withId(R.id.action_audio_player)).check(matches(not(isDisplayed())))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  fun testAudioWithNoVoiceover_openPrototypeExploration_configurationChange_checkAudioButtonIsHidden() { // ktlint-disable max-line-length
    getApplicationDependencies(TEST_EXPLORATION_ID_2)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        TEST_TOPIC_ID_0,
        TEST_STORY_ID_0,
        TEST_EXPLORATION_ID_2
      )
    ).use {
      onView(isRoot()).perform(orientationLandscape())
      onView(withId(R.id.action_audio_player)).check(matches(not(isDisplayed())))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithNoConnection_openRatioExploration_clickAudioIcon_checkOpensNoConnectionDialog() {
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(NetworkConnectionUtil.ConnectionStatus.NONE)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0,
        RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(withText(context.getString(R.string.audio_dialog_offline_message))).check(
        matches(
          isDisplayed()
        )
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithCellular_openRatioExploration_clickAudioIcon_checkOpensCellularAudioDialog() {
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(
      NetworkConnectionUtil.ConnectionStatus.CELLULAR
    )
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId, RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0, RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(withText(context.getString(R.string.cellular_data_alert_dialog_title))).check(
        matches(
          isDisplayed()
        )
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithCellular_openRatioExploration_clickAudioIcon_changeConfiguration_checkOpensCellularAudioDialog() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(
      NetworkConnectionUtil.ConnectionStatus.CELLULAR
    )
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId, RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0, RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(isRoot()).perform(orientationLandscape())
      onView(withText(context.getString(R.string.cellular_data_alert_dialog_title))).check(
        matches(
          isDisplayed()
        )
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithCellular_openRatioExploration_clickAudioIcon_clickNegative_checkAudioFragmentIsHidden() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(
      NetworkConnectionUtil.ConnectionStatus.CELLULAR
    )
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId, RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0, RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(
        allOf(
          withText(context.getString(R.string.cellular_data_alert_dialog_title)),
          withEffectiveVisibility(Visibility.VISIBLE)
        )
      )
      onView(
        allOf(
          withText(context.getString(R.string.audio_language_select_dialog_cancel_button)),
          withEffectiveVisibility(Visibility.VISIBLE)
        )
      ).perform(click())
      onView(withId(R.id.ivPlayPauseAudio)).check(matches(not(isDisplayed())))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithCellular_openRatioExploration_clickAudioIcon_clickPositive_checkAudioFragmentIsVisible() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(
      NetworkConnectionUtil.ConnectionStatus.CELLULAR
    )
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0,
        RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(
        allOf(
          withText(context.getString(R.string.cellular_data_alert_dialog_title)),
          withEffectiveVisibility(Visibility.VISIBLE)
        )
      )

      onView(
        allOf(
          withText(context.getString(R.string.cellular_data_alert_dialog_okay_button)),
          withEffectiveVisibility(Visibility.VISIBLE)
        )
      ).perform(click())

      onView(
        allOf(
          withId(R.id.ivPlayPauseAudio),
          withEffectiveVisibility(Visibility.VISIBLE)
        )
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithCellular_openRatioExploration_clickCheckboxAndNegative_clickAudioIcon_checkAudioFragmentIsHiddenAndDialogIsNotDisplayed() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(
      NetworkConnectionUtil.ConnectionStatus.CELLULAR
    )
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId, RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0, RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(withText(context.getString(R.string.cellular_data_alert_dialog_title))).check(
        matches(
          isDisplayed()
        )
      )
      onView(withId(R.id.cellular_data_dialog_checkbox)).perform(click())
      onView(withText(context.getString(R.string.audio_language_select_dialog_cancel_button)))
        .perform(
          click()
        )

      onView(withId(R.id.action_audio_player)).perform(click())

      onView(withId(R.id.ivPlayPauseAudio)).check(matches(not(isDisplayed())))
      onView(withText(context.getString(R.string.cellular_data_alert_dialog_title))).check(
        doesNotExist()
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithCellular_openRatioExploration_clickCheckboxAndPositive_clickAudioIconTwice_checkAudioFragmentIsVisibleAndDialogIsNotDisplayed() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(
      NetworkConnectionUtil.ConnectionStatus.CELLULAR
    )
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId, RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0, RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(withText(context.getString(R.string.cellular_data_alert_dialog_title))).check(
        matches(
          isDisplayed()
        )
      )
      onView(withId(R.id.cellular_data_dialog_checkbox)).perform(click())
      onView(withText(context.getString(R.string.audio_language_select_dialog_okay_button)))
        .perform(
          click()
        )

      onView(withId(R.id.action_audio_player)).perform(click())
      onView(withId(R.id.action_audio_player)).perform(click())

      onView(withId(R.id.ivPlayPauseAudio)).check(matches(isDisplayed()))
      onView(withText(context.getString(R.string.cellular_data_alert_dialog_title))).check(
        doesNotExist()
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  @Ignore("The ExplorationActivity takes time to finish, needs to fixed in #89.")
  fun testAudioWithWifi_openRatioExploration_clickAudioIcon_checkAudioFragmentHasDefaultLanguageAndAutoPlays() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(NetworkConnectionUtil.ConnectionStatus.LOCAL)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0,
        RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(
        allOf(
          withId(R.id.ivPlayPauseAudio),
          withEffectiveVisibility(Visibility.VISIBLE)
        )
      )
      onView(allOf(withText("EN"), withEffectiveVisibility(Visibility.VISIBLE)))
      waitForTheView(withDrawable(R.drawable.ic_pause_circle_filled_white_24dp))
      onView(withId(R.id.ivPlayPauseAudio)).check(
        matches(
          withDrawable(
            R.drawable.ic_pause_circle_filled_white_24dp
          )
        )
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  // TODO(#973): Fix ExplorationActivityTest
  @Ignore
  fun testAudioWithWifi_openFractionsExploration_changeLanguage_clickNext_checkLanguageIsHinglish() { // ktlint-disable max-line-length
    getApplicationDependencies(FRACTIONS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(NetworkConnectionUtil.ConnectionStatus.LOCAL)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        FRACTIONS_TOPIC_ID,
        FRACTIONS_STORY_ID_0,
        FRACTIONS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Fraction?"))
      onView(withId(R.id.state_recycler_view)).perform(
        scrollToPosition<RecyclerView.ViewHolder>(
          1
        )
      )
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.action_audio_player)).perform(click())
      onView(
        allOf(
          withText("EN"),
          withEffectiveVisibility(Visibility.VISIBLE)
        )
      ).perform(click())
      onView(withText("Hinglish")).perform(click())
      onView(withText(context.getString(R.string.audio_language_select_dialog_okay_button)))
        .perform(
          click()
        )
      onView(withId(R.id.continue_button)).perform(click())
      onView(withText("HI-EN")).check(matches(isDisplayed()))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  @Ignore("The ExplorationActivity takes time to finish, needs to fixed in #89.")
  fun testAudioWithWifi_openRatioExploration_continueToInteraction_clickAudioButton_submitAnswer_checkFeedbackAudioPlays() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(NetworkConnectionUtil.ConnectionStatus.LOCAL)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId, RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0, RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      // Clicks continue until we reach the first interaction.
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())

      onView(withId(R.id.action_audio_player)).perform(click())
      onView(withId(R.id.text_input_interaction_view)).perform(
        ViewActions.typeText("123"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.submit_answer_button)).perform(click())
      Thread.sleep(1000)

      onView(withId(R.id.ivPlayPauseAudio))
        .check(matches(withContentDescription(context.getString(R.string.audio_pause_description))))
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  fun testMarquee_openRatioExploration_continueToInteraction_submitAnswer_checkToolBarTitleIsSelected() { // ktlint-disable max-line-length
    getApplicationDependencies(RATIOS_EXPLORATION_ID_0)
    networkConnectionUtil.setCurrentConnectionStatus(NetworkConnectionUtil.ConnectionStatus.LOCAL)
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId, RATIOS_TOPIC_ID,
        RATIOS_STORY_ID_0, RATIOS_EXPLORATION_ID_0
      )
    ).use {
      waitForTheView(withText("What is a Ratio?"))
      // Clicks continue until we reach the first interaction.
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())
      onView(withId(R.id.continue_button)).perform(click())

      onView(withId(R.id.text_input_interaction_view)).perform(
        ViewActions.typeText("123"),
        closeSoftKeyboard()
      )
      onView(withId(R.id.submit_answer_button)).perform(click())
      Thread.sleep(1000)

      onView(withId(R.id.exploration_toolbar_title)).check(
        matches(isSelected())
      )
    }
    explorationDataController.stopPlayingExploration()
  }

  @Test
  fun testExplorationActivity_loadExplorationFragment_hasDummyString() {
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        FRACTIONS_TOPIC_ID,
        FRACTIONS_STORY_ID_0,
        FRACTIONS_EXPLORATION_ID_0
      )
    ).use {
      onView(withId(R.id.exploration_fragment_placeholder)).check(matches(isDisplayed()))
    }
  }

  @Test
  fun testExplorationActivity_onBackPressed_showsStopExplorationDialog() {
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        FRACTIONS_TOPIC_ID,
        FRACTIONS_STORY_ID_0,
        FRACTIONS_EXPLORATION_ID_0
      )
    ).use {
      pressBack()
      onView(withText(R.string.stop_exploration_dialog_title)).inRoot(isDialog())
        .check(matches(isDisplayed()))
    }
  }

  @Test
  fun testExplorationActivity_onToolbarClosePressed_showsStopExplorationDialog() {
    launch<ExplorationActivity>(
      createExplorationActivityIntent(
        internalProfileId,
        FRACTIONS_TOPIC_ID,
        FRACTIONS_STORY_ID_0,
        FRACTIONS_EXPLORATION_ID_0
      )
    ).use {
      onView(withContentDescription(R.string.nav_app_bar_navigate_up_description)).perform(click())
      onView(withText(R.string.stop_exploration_dialog_title)).inRoot(isDialog())
        .check(matches(isDisplayed()))
    }
  }

  // TODO(#89): Check this test case too. It works in pair with below test case.
  @Test
  fun testExplorationActivity_onBackPressed_showsStopExplorationDialog_clickCancel_dismissesDialog() { // ktlint-disable max-line-length
    explorationActivityTestRule.launchActivity(
      createExplorationActivityIntent(
        internalProfileId,
        FRACTIONS_TOPIC_ID,
        FRACTIONS_STORY_ID_0,
        FRACTIONS_EXPLORATION_ID_0
      )
    )
    pressBack()
    onView(withText(R.string.stop_exploration_dialog_cancel_button)).inRoot(isDialog())
      .perform(click())
    assertThat(explorationActivityTestRule.activity.isFinishing).isFalse()
  }

  // TODO(#89): The ExplorationActivity takes time to finish. This test case is failing currently.
  @Test
  @Ignore("The ExplorationActivity takes time to finish, needs to fixed in #89.")
  fun testExplorationActivity_onBackPressed_showsStopExplorationDialog_clickLeave_closesExplorationActivity() { // ktlint-disable max-line-length
    explorationActivityTestRule.launchActivity(
      createExplorationActivityIntent(
        internalProfileId,
        FRACTIONS_TOPIC_ID,
        FRACTIONS_STORY_ID_0,
        FRACTIONS_EXPLORATION_ID_0
      )
    )
    pressBack()
    onView(withText(R.string.stop_exploration_dialog_leave_button)).inRoot(isDialog())
      .perform(click())
    assertThat(explorationActivityTestRule.activity.isFinishing).isTrue()
  }

  private fun createExplorationActivityIntent(
    internalProfileId: Int,
    topicId: String,
    storyId: String,
    explorationId: String
  ): Intent {
    return ExplorationActivity.createExplorationActivityIntent(
      ApplicationProvider.getApplicationContext(),
      internalProfileId,
      topicId,
      storyId,
      explorationId, /* backflowScreen= */
      null
    )
  }

  private fun waitForTheView(viewMatcher: Matcher<View>): ViewInteraction {
    return onView(isRoot()).perform(waitForMatch(viewMatcher, 30000L))
  }

// TODO(#59): Remove these waits once we can ensure that the production executors are not depended on in tests.
//  Sleeping is really bad practice in Espresso tests, and can lead to test flakiness. It shouldn't be necessary if we
//  use a test executor service with a counting idle resource, but right now Gradle mixes dependencies such that both
//  the test and production blocking executors are being used. The latter cannot be updated to notify Espresso of any
//  active coroutines, so the test attempts to assert state before it's ready. This artificial delay in the Espresso
//  thread helps to counter that.
  /**
   * Perform action of waiting for a specific matcher to finish. Adapted from:
   * https://stackoverflow.com/a/22563297/3689782.
   */
  private fun waitForMatch(viewMatcher: Matcher<View>, millis: Long): ViewAction {
    return object : ViewAction {
      override fun getDescription(): String {
        return "wait for a specific view with matcher <$viewMatcher> during $millis millis."
      }

      override fun getConstraints(): Matcher<View> {
        return isRoot()
      }

      override fun perform(uiController: UiController?, view: View?) {
        checkNotNull(uiController)
        uiController.loopMainThreadUntilIdle()
        val startTime = System.currentTimeMillis()
        val endTime = startTime + millis

        do {
          if (TreeIterables.breadthFirstViewTraversal(view).any { viewMatcher.matches(it) }) {
            return
          }
          uiController.loopMainThreadForAtLeast(50)
        } while (System.currentTimeMillis() < endTime)

        // Couldn't match in time.
        throw PerformException.Builder()
          .withActionDescription(description)
          .withViewDescription(HumanReadables.describe(view))
          .withCause(TimeoutException())
          .build()
      }
    }
  }

  // TODO(#59): Figure out a way to reuse modules instead of needing to re-declare them.
  // TODO(#1675): Add NetworkModule once data module is migrated off of Moshi.
  @Singleton
  @Component(
    modules = [
      TestDispatcherModule::class, ApplicationModule::class,
      LoggerModule::class, ContinueModule::class, FractionInputModule::class,
      ItemSelectionInputModule::class, MultipleChoiceInputModule::class,
      NumberWithUnitsRuleModule::class, NumericInputRuleModule::class, TextInputRuleModule::class,
      DragDropSortInputModule::class, ImageClickInputModule::class, InteractionsModule::class,
      GcsResourceModule::class, GlideImageLoaderModule::class, ImageParsingModule::class,
      HtmlParserEntityTypeModule::class, QuestionModule::class, TestLogReportingModule::class,
      TestAccessibilityModule::class, LogStorageModule::class, CachingTestModule::class,
      PrimeTopicAssetsControllerModule::class, ExpirationMetaDataRetrieverModule::class,
      ViewBindingShimModule::class, RatioInputModule::class,
      ApplicationStartupListenerModule::class, LogUploadWorkerModule::class,
      WorkManagerConfigurationModule::class, HintsAndSolutionConfigModule::class,
      FirebaseLogUploaderModule::class
    ]
  )
  interface TestApplicationComponent : ApplicationComponent, ApplicationInjector {
    @Component.Builder
    interface Builder : ApplicationComponent.Builder

    fun inject(explorationActivityTest: ExplorationActivityTest)
  }

  class TestApplication : Application(), ActivityComponentFactory, ApplicationInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerExplorationActivityTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build() as TestApplicationComponent
    }

    fun inject(explorationActivityTest: ExplorationActivityTest) {
      component.inject(explorationActivityTest)
    }

    override fun createActivityComponent(activity: AppCompatActivity): ActivityComponent {
      return component.getActivityComponentBuilderProvider().get().setActivity(activity).build()
    }

    override fun getApplicationInjector(): ApplicationInjector = component
  }
}
