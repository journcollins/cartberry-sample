package com.journcorp.journcart.productDetails.dataClass

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product (
    val pdt_type: Int = 0,
    val url: String = "",
    val event_tag: Int = 0,
    val main_img: String = "",
    val other_img: String = "",
    val vid: String = "",
    val pdt_name: String = "",
    val category: Int = 0,
    val pdt_condition: Int = 0,
    val currency: String = "",
    val unit_price: Double = 0.0,
    val old_unit_price: Double = 0.0,
    val promo_expiry: Int = 0,
    val avail_qty: Int = 0,
    val min_orders: Int = 0,
    val max_orders: Int = 0,
    val containment: String = "",
    val txt_desc: String = "",
    val attr_n: String = "",
    val attr_v: String = "",
    var shipping: String = "",
    val vars_arr: String = "",
    val orders: Int = 0,
    val one_star: Int = 0,
    val two_star: Int = 0,
    val three_star: Int = 0,
    val four_star: Int = 0,
    val five_star: Int = 0,
    val bid_count: Int = 0,
    val bid_user_id: String? = "",//is int in db but just in case
    val title: String = "",
    val certified: Int = 0,
    val avail_pdt_categories: String = "",
    val banner1: String = "",
    val star_total: Int = 0,
    val star_votes: Int = 0,
    var shipping_id: String = "",
    var shipping_type: Int = 0,
    var shipping_name: String = "",
    var shipping_currency: String = "",
    var shipping_regions: String = "",
    var estimated_time_in: Int = 0,
    var estimated_time_out: Int = 0,
    val event_name: String = "",
    val commence: Int = 0,
    val expire: Int = 0,
    val brand_id: String = "",
    val brand_name: String = ""
): Parcelable

data class ProductVars (
    val var0 : List<Var0>,
    val var1 : List<Var1>,
    val var2 : List<Var2>
)
data class Var0 (

    val variableType : String = "",
    val variableInitial : String = "",
    val varInitialStatus : String = "",
    val variableName : String = "",
    val variableQty : Int = 0,
    val variablePrice : Double = 0.0,
    val storeVariablePrice : Double = 0.0
)
data class Var1 (

    val variableType : String = "",
    val variableInitial : String = "",
    val varInitialStatus : String = "",
    val variableName : String = "",
    val variableQty : Int = 0,
    val variablePrice : Double = 0.0,
    val storeVariablePrice : Double = 0.0
)
data class Var2 (

    val variableType : String = "",
    val variableInitial : String = "",
    val varInitialStatus : String = "",
    val variableName : String = "",
    val variableQty : Int = 0,
    val variablePrice : Double = 0.0,
    val storeVariablePrice : Double = 0.0
)






