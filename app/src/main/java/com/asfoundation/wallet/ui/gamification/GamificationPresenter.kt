package com.asfoundation.wallet.ui.gamification

import android.os.Bundle
import com.appcoins.wallet.gamification.GamificationScreen
import com.appcoins.wallet.gamification.repository.GamificationStats
import com.appcoins.wallet.gamification.repository.Levels
import com.asfoundation.wallet.analytics.gamification.GamificationAnalytics
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.util.WalletCurrency
import com.asfoundation.wallet.util.isNoNetworkException
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.math.BigDecimal

class GamificationPresenter(private val view: GamificationFragment,
                            private val activityView: RewardsLevelView,
                            private val gamification: GamificationInteractor,
                            private val analytics: GamificationAnalytics,
                            private val formatter: CurrencyFormatUtils,
                            private val disposables: CompositeDisposable,
                            private val viewScheduler: Scheduler,
                            private val networkScheduler: Scheduler) {

  fun present(savedInstanceState: Bundle?) {
    handleLevelInformation(savedInstanceState == null)
  }

  private fun handleLevelInformation(sendEvent: Boolean) {
    disposables.add(Single.zip(gamification.getLevels(), gamification.getUserStats(),
        BiFunction { levels: Levels, gamificationStats: GamificationStats ->
          handleHeaderInformation(gamificationStats.totalEarned, gamificationStats.totalSpend,
              gamificationStats.status)
          mapToUserStatus(levels, gamificationStats)
        })
        .subscribeOn(networkScheduler)
        .observeOn(viewScheduler)
        .doOnSuccess { displayInformation(it, sendEvent) }
        .flatMapCompletable {
          gamification.levelShown(it.currentLevel, GamificationScreen.MY_LEVEL)
        }
        .subscribe({}, { handleError(it) }))
  }

  private fun mapToUserStatus(levels: Levels,
                              gamificationStats: GamificationStats): GamificationInfo {
    var status = Status.UNKNOWN_ERROR
    if (levels.status == Levels.Status.OK && gamificationStats.status == GamificationStats.Status.OK) {
      return GamificationInfo(gamificationStats.level, gamificationStats.totalSpend, levels.list,
          Status.OK)
    }
    if (levels.status == Levels.Status.NO_NETWORK || gamificationStats.status == GamificationStats.Status.NO_NETWORK) {
      status = Status.NO_NETWORK
    }
    return GamificationInfo(status)
  }

  private fun displayInformation(gamification: GamificationInfo, sendEvent: Boolean) {
    if (gamification.status != Status.OK) {
      activityView.showNetworkErrorView()
    } else {
      val currentLevel = gamification.currentLevel
      activityView.showMainView()
      view.displayGamificationInfo(currentLevel, gamification.levels, gamification.totalSpend)
      if (sendEvent) analytics.sendMainScreenViewEvent(currentLevel + 1)
    }
  }

  private fun handleHeaderInformation(totalEarned: BigDecimal, totalSpend: BigDecimal,
                                      status: GamificationStats.Status) {
    if (status == GamificationStats.Status.OK) {
      disposables.add(gamification.getAppcToLocalFiat(totalEarned.toString(), 2)
          .filter { it.amount.toInt() >= 0 }
          .observeOn(viewScheduler)
          .doOnNext {
            val totalSpent = formatter.formatCurrency(totalSpend, WalletCurrency.FIAT)
            val bonusEarned = formatter.formatCurrency(it.amount, WalletCurrency.FIAT)
            view.showHeaderInformation(totalSpent, bonusEarned, it.symbol)
          }
          .subscribeOn(networkScheduler)
          .subscribe({}, { handleError(it) }))
    }
  }

  private fun handleError(throwable: Throwable) {
    throwable.printStackTrace()
    if (throwable.isNoNetworkException()) {
      activityView.showNetworkErrorView()
    }
  }


  fun stop() = disposables.clear()
}
