package com.asfoundation.wallet.di

import android.content.Context
import cm.aptoide.analytics.AnalyticsManager
import com.asfoundation.wallet.abtesting.experiments.topup.TopUpABTestingAnalytics
import com.asfoundation.wallet.analytics.*
import com.asfoundation.wallet.analytics.gamification.GamificationAnalytics
import com.asfoundation.wallet.billing.analytics.BillingAnalytics
import com.asfoundation.wallet.billing.analytics.PageViewAnalytics
import com.asfoundation.wallet.billing.analytics.PoaAnalytics
import com.asfoundation.wallet.billing.analytics.WalletsAnalytics
import com.asfoundation.wallet.home.HomeAnalytics
import com.asfoundation.wallet.rating.RatingAnalytics
import com.asfoundation.wallet.topup.TopUpAnalytics
import com.asfoundation.wallet.ui.iab.PaymentMethodsAnalytics
import com.asfoundation.wallet.verification.ui.credit_card.VerificationAnalytics
import com.facebook.appevents.AppEventsLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AnalyticsModule {

  @Singleton
  @Provides
  @Named("bi_event_list")
  fun provideBiEventList() =
      listOf(BillingAnalytics.PURCHASE_DETAILS, BillingAnalytics.PAYMENT_METHOD_DETAILS,
          BillingAnalytics.PAYMENT, PoaAnalytics.POA_STARTED, PoaAnalytics.POA_COMPLETED)

  @Singleton
  @Provides
  @Named("facebook_event_list")
  fun provideFacebookEventList() =
      listOf(BillingAnalytics.PURCHASE_DETAILS, BillingAnalytics.PAYMENT_METHOD_DETAILS,
          BillingAnalytics.PAYMENT, BillingAnalytics.REVENUE, PoaAnalytics.POA_STARTED,
          PoaAnalytics.POA_COMPLETED, HomeAnalytics.OPEN_APPLICATION,
          GamificationAnalytics.GAMIFICATION, GamificationAnalytics.GAMIFICATION_MORE_INFO)

  @Singleton
  @Provides
  @Named("rakam_event_list")
  fun provideRakamEventList() =
      listOf(LaunchAnalytics.FIRST_LAUNCH, HomeAnalytics.WALLET_HOME_INTERACTION_EVENT,
          BillingAnalytics.RAKAM_PRESELECTED_PAYMENT_METHOD, BillingAnalytics.RAKAM_PAYMENT_METHOD,
          BillingAnalytics.RAKAM_PAYMENT_CONFIRMATION, BillingAnalytics.RAKAM_PAYMENT_CONCLUSION,
          BillingAnalytics.RAKAM_PAYMENT_START, BillingAnalytics.RAKAM_PAYPAL_URL,
          BillingAnalytics.RAKAM_PAYMENT_METHOD_DETAILS, BillingAnalytics.RAKAM_PAYMENT_BILLING,
          TopUpAnalytics.WALLET_TOP_UP_START, TopUpAnalytics.WALLET_TOP_UP_SELECTION,
          TopUpAnalytics.WALLET_TOP_UP_CONFIRMATION, TopUpAnalytics.WALLET_TOP_UP_CONCLUSION,
          TopUpAnalytics.WALLET_TOP_UP_PAYPAL_URL, TopUpAnalytics.RAKAM_TOP_UP_BILLING,
          PoaAnalytics.RAKAM_POA_EVENT, WalletsAnalytics.WALLET_CREATE_BACKUP,
          WalletsAnalytics.WALLET_SAVE_BACKUP, WalletsAnalytics.WALLET_CONFIRMATION_BACKUP,
          WalletsAnalytics.WALLET_SAVE_FILE, WalletsAnalytics.WALLET_IMPORT_RESTORE,
          WalletsAnalytics.WALLET_MY_WALLETS_INTERACTION_EVENT,
          WalletsAnalytics.WALLET_PASSWORD_RESTORE, PageViewAnalytics.WALLET_PAGE_VIEW,
          TopUpABTestingAnalytics.TOPUP_DEFAULT_VALUE_PARTICIPATING_EVENT,
          RatingAnalytics.WALLET_RATING_WELCOME_EVENT, RatingAnalytics.WALLET_RATING_POSITIVE_EVENT,
          RatingAnalytics.WALLET_RATING_NEGATIVE_EVENT, RatingAnalytics.WALLET_RATING_FINISH_EVENT,
          VerificationAnalytics.START_EVENT, VerificationAnalytics.INSERT_CARD_EVENT,
          VerificationAnalytics.REQUEST_CONCLUSION_EVENT, VerificationAnalytics.CONFIRM_EVENT,
          VerificationAnalytics.CONCLUSION_EVENT,
          PaymentMethodsAnalytics.WALLET_PAYMENT_LOADING_TOTAL,
          PaymentMethodsAnalytics.WALLET_PAYMENT_LOADING_STEP)

  @Singleton
  @Provides
  @Named("amplitude_event_list")
  fun provideAmplitudeEventList() = listOf(BillingAnalytics.RAKAM_PRESELECTED_PAYMENT_METHOD,
      BillingAnalytics.RAKAM_PAYMENT_METHOD, BillingAnalytics.RAKAM_PAYMENT_CONFIRMATION,
      BillingAnalytics.RAKAM_PAYMENT_CONCLUSION, BillingAnalytics.RAKAM_PAYMENT_START,
      BillingAnalytics.RAKAM_PAYPAL_URL, TopUpAnalytics.WALLET_TOP_UP_START,
      TopUpAnalytics.WALLET_TOP_UP_SELECTION, TopUpAnalytics.WALLET_TOP_UP_CONFIRMATION,
      TopUpAnalytics.WALLET_TOP_UP_CONCLUSION, TopUpAnalytics.WALLET_TOP_UP_PAYPAL_URL,
      PoaAnalytics.RAKAM_POA_EVENT, WalletsAnalytics.WALLET_CREATE_BACKUP,
      WalletsAnalytics.WALLET_SAVE_BACKUP, WalletsAnalytics.WALLET_CONFIRMATION_BACKUP,
      WalletsAnalytics.WALLET_SAVE_FILE, WalletsAnalytics.WALLET_IMPORT_RESTORE,
      WalletsAnalytics.WALLET_PASSWORD_RESTORE, PageViewAnalytics.WALLET_PAGE_VIEW)

  @Singleton
  @Provides
  fun provideAnalyticsManager(@Named("default") okHttpClient: OkHttpClient, api: AnalyticsAPI,
                              @ApplicationContext context: Context,
                              @Named("bi_event_list") biEventList: List<String>,
                              @Named("facebook_event_list") facebookEventList: List<String>,
                              @Named("rakam_event_list") rakamEventList: List<String>,
                              @Named("amplitude_event_list")
                              amplitudeEventList: List<String>): AnalyticsManager {
    return AnalyticsManager.Builder()
        .addLogger(BackendEventLogger(api), biEventList)
        .addLogger(FacebookEventLogger(AppEventsLogger.newLogger(context)), facebookEventList)
        .addLogger(RakamEventLogger(), rakamEventList)
        .addLogger(AmplitudeEventLogger(), amplitudeEventList)
        .setAnalyticsNormalizer(KeysNormalizer())
        .setDebugLogger(LogcatAnalyticsLogger())
        .setKnockLogger(HttpClientKnockLogger(okHttpClient))
        .build()
  }
}
