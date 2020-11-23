package com.asfoundation.wallet.wallet_verification.code

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asf.wallet.R
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.util.Duration
import com.asfoundation.wallet.util.KeyboardUtils
import com.asfoundation.wallet.util.WalletCurrency
import com.asfoundation.wallet.wallet_verification.WalletVerificationActivityView
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.support.DaggerFragment
import io.reactivex.Observable
import kotlinx.android.synthetic.main.error_top_up_layout.*
import kotlinx.android.synthetic.main.error_top_up_layout.view.*
import kotlinx.android.synthetic.main.fragment_verification_code.*
import kotlinx.android.synthetic.main.layout_verify_example.view.*
import java.util.*
import javax.inject.Inject

class WalletVerificationCodeFragment : DaggerFragment(), WalletVerificationCodeView {

  @Inject
  lateinit var presenter: WalletVerificationCodePresenter

  @Inject
  lateinit var data: VerificationCodeData

  @Inject
  lateinit var formatter: CurrencyFormatUtils

  private lateinit var activityView: WalletVerificationActivityView

  companion object {

    internal const val CURRENCY_KEY = "currency"
    internal const val SIGN_KEY = "sign"
    internal const val AMOUNT_KEY = "amount"
    internal const val DIGITS_KEY = "digits"
    internal const val FORMAT_KEY = "format"
    internal const val PERIOD_KEY = "period"
    internal const val DATE_KEY = "date"

    @JvmStatic
    fun newInstance(currency: String, sign: String, value: String, digits: Int, format: String,
                    period: String, date: Long): WalletVerificationCodeFragment {
      return WalletVerificationCodeFragment().apply {
        arguments = Bundle().apply {
          putString(CURRENCY_KEY, currency)
          putString(SIGN_KEY, sign)
          putString(AMOUNT_KEY, value)
          putString(FORMAT_KEY, format)
          putString(PERIOD_KEY, period)
          putLong(DATE_KEY, date)
          putInt(DIGITS_KEY, digits)
        }
      }
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    require(context is WalletVerificationActivityView) {
      throw IllegalStateException(
          "Wallet Verification Code must be attached to Wallet Verification Activity")
    }
    activityView = context
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_verification_code, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupUi()
  }

  fun setupUi() {
    val amount = formatter.formatCurrency(data.amount, WalletCurrency.FIAT)
    val amountWithCurrency = "${data.sign} $amount"
    val amountWithCurrencyAndSign = "${data.sign} -$amount"

    val date = convertToDate(data.date)
    val duration = Duration.parse(data.period)

    val periodInDays = duration.toDays()
    val periodInHours = duration.toHours()

    val period = String.format(getString(R.string.card_verification_code_example_code),
        periodInDays.toString())
    val codeTitle = String.format(getString(R.string.card_verification_code_enter_title),
        data.digits.toString())
    val codeDisclaimer = if (periodInDays > 0) {
      resources.getQuantityString(R.plurals.card_verification_code_enter_days_body,
          periodInDays.toInt(), amountWithCurrency, periodInDays.toString())
    } else {
      resources.getQuantityString(R.plurals.card_verification_code_enter_hours_body,
          periodInHours.toInt(), amountWithCurrency, periodInHours.toString())
    }

    code.setEms(data.digits)
    code.filters = arrayOf(InputFilter.LengthFilter(data.digits))

    layout_example.trans_date_value.text = date
    layout_example.description_value.text = data.format
    layout_example.amount_value.text = amountWithCurrencyAndSign
    layout_example.arrow_desc.text = period
    code_title.text = codeTitle
    code_disclaimer.text = codeDisclaimer
  }

  override fun showLoading() {
    no_network.visibility = View.GONE
    fragment_adyen_error?.visibility = View.GONE
    content_container.visibility = View.GONE
    progress_bar.visibility = View.VISIBLE
  }

  override fun hideLoading() {
    progress_bar.visibility = View.GONE
    content_container.visibility = View.VISIBLE
  }

  override fun showSuccess() {
    TODO("Not yet implemented")
  }

  override fun showGenericError() {
    showSpecificError(R.string.unknown_error)
  }

  override fun showNetworkError() {
    unlockRotation()
    progress_bar.visibility = View.GONE
    content_container.visibility = View.GONE
    no_network.visibility = View.VISIBLE
  }

  override fun showSpecificError(stringRes: Int) {
    unlockRotation()
    progress_bar.visibility = View.GONE
    content_container.visibility = View.GONE

    val message = getString(stringRes)
    fragment_adyen_error?.error_message?.text = message
    fragment_adyen_error?.visibility = View.VISIBLE
  }

  override fun hideKeyboard() {
    view?.let { KeyboardUtils.hideKeyboard(view) }
  }

  override fun lockRotation() {
    activityView.lockRotation()
  }

  override fun unlockRotation() {
    activityView.unlockRotation()
  }

  override fun getMaybeLaterClicks() = RxView.clicks(maybe_later)

  override fun getConfirmClicks(): Observable<String> = RxView.clicks(confirm)
      .map { code.text.toString() }

  override fun getTryAgainClicks() = RxView.clicks(try_again)

  override fun getChangeCardClicks() = RxView.clicks(change_card_button)

  override fun getSupportClicks(): Observable<Any> {
    return Observable.merge(RxView.clicks(layout_support_logo), RxView.clicks(layout_support_icn))
  }

  private fun convertToDate(ts: Long): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
        .apply { timeInMillis = ts }
    return DateFormat.format("dd/MM/yyyy", cal.time)
        .toString()
  }

  override fun onDestroyView() {
    presenter.stop()
    super.onDestroyView()
  }

}