package com.journcorp.journcart.productDetails.dataClass

data class ShippingDetails(
    val shipping: String = "",//json array
    val logo: String = "",
    val shipping_id: String = "",
    val shipping_type: Int = 0,
    val shipping_name: String = "",
    val shipping_regions: String = "",
    val estimated_time_in: Int = 0,
    val estimated_time_out: Int = 0,
)
