package com.journcorp.journcart.home.models

import com.journcorp.journcart.core.models.ProductSmall

data class ProductSmallAutoLoadResponse(
    val page: Int,
    val results: List<ProductSmall>
)
