package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.coins.Coin
import io.reactivex.subjects.BehaviorSubject

class WalletPresenter(
        private var interactor: WalletModule.IInteractor,
        private val router: WalletModule.IRouter) : WalletModule.IViewDelegate, WalletModule.IInteractorDelegate {

    var view: WalletModule.IView? = null

    private var coinValues = mutableMapOf<String, CoinValue>()
    private var rates = mutableMapOf<Coin, CurrencyValue>()
    private var progresses = mutableMapOf<String, BehaviorSubject<Double>>()

    override fun onReceiveClicked(adapterId: String) {
        router.openReceiveDialog(adapterId)
    }

    override fun onSendClicked(adapterId: String) {
        val adapter = App.adapterManager.adapters.firstOrNull { it.id == adapterId }
        adapter?.let { router.openSendDialog(it) }
    }

    override fun viewDidLoad() {
        interactor.notifyWalletBalances()
    }

    override fun didInitialFetch(coinValues: MutableMap<String, CoinValue>, rates: MutableMap<Coin, CurrencyValue>, progresses: MutableMap<String, BehaviorSubject<Double>>) {
        this.coinValues = coinValues
        this.rates = rates
        this.progresses = progresses

        updateView()
    }

    override fun didUpdate(coinValue: CoinValue, adapterId: String) {
        coinValues[adapterId] = coinValue

        updateView()
    }

    override fun didExchangeRateUpdate(rates: MutableMap<Coin, CurrencyValue>) {
        this.rates = rates

        updateView()
    }

    private fun updateView() {
        var totalBalance = 0.0
        val viewItems = mutableListOf<WalletBalanceViewItem>()
        var baseCurrency: Currency? = null

        for (item in coinValues) {

            val adapterId = item.key
            val coinValue = item.value
            val exchangeRateValue = rates[coinValue.coin]
            var currencyValue: CurrencyValue? = null

            exchangeRateValue?.let {
                val valueInFiat = it.value * coinValue.value
                currencyValue = CurrencyValue(it.currency, valueInFiat)
                totalBalance += valueInFiat
                baseCurrency = it.currency
            }

            viewItems.add(
                    WalletBalanceViewItem(
                            adapterId = adapterId,
                            coinValue = coinValue,
                            exchangeRateValue = exchangeRateValue,
                            currencyValue = currencyValue,
                            progress = progresses[adapterId]
                    )
            )
        }

        baseCurrency?.let {
            view?.showTotalBalance(CurrencyValue(it, totalBalance))
        }
        view?.showWalletBalances(viewItems)
    }

}
