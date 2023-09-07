package com.journcorp.journcart.productDetails

import android.app.Application
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.journcorp.journcart.R
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.models.PostResult
import com.journcorp.journcart.core.models.ShippingCosts
import com.journcorp.journcart.core.utils.Constants
import com.journcorp.journcart.core.utils.Encryption
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.viewModels.BaseMainViewModel
import com.journcorp.journcart.productDetails.dataClass.Product
import com.journcorp.journcart.productDetails.dataClass.ReviewDetails
import com.journcorp.journcart.productDetails.dataClass.ShippingDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONException
import java.util.*

class ProductViewModel(
    private val database: MainRoomDatabase,
    private val app: Application,
    private val productId: String,
) : BaseMainViewModel(app) {

    private val productRepository = ProductRepository(database)

    private var webSocket: WebSocket? = null
    val webSocketError =
        MutableStateFlow(false)

    private val _productDataStatus = MutableLiveData<Resource<Product?>>()
    val productDataStatus: LiveData<Resource<Product?>> = _productDataStatus

    private val _shippingRegionsDataStatus = MutableLiveData<List<ShippingCosts>>()
    val shippingRegionsDataStatus: LiveData<List<ShippingCosts>> = _shippingRegionsDataStatus

    private val _shippingRegionsCountDataStatus = MutableLiveData<Int>()
    val shippingRegionsCountDataStatus: LiveData<Int> = _shippingRegionsCountDataStatus

    private val _auctionDetails = MutableLiveData<HashMap<String, String>>()
    val auctionDetails: LiveData<HashMap<String, String>> = _auctionDetails

    private val _cartDataStatus = MutableLiveData<Resource<PostResult?>>()
    val cartDataStatus: LiveData<Resource<PostResult?>> = _cartDataStatus

    private val _wishlistDataStatus = MutableLiveData<Resource<PostResult?>>()
    val wishlistDataStatus: LiveData<Resource<PostResult?>> = _wishlistDataStatus

    private val _countryDataStatus = MutableLiveData<Resource<ShippingDetails?>>()
    val countryDataStatus: LiveData<Resource<ShippingDetails?>> = _countryDataStatus

    private val _getShippingDataStatus = MutableLiveData<Resource<List<ShippingDetails>?>>()
    val getShippingDataStatus: LiveData<Resource<List<ShippingDetails>?>> = _getShippingDataStatus

    private val _setShippingDataStatus = MutableLiveData<Resource<ShippingDetails?>>()
    val setShippingDataStatus: LiveData<Resource<ShippingDetails?>> = _setShippingDataStatus

    /*private val _auctionDataStatus = MutableLiveData<Resource<PostResult?>>()
    val auctionDataStatus: LiveData<Resource<PostResult?>> = _auctionDataStatus*/

    private val _reviewsListStatus = MutableLiveData<Resource<List<ReviewDetails>?>>()
    val reviewsListStatus: LiveData<Resource<List<ReviewDetails>?>> = _reviewsListStatus


    fun getProductDetails(productID: String) {
        _productDataStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val data = productRepository.getProductDetails(productID)
            _productDataStatus.postValue(data)

            data.data?.shipping_id?.let {
                getShippingRegions(productId, it)
            }

            if (data.data?.pdt_type == 2 && Constants.userId != "") {//connect only for auctions

                val calender = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val now = calender.timeInMillis / 1000
                val distance = data.data.promo_expiry - now

                if (distance > 0) {//hasn't yet expired
                    instantiateWebSocket()
                }
            }
        }
    }

    fun getShippingRegions(productID: String, shippingID: String, start: Int = 0, total: Int = 10) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.getProductShippingDetails(productID, shippingID)

            val data2 = productRepository.shippingCostsDetails(shippingID, start, total).asFlow()
            data2.collectLatest { ship ->
                _shippingRegionsDataStatus.postValue(ship)
            }
        }
    }

    fun getCountShippingRegions(shippingID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data2 = productRepository.countShippingRegions(shippingID).asFlow()
            data2.collectLatest { ship ->
                _shippingRegionsCountDataStatus.postValue(ship)
            }
        }
    }

    fun addToCart(
        deviceKey: String,
        variableId: String,
        url: String,
        productId: String,
        cartVarsArr: String,
        shippingId: String,
        qty: Int,
        country: String,
        regionId: String
    ) {
        _cartDataStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val data = productRepository.addToCart(
                deviceKey,
                variableId,
                url,
                productId,
                cartVarsArr,
                shippingId,
                qty,
                country,
                regionId
            )
            _cartDataStatus.postValue(data)
        }
    }

    fun addToWishlist(deviceKey: String, url: String, productId: String) {
        _wishlistDataStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val data = productRepository.addToWishlist(deviceKey, url, productId)
            _wishlistDataStatus.postValue(data)
        }
    }

    fun changeUserCountry(deviceKey: String, productId: String, country: String) {
        _countryDataStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val data = productRepository.changeUserCountry(deviceKey, productId, country)
            _countryDataStatus.postValue(data)
        }
    }

    fun getShippingMethods(deviceKey: String, productId: String, shipping: String) {
        _getShippingDataStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val data = productRepository.getShippingMethod(deviceKey, productId, shipping)
            _getShippingDataStatus.postValue(data)
        }
    }

    fun setShippingMethods(deviceKey: String, productId: String, shippingId: String) {
        _setShippingDataStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val data = productRepository.setShippingMethod(deviceKey, productId, shippingId)
            _setShippingDataStatus.postValue(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun submitAuction(bidInput: Double) {
        //_auctionDataStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val key = Constants.hashedKey
            val hashMap = hashMapOf("bid_input" to bidInput)
            val data = Gson().toJson(hashMap)
            Log.i("submitAuction", key.toString())
            Log.i("submitAuction 1", data.toString())
            val enc = Encryption.encrypt(data, key)
            webSocket?.send(enc)
            //val data = productRepository.submitAuction(deviceKey,productId,bidInput)
            //_auctionDataStatus.postValue(data)
        }
    }

    fun getReviewsList(deviceKey: String, productId: String, start: Int, total: Int) {
        _reviewsListStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val data = productRepository.getReviewsList(deviceKey, productId, start, total)
            _reviewsListStatus.postValue(data)
        }
    }

    private fun instantiateWebSocket() {
        webSocket?.close(1000, null)
        webSocket = null

        backgroundScope.launch {
            webSocketError.emit(false)
        }
        val client = OkHttpClient()
        //replace x.x.x.x with your machine's IP Address
        val url =
            Constants.wsUrl + "?platform_token=" + Constants.deviceKey + "&action=product&sub_action=" + productId
        val request = Request.Builder().url(url).build()
        val socketListener = SocketListener()
        webSocket = client.newWebSocket(request, socketListener)
    }

    inner class SocketListener :
        WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            uiScope.launch {
                Toast.makeText(
                    app.applicationContext,
                    app.resources.getString(R.string.connected_to_live_server),
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onMessage(webSocket: WebSocket, text: String) {
            uiScope.launch {
                try {
                    val key = Constants.hashedKey
                    //val enc: String = Encryption.encrypt(string,key)
                    val dec: String = Encryption.decrypt(text, key)

                    val received = Gson().fromJson(dec, HashMap::class.java)
                    val dataType = received["dataType"].toString()

                    //update watchlist table
                    if (dataType == "auction") {
                        val isMe = received["is_me"].toString()
                        val unitPrice = received["unit_price"].toString()
                        val hashMap = hashMapOf("isMe" to isMe, "unitPrice" to unitPrice)
                        _auctionDetails.postValue(hashMap)
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            onFailure(webSocket, Exception("closed"), null)
            super.onClosed(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            backgroundScope.launch {
                webSocketError.emit(true)
            }

            super.onFailure(webSocket, t, response)
        }
    }


    override fun onCleared() {
        webSocket?.close(1000, null)
        webSocket = null
        super.onCleared()
    }
}
