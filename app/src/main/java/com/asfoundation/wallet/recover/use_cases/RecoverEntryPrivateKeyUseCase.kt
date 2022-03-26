package com.asfoundation.wallet.recover.use_cases

import com.asfoundation.wallet.recover.result.RecoverEntryResult
import com.asfoundation.wallet.recover.result.RecoverEntryResultMapper
import com.asfoundation.wallet.recover.result.SuccessfulEntryRecover
import com.asfoundation.wallet.repository.BackupRestorePreferencesRepository
import com.asfoundation.wallet.repository.PasswordStore
import com.asfoundation.wallet.repository.WalletRepositoryType
import com.asfoundation.wallet.util.CurrencyFormatUtils
import com.asfoundation.wallet.wallets.usecases.GetWalletInfoUseCase
import io.reactivex.Single
import javax.inject.Inject

class RecoverEntryPrivateKeyUseCase @Inject constructor(
  private val walletRepository: WalletRepositoryType,
  private val passwordStore: PasswordStore,
  private val backupRestorePreferencesRepository: BackupRestorePreferencesRepository,
  private val getWalletInfoUseCase: GetWalletInfoUseCase,
  private val currencyFormatUtils: CurrencyFormatUtils
) {

  operator fun invoke(privateKey: String): Single<RecoverEntryResult> {
    return passwordStore.generatePassword()
      .flatMap { newPassword ->
        walletRepository.restorePrivateKeyToWallet(privateKey, newPassword)
      }
      .flatMap {
        RecoverEntryResultMapper(getWalletInfoUseCase, currencyFormatUtils, privateKey).map(it)
      }
      .doOnSuccess {
        when (it) {
          is SuccessfulEntryRecover -> backupRestorePreferencesRepository.setWalletRestoreBackup(
            it.address
          )
          else -> Unit
        }
      }
  }
}