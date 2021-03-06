package io.horizontalsystems.bankwallet.modules.guest

import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute

class GuestViewModel: ViewModel(), GuestModule.IView, GuestModule.IRouter, IKeyStoreSafeExecute {

    lateinit var delegate: GuestModule.IViewDelegate

    val openBackupScreenLiveEvent = SingleLiveEvent<Void>()
    val openRestoreWalletScreenLiveEvent = SingleLiveEvent<Void>()
    val showErrorDialog = SingleLiveEvent<Void>()
    val keyStoreSafeExecute = SingleLiveEvent<Triple<Runnable, Runnable?, Runnable?>>()

    fun init() {
        GuestModule.init(this, this, this)
    }

    // router
    override fun navigateToBackupRoutingToMain() {
        openBackupScreenLiveEvent.call()
    }

    override fun navigateToRestore() {
        openRestoreWalletScreenLiveEvent.call()
    }

    override fun showError() {
        showErrorDialog.call()
    }

    override fun safeExecute(action: Runnable, onSuccess: Runnable?, onFailure: Runnable?) {
        keyStoreSafeExecute.value = Triple(action, onSuccess, onFailure)
    }
}
