package com.journcorp.journcart.orders.models

data class OrdersResult(
    val page: Int,
    val result: List<Orders>
)

data class Orders(
    val order_id: String,
    val order_status: Int,
    val country: String,
    val paid: Int,
    val url: String,
    val pdt_name: String,
    val product_id: String,
    val qty: Int,
    val variations: String,
    val currency: String = "",
    val subtotal: Double,
    val shipping_total: Double,
    val total: Double,
    val time_added: String,
    val title: String,
    val main_img: String
)
