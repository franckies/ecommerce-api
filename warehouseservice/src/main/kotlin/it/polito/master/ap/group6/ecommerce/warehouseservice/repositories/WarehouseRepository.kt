package it.polito.master.ap.group6.ecommerce.warehouseservice.repositories

import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Product
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WarehouseRepository: MongoRepository<Product, String> {

//    @Query("{ 'currentPrice' : { \$lt : ?0 } }")
//    fun findProductBelow(price: Double): List<Product>?
//
//    @Query("{ 'currentPrice' : { \$gt: ?0, \$lt: ?1 } }")
//    fun findProductBetween(priceGT: Double, priceLT: Double): List<Product>?

    @Query(value="{ }", fields = "{}")
    fun getProductsTotals() : List<Product>

    @Query(value="{ }", fields = "{}")
    fun getProductsPerWarehouse() : List<Product>

//    @Query(value="{'name' : ?0 , 'category' : ?1 }")
    fun getProductByNameAndCategory(name: String, category: String) : Optional<Product>

}
