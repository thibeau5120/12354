package com.liberty.apps.studio.libertyvpn.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.liberty.apps.studio.libertyvpn.AppSettings.Companion.one_month_subscription_id
import com.liberty.apps.studio.libertyvpn.AppSettings.Companion.one_year_subscription_id
import com.liberty.apps.studio.libertyvpn.AppSettings.Companion.three_month_subscription_id

class BillingClass(private val refActivity: Activity) : PurchasesUpdatedListener,
    BillingClientStateListener {
    private val billingClient: BillingClient
    private val skuListSubscriptionsList: MutableList<String>?
    private var skuListFromStore: List<SkuDetails>? = null

    //others
    var isAvailable = false
        private set
    var isListGot = false
        private set
    private var mCallback: BillingErrorHandler? = null
    private var mDetailsCallback: SkuDetailsListener? = null

    //step-1 init
    init {
        billingClient = BillingClient.newBuilder(refActivity)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        skuListSubscriptionsList = ArrayList()

        //add all products here (subscriptions)
        skuListSubscriptionsList.add(one_month_subscription_id)
        skuListSubscriptionsList.add(three_month_subscription_id)
        skuListSubscriptionsList.add(one_year_subscription_id)
    }

    //step-2 make connection to store
    fun startConnection() {
        billingClient.startConnection(this)
    }

    fun setmCallback(mCallback: BillingErrorHandler?, skuDetailsListener: SkuDetailsListener?) {
        if (this.mCallback == null) {
            this.mCallback = mCallback
        }
        if (mDetailsCallback == null) {
            mDetailsCallback = skuDetailsListener
        }
    }

    //purchase update listener
    //step-5
    override fun onPurchasesUpdated(billingResult: BillingResult, list: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
            && list != null
        ) {
            for (purchase in list) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    //step-3
    //state listener
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            mCallback!!.displayErrorMessage("done")

            //proceed, Client is ready
            isAvailable = true
            val subscriptionParams = SkuDetailsParams.newBuilder()
            val oneTimeProductsParams = SkuDetailsParams.newBuilder()
            subscriptionParams.setSkusList(skuListSubscriptionsList!!)
                .setType(BillingClient.SkuType.SUBS)
            if (!skuListSubscriptionsList.isEmpty()) {
                billingClient.querySkuDetailsAsync(subscriptionParams.build()) { billingResult, list -> //subscription list
                    skuListFromStore = list
                    isListGot = true
                    mDetailsCallback!!.subscriptionsDetailList(list)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE || billingResult.responseCode == BillingClient.BillingResponseCode.ERROR || billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
            mCallback!!.displayErrorMessage("error")
        }
    }

    override fun onBillingServiceDisconnected() {
        //restart, No connection to billing service
        isAvailable = false
    }

    //step-4
    fun purchaseSubscriptionItemByPos(itemIndex: Int): String {
        var index = itemIndex
        for (i in skuListFromStore!!.indices) {
            if (skuListFromStore!![i].sku == skuListSubscriptionsList!![itemIndex]) {
                index = i
            }
        }
        if (billingClient.isReady) {
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuListFromStore!![index])
                .build()
            val responseCode = billingClient.launchBillingFlow(refActivity, billingFlowParams)
            when (responseCode.responseCode) {
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                    mCallback!!.displayErrorMessage("Billing not supported for type of request")
                    return "Billing not supported for type of request"
                }

                BillingClient.BillingResponseCode.ITEM_NOT_OWNED, BillingClient.BillingResponseCode.DEVELOPER_ERROR -> return ""
                BillingClient.BillingResponseCode.ERROR -> {
                    mCallback!!.displayErrorMessage("Error completing request")
                    return "Error completing request"
                }

                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> return "Error processing request."
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> return "Selected item is already owned"
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> return "Item not available"
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> return "Play Store service is not connected now"
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> return "Timeout"
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                    mCallback!!.displayErrorMessage("Network error.")
                    return "Network Connection down"
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    mCallback!!.displayErrorMessage("Request Canceled")
                    return "Request Canceled"
                }

                BillingClient.BillingResponseCode.OK -> return "Subscribed Successfully"
            }
        }
        return ""
    }

    fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { }
        }
    }

    fun isSubscribedToSubscriptionItem(sku: String): Boolean {
        if (skuListSubscriptionsList != null) {
            val result = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
            if (result.responseCode == BillingClient.BillingResponseCode.OK && result.purchasesList != null) {
                for (purchase in result.purchasesList!!) {
                    return if (purchase.sku == sku) {
                        true
                    } else {
                        false
                    }
                }
            }
        }
        return false
    }

    //message interface
    interface BillingErrorHandler {
        fun displayErrorMessage(message: String?)
    }

    interface SkuDetailsListener {
        fun subscriptionsDetailList(skuDetailsList: List<SkuDetails>?)
    }
}