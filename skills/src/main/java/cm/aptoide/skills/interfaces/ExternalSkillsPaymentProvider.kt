package cm.aptoide.skills.interfaces

import cm.aptoide.skills.model.CreatedTicket
import cm.aptoide.skills.model.Price
import cm.aptoide.skills.util.EskillsPaymentData
import io.reactivex.Completable
import io.reactivex.Single
import java.math.BigDecimal

interface ExternalSkillsPaymentProvider {
  fun pay(eskillsPaymentData: EskillsPaymentData, ticket: CreatedTicket): Completable
  fun getLocalFiatAmount(value: BigDecimal, currency: String): Single<Price>
  fun getFiatToAppcAmount(value: BigDecimal, currency: String): Single<Price>
  fun getFormattedAppcAmount(value: BigDecimal, currency: String): Single<String>
}
