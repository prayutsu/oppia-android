package org.oppia.android.data.backends.test

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.BindsInstance
import dagger.Component
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.android.data.backends.api.MockConceptCardService
import org.oppia.android.data.backends.gae.api.ConceptCardService
import org.oppia.android.testing.network.RetrofitTestModule
import org.robolectric.annotation.LooperMode
import retrofit2.mock.MockRetrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test for [ConceptCardService] retrofit instance using [MockConceptCardService]
 */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MockConceptCardTest {

  @Inject
  lateinit var mockRetrofit: MockRetrofit

  @Before
  fun setUp() {
    setUpTestApplicationComponent()
  }

  @Test
  fun testConceptCardService_usingFakeJson_deserializationSuccessful() {
    val delegate = mockRetrofit.create(ConceptCardService::class.java)
    val mockConceptCardService = MockConceptCardService(delegate)

    val skillIdList = ArrayList<String>()
    skillIdList.add("1")
    skillIdList.add("2")
    skillIdList.add("3")

    val skillIds = skillIdList.joinToString(separator = ", ")
    val conceptCard = mockConceptCardService.getSkillContents(skillIds)
    val conceptCardResponse = conceptCard.execute()

    assertThat(conceptCardResponse.isSuccessful).isTrue()
    assertThat(conceptCardResponse.body()!!.conceptCardDicts!!.size).isEqualTo(1)
  }

  private fun setUpTestApplicationComponent() {
    DaggerMockConceptCardTest_TestApplicationComponent
      .builder()
      .setApplication(ApplicationProvider.getApplicationContext()).build().inject(this)
  }

  // TODO(#89): Move this to a common test application component.
  @Singleton
  @Component(modules = [RetrofitTestModule::class])
  interface TestApplicationComponent {
    @Component.Builder
    interface Builder {
      @BindsInstance
      fun setApplication(application: Application): Builder

      fun build(): TestApplicationComponent
    }

    fun inject(test: MockConceptCardTest)
  }
}
