package org.oppia.android.data.backends.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.android.data.backends.ApiUtils
import org.oppia.android.data.backends.api.MockFeedbackReportingService
import org.oppia.android.data.backends.gae.api.FeedbackReportingService
import org.oppia.android.data.backends.gae.model.GaeFeedbackReport
import org.oppia.android.testing.network.MockRetrofitHelper
import org.robolectric.annotation.LooperMode
import retrofit2.mock.MockRetrofit

/** Test for [FeedbackReportingService] retrofit instance using a [MockFeedbackReportingService]. */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MockFeedbackReportingTest {
  private lateinit var mockRetrofit: MockRetrofit

  @Before
  fun setUp() {
    mockRetrofit = MockRetrofitHelper().createMockRetrofit()
  }

  @Test
  fun testFeedbackReportingService_postRequest_successfulResponseReceived() {
    val delegate = mockRetrofit.create(FeedbackReportingService::class.java)
    val mockService = MockFeedbackReportingService(delegate)
    val mockGaeFeedbackReport = createMockGaeFeedbackReport()

    val response = mockService.postFeedbackReport(mockGaeFeedbackReport).execute()

    // Service returns a Unit type so no information is contained in the response.
    assertThat(response.isSuccessful).isTrue()
  }

  private fun createMockGaeFeedbackReport(): GaeFeedbackReport {
    val feedbackReportJson = ApiUtils.getFakeJson("feedback_reporting.json")
    val moshi = Moshi.Builder().build()

    val adapter: JsonAdapter<GaeFeedbackReport> = moshi.adapter(GaeFeedbackReport::class.java)
    val mockGaeFeedbackReport = adapter.fromJson(feedbackReportJson)
    return mockGaeFeedbackReport!!
  }
}
