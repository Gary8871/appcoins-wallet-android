package com.appcoins.wallet.gamification.repository

import com.appcoins.wallet.gamification.GamificationContext
import com.appcoins.wallet.gamification.repository.entity.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.io.IOException
import java.math.BigDecimal
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit

class BdsPromotionsRepository(private val api: GamificationApi,
                              private val local: UserStatsLocalData) : PromotionsRepository {

  private fun getUserStats(wallet: String): Single<UserStatusResponse> {
    return api.getUserStats(wallet, Locale.getDefault().language)
        .map { filterByDate(it) }
        .flatMap { userStats ->
          local.deletePromotions()
              .andThen(local.insertPromotions(userStats.promotions)
                  .andThen(local.insertWalletOrigin(wallet, userStats.walletOrigin)))
              .toSingle { userStats }
        }
        .onErrorResumeNext { t ->
          Single.zip(local.getPromotions(), local.retrieveWalletOrigin(wallet),
              BiFunction { promotions: List<PromotionsResponse>, walletOrigin: WalletOrigin ->
                Pair(promotions, walletOrigin)
              })
              .map { (promotions, walletOrigin) ->
                mapErrorToUserStatsModel(promotions, walletOrigin, t)
              }
              .onErrorReturn { mapErrorToUserStatsModel(t, false) }
        }
  }

  // TODO, see if this can be merged with the above one, passing a boolean to decide which
  //  one to apply (if necessary)
  private fun getUserStatsDbFirst(wallet: String): Observable<UserStatusResponse> {
    return Observable.concat(getUserStatsFromDB(wallet), getUserStatsFromAPI(wallet))
  }

  private fun getUserStatsFromDB(wallet: String): Observable<UserStatusResponse> {
    return Single.zip(local.getPromotions(), local.retrieveWalletOrigin(wallet),
        BiFunction { promotions: List<PromotionsResponse>, walletOrigin: WalletOrigin ->
          Pair(promotions, walletOrigin)
        })
        .toObservable()
        .map { (promotions, walletOrigin) ->
          UserStatusResponse(promotions, walletOrigin, null, true)
        }
        .onErrorReturn {
          mapErrorToUserStatsModel(it, true)
        }
  }

  private fun getUserStatsFromAPI(wallet: String): Observable<UserStatusResponse> {
    return api.getUserStats(wallet, Locale.getDefault().language)
        .toObservable()
        .map {
          filterByDate(it)
        }
        .flatMap {
          local.deletePromotions()
              .andThen(local.insertPromotions(it.promotions)
                  .andThen(local.insertWalletOrigin(wallet, it.walletOrigin)))
              .toSingle { it }
              .toObservable()
        }
        .onErrorReturn {
          mapErrorToUserStatsModel(it, false)
        }
  }

  private fun filterByDate(userStatusResponse: UserStatusResponse): UserStatusResponse {
    val validPromotions = userStatusResponse.promotions.filter { hasValidDate(it) }
    return UserStatusResponse(validPromotions, userStatusResponse.walletOrigin)
  }

  private fun hasValidDate(promotionsResponse: PromotionsResponse): Boolean {
    return if (promotionsResponse is GenericResponse) {
      val currentTime = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      currentTime < promotionsResponse.endDate
    } else true
  }

  override fun getLastShownLevel(wallet: String,
                                 gamificationContext: GamificationContext): Single<Int> {
    return local.getLastShownLevel(wallet, gamificationContext)
  }

  override fun shownLevel(wallet: String, level: Int, gamificationContext: GamificationContext) {
    return local.saveShownLevel(wallet, level, gamificationContext)
  }

  override fun getSeenGenericPromotion(id: String, screen: String): Boolean {
    return local.getSeenGenericPromotion(id, screen)
  }

  override fun setSeenGenericPromotion(id: String, screen: String) {
    return local.setSeenGenericPromotion(id, screen)
  }

  override fun getForecastBonus(wallet: String, packageName: String,
                                amount: BigDecimal): Single<ForecastBonus> {
    return api.getForecastBonus(wallet, packageName, amount, "APPC")
        .map { map(it) }
        .onErrorReturn { mapForecastError(it) }
  }

  private fun mapForecastError(throwable: Throwable): ForecastBonus {
    throwable.printStackTrace()
    return if (isNoNetworkException(throwable)) {
      ForecastBonus(ForecastBonus.Status.NO_NETWORK)
    } else {
      ForecastBonus(ForecastBonus.Status.UNKNOWN_ERROR)
    }
  }

  private fun map(bonusResponse: ForecastBonusResponse): ForecastBonus {
    if (bonusResponse.status == ForecastBonusResponse.Status.ACTIVE) {
      return ForecastBonus(ForecastBonus.Status.ACTIVE, bonusResponse.bonus)
    }
    return ForecastBonus(ForecastBonus.Status.INACTIVE)
  }

  override fun getGamificationStats(wallet: String): Observable<GamificationStats> {
    return getUserStatsDbFirst(wallet)
        .map {
          mapToGamificationStats(it)
        }
        .flatMap {
          local.setGamificationLevel(it.level)
              .toSingle { it }
              .toObservable()
        }
  }

  override fun getGamificationLevel(wallet: String): Single<Int> {
    return getGamificationStats(wallet).map { it.level }
        .filter { it >= 0 }
        .lastOrError()
        .onErrorReturn { -1 }
  }

  private fun map(status: Status, fromCache: Boolean): GamificationStats {
    return if (status == Status.NO_NETWORK) {
      GamificationStats(GamificationStats.Status.NO_NETWORK, fromCache = fromCache)
    } else {
      GamificationStats(GamificationStats.Status.UNKNOWN_ERROR, fromCache = fromCache)
    }
  }

  private fun mapErrorToUserStatsModel(throwable: Throwable,
                                       fromCache: Boolean): UserStatusResponse {
    throwable.printStackTrace()
    return if (isNoNetworkException(throwable)) {
      UserStatusResponse(Status.NO_NETWORK, fromCache)
    } else {
      UserStatusResponse(Status.UNKNOWN_ERROR, fromCache)
    }
  }

  private fun mapErrorToUserStatsModel(promotions: List<PromotionsResponse>,
                                       walletOrigin: WalletOrigin,
                                       throwable: Throwable): UserStatusResponse {
    return when {
      promotions.isEmpty() && isNoNetworkException(throwable) -> {
        throwable.printStackTrace()
        UserStatusResponse(Status.NO_NETWORK)
      }
      promotions.isEmpty() -> {
        throwable.printStackTrace()
        UserStatusResponse(Status.UNKNOWN_ERROR)
      }
      else -> UserStatusResponse(promotions, walletOrigin)
    }
  }

  private fun mapToGamificationStats(response: UserStatusResponse): GamificationStats {
    return if (response.error != null) {
      map(response.error, response.fromCache)
    } else {
      val gamification =
          response.promotions.firstOrNull { it is GamificationResponse } as GamificationResponse?
      if (gamification == null) {
        GamificationStats(GamificationStats.Status.UNKNOWN_ERROR, fromCache = response.fromCache)
      } else {
        GamificationStats(GamificationStats.Status.OK, gamification.level,
            gamification.nextLevelAmount, gamification.bonus, gamification.totalSpend,
            gamification.totalEarned, PromotionsResponse.Status.ACTIVE == gamification.status,
            response.fromCache)
      }
    }
  }

  override fun getLevels(wallet: String): Single<Levels> {
    return api.getLevels(wallet)
        .flatMap {
          local.deleteLevels()
              .andThen(local.insertLevels(it))
              .toSingle { it }
        }
        .map { map(it, false) }
        .onErrorResumeNext { t ->
          local.getLevels()
              .map { map(it, true) }
              .onErrorReturn { mapLevelsError(t, false) }
        }
  }

  override fun getLevelsOfflineFirst(wallet: String): Observable<Levels> {
    return Observable.concat(getLevelsFromDB(), getLevelsFromAPI(wallet))
  }

  private fun getLevelsFromDB(): Observable<Levels> {
    return local.getLevels()
        .toObservable()
        .map { map(it, true) }
        .onErrorReturn { mapLevelsError(it, true) }
  }

  private fun getLevelsFromAPI(wallet: String): Observable<Levels> {
    return api.getLevels(wallet)
        .toObservable()
        .flatMap {
          local.deleteLevels()
              .andThen(local.insertLevels(it))
              .toSingle { map(it, false) }
              .toObservable()
        }
        .onErrorReturn { mapLevelsError(it, false) }
  }

  private fun mapLevelsError(throwable: Throwable, fromCache: Boolean): Levels {
    throwable.printStackTrace()
    return if (isNoNetworkException(throwable)) {
      Levels(Levels.Status.NO_NETWORK, fromCache = fromCache)
    } else {
      Levels(Levels.Status.UNKNOWN_ERROR, fromCache = fromCache)
    }
  }

  private fun map(response: LevelsResponse, fromCache: Boolean): Levels {
    val list = response.list.map { Levels.Level(it.amount, it.bonus, it.level) }
    return Levels(Levels.Status.OK, list, LevelsResponse.Status.ACTIVE == response.status,
        response.updateDate, fromCache)
  }

  override fun getUserStatus(wallet: String): Single<UserStatusResponse> {
    return getUserStats(wallet)
        .flatMap { userStatusResponse ->
          val gamification =
              userStatusResponse.promotions.firstOrNull { it is GamificationResponse } as GamificationResponse?
          if (userStatusResponse.error != null || gamification == null) {
            Single.just(userStatusResponse)
          } else {
            local.setGamificationLevel(gamification.level)
                .toSingle { userStatusResponse }
          }
        }
        .doOnError { it.printStackTrace() }
  }

  override fun getUserStatusDbFirst(wallet: String): Observable<UserStatusResponse> {
    return getUserStatsDbFirst(wallet)
        .flatMap { userStatusResponse ->
          val gamification =
              userStatusResponse.promotions.firstOrNull { it is GamificationResponse } as GamificationResponse?
          if (userStatusResponse.error != null || gamification == null) {
            Observable.just(userStatusResponse)
          } else {
            local.setGamificationLevel(gamification.level)
                .toSingle { userStatusResponse }
                .toObservable()
          }
        }
        .doOnError { it.printStackTrace() }
  }

  override fun getWalletOrigin(wallet: String): Single<WalletOrigin> {
    return getUserStatusDbFirst(wallet)
        .map { it.walletOrigin }
        .filter { it != WalletOrigin.UNKNOWN }
        .lastOrError()
        .onErrorReturn { WalletOrigin.UNKNOWN }
  }

  override fun getReferralUserStatus(wallet: String): Single<ReferralResponse> {
    return getUserStatsDbFirst(wallet)
        .lastOrError()
        .flatMap {
          val gamification =
              it.promotions.firstOrNull { promotions -> promotions is GamificationResponse } as GamificationResponse?
          val referral =
              it.promotions.firstOrNull { promotions -> promotions is ReferralResponse } as ReferralResponse?
          if (gamification != null) {
            local.setGamificationLevel(gamification.level)
                .toSingle { referral }
          } else {
            Single.just(referral)
          }
        }
        .map { it }
  }

  override fun getReferralInfo(): Single<ReferralResponse> {
    return api.getReferralInfo()
  }

  private fun isNoNetworkException(throwable: Throwable): Boolean {
    return throwable is IOException ||
        throwable.cause != null && throwable.cause is IOException ||
        throwable is UnknownHostException
  }

}