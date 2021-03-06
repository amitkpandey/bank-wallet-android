package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IExchangeRateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.coins.Coin
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ExchangeRateManager(currencyManager: ICurrencyManager): IExchangeRateManager {

    private var disposables: CompositeDisposable = CompositeDisposable()

    init {
        fetchRatesWithInterval(currencyManager.baseCurrency)
        val baseCurrencyDisposable = currencyManager.subject.subscribe { baseCurrency ->
            fetchRatesWithInterval(baseCurrency)
        }
    }

    private fun fetchRatesWithInterval(baseCurrency: Currency) {
        disposables.clear()
        disposables.add(Observable.interval(5, 180, TimeUnit.SECONDS, Schedulers.io())
                .subscribe {
                    refreshRates(baseCurrency)
                })
    }

    private var latestExchangeRateSubject: PublishSubject<MutableMap<Coin, CurrencyValue>> = PublishSubject.create()

    override fun getLatestExchangeRateSubject() = latestExchangeRateSubject

    private var exchangeRates: MutableMap<Coin, CurrencyValue> = hashMapOf()

    override fun getExchangeRates() = exchangeRates

    private fun refreshRates(baseCurrency: Currency) {
        val flowableList = mutableListOf<Flowable<Pair<String, Double>>>()
        App.adapterManager.adapters.forEach { adapter ->
            flowableList.add(App.networkManager.getLatestRate(adapter.coin.code, baseCurrency.code)
                    .map { Pair(adapter.coin.code, it) })
        }

        disposables.add(Flowable.zip(flowableList, Arrays::asList)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .map {resultRates ->
                    (resultRates as List<Pair<String, Double>>).toMap()
                }
                .subscribe { ratesMap ->
                    App.adapterManager.adapters.forEach { adapter ->
                        val rate = ratesMap[adapter.coin.code] ?: 0.0
                        exchangeRates[adapter.coin] = CurrencyValue(baseCurrency, rate)
                    }
                    latestExchangeRateSubject.onNext(exchangeRates)
                })
    }

    override fun getRate(coinCode: String, currency: String, timestamp: Long): Flowable<Double> {
        val calendar = DateHelper.getCalendarFromTimestamp(timestamp)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val f = DecimalFormat("00")
        val formattedMonth = f.format(month).toString()
        val formattedDay = f.format(day).toString()
        val formattedHour = f.format(hour).toString()
        val formattedMinute = f.format(minute).toString()

        return App.networkManager.getRate(coinCode, currency, year, formattedMonth, formattedDay, formattedHour, formattedMinute)
    }

}
