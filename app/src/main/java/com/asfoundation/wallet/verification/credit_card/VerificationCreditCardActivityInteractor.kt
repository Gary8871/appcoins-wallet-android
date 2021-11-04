package com.asfoundation.wallet.verification.credit_card

import com.appcoins.wallet.bdsbilling.WalletService
import com.asfoundation.wallet.verification.credit_card.network.VerificationStatus
import com.asfoundation.wallet.verification.repository.VerificationRepository
import io.reactivex.Single

class VerificationCreditCardActivityInteractor(
    private val verificationRepository: VerificationRepository,
    private val walletService: WalletService
) {

  fun getVerificationStatus(): Single<VerificationStatus> {
    return walletService.getAndSignCurrentWalletAddress()
        .flatMap { addressModel ->
          verificationRepository.getCardVerificationState(addressModel.address,
              addressModel.signedAddress)
        }
        .onErrorReturn { VerificationStatus.UNVERIFIED }
  }
}