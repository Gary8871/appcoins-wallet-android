package com.asfoundation.wallet.logging.send_logs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import io.reactivex.Observable

class SettingsSendLogsPreference(context: Context?, attrs: AttributeSet?) :
    Preference(context, attrs) {
  //  private var currency: TextView? = null
//  private var flag: ImageView? = null
//  var preferenceClickListener: View.OnClickListener? = null
//
  init {
    this.isVisible = false
  }

  //
//  override fun onBindViewHolder(holder: PreferenceViewHolder) {
//    super.onBindViewHolder(holder)
//    currency = holder.findViewById(R.id.settings_currency_text) as TextView
//    flag = holder.findViewById(R.id.settings_flag_ic) as ImageView
//    setCurrencyTextView()
//    setFlagImageView()
//  }
//
  fun setSendLogsState(sendLogsState: Observable<SendLogsState>) {
    sendLogsState
        .map {
          this.isVisible = it.shouldShow
        }
        .subscribe()
  }
//
//  private fun setCurrencyTextView() {
//    currency?.text = selectedCurrency?.currency
//  }
//
//  private fun setFlagImageView() {
//    safeLet(flag, selectedCurrency?.flag) { flagView, flagUrl ->
//      GlideApp.with(context)
//          .load(Uri.parse(flagUrl))
//          .transition(DrawableTransitionOptions.withCrossFade())
//          .circleCrop()
//          .into(flagView)
//    }
//  }
}