package com.journcorp.journcart.productDetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.journcorp.journcart.core.RetroInstance
import com.journcorp.journcart.core.databases.MainRoomDatabase
import com.journcorp.journcart.core.entities.asDatabaseModel
import com.journcorp.journcart.core.entities.asDomainModel
import com.journcorp.journcart.core.models.PostResult
import com.journcorp.journcart.core.models.ShippingCosts
import com.journcorp.journcart.core.utils.Resource
import com.journcorp.journcart.core.utils.safeCall
import com.journcorp.journcart.core.utils.safeCallBoolean
import com.journcorp.journcart.productDetails.dataClass.Product
import com.journcorp.journcart.productDetails.dataClass.ReviewDetails
import com.journcorp.journcart.productDetails.dataClass.ShippingDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(datasource: MainRoomDatabase) {
    private val instance = RetroInstance.getRetroInstance()
    private val retroService = instance.create(ProductRetroInterface::class.java)

    private val shippingCostsDAO = datasource.shippingCostsDAO

    fun shippingCostsDetails(shippingId: String, start: Int, total: Int): LiveData<List<ShippingCosts>> =
        shippingCostsDAO.getAllById(shippingId, start, total).map {
            it.asDomainModel()
        }

    fun countShippingRegions(shippingId: String): LiveData<Int> =
        shippingCostsDAO.countAll(shippingId)

    suspend fun getProductDetails(productID: String): Resource<Product?> {
        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.getProductData(productID)
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    suspend fun getProductShippingDetails(productID: String, shippingID: String): Boolean {
        return withContext(Dispatchers.IO) {
            safeCallBoolean {
                val response = retroService.getProductShippingData(productID, shippingID)
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        val id = productID + shippingID
                        shippingCostsDAO.deleteById(id)
                        shippingCostsDAO.insert(*body.asDatabaseModel())
                    }

                    Resource.Success(true)
                } else {
                    Resource.Error(false.toString())
                }
            }
        }
    }

    suspend fun addToCart(
        deviceKey: String,
        variableId: String,
        url: String,
        productId: String,
        cartVarsArr: String,
        shippingId: String,
        qty: Int,
        country: String,
        regionId: String
    ): Resource<PostResult?> {

        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.addToCart(
                    path_id = productId,
                    deviceKey = deviceKey,
                    variableId = variableId,
                    url = url,
                    product_id = productId,
                    cart_vars_arr = cartVarsArr,
                    shipping_id = shippingId,
                    qty = qty,
                    country = country,
                    regionId = regionId
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    suspend fun addToWishlist(
        deviceKey: String, url: String, productId: String
    ): Resource<PostResult?> {

        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.addToWishlist(
                    path_id = productId, deviceKey = deviceKey, url = url, product_id = productId
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    suspend fun changeUserCountry(
        deviceKey: String, productId: String, country: String
    ): Resource<ShippingDetails?> {

        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.setUserCountry(
                    deviceKey = deviceKey, path_id = productId, country = country
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    suspend fun getShippingMethod(
        deviceKey: String, productId: String, shipping: String
    ): Resource<List<ShippingDetails>?> {

        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.getShippingMethods(
                    deviceKey = deviceKey, path_id = productId, shipping = shipping
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    suspend fun setShippingMethod(
        deviceKey: String, productId: String, shippingId: String
    ): Resource<ShippingDetails?> {

        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.setShippingMethod(
                    deviceKey = deviceKey,
                    path_id = productId,
                    productId = productId,
                    shippingId = shippingId
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    suspend fun submitAuction(//not valid for now since we are using websockets and this is done in the viewModel
        deviceKey: String, productId: String, bidInput: Double
    ): Resource<PostResult?> {

        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.submitAuction(
                    deviceKey = deviceKey, path_id = productId, bidInput = bidInput
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

    suspend fun getReviewsList(
        deviceKey: String, productId: String, start: Int, total: Int
    ): Resource<List<ReviewDetails>?> {

        return withContext(Dispatchers.IO) {
            safeCall {
                val response = retroService.getReviewsList(
                    deviceKey = deviceKey, path_id = productId, start = start, total = total
                )
                if (response.isSuccessful) {
                    Resource.Success(response.body())
                } else {
                    Resource.Error(response.message())
                }
            }
        }
    }

}
