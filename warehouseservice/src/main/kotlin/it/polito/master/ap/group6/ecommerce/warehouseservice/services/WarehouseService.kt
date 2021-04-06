package it.polito.master.ap.group6.ecommerce.warehouseservice.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.common.misc.MicroService
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.DeliveryLog
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.DeliveryLogStatus
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Product
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.WarehouseStock
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.DeliveryLogRepository
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.WarehouseRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.*
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.annotation.Transactional

interface WarehouseService {

    fun getProductsTotals(): ProductListDTO
//    fun getProductsPerWarehouse(): ProductListAdminDTO
    fun getProductsPerWarehouse(): ProductListWarehouseDTO
    fun insertNewProductInWarehouse(productAdminDTO: ProductAdminDTO): ProductAdminDTO? // return productAdminDTO if inserted, null otherwise
    fun checkAvailability(purchases: List<PurchaseDTO>): Boolean?
    fun getDeliveries(orderID : String, purchases : List<PurchaseDTO>?, deliveryAddress: String?): DeliveryListDTO? // return filled DeliveryList if requested products are available, empty DeliveryList if not available, null if do not exist
    fun updateStocksAfterDeliveriesCancellation(orderID: String) : Boolean // return true if restore of product quantities is ok, false otherwise
    fun updateProductInWarehouse(productID:String, productAdminDTO: ProductAdminDTO) : ProductAdminDTO? // return productAdminDTO if updated, null otherwise
    fun getProductByID(productID: String) : Product?
}

@Service
@Transactional
class WarehouseServiceImpl(
    private val warehouseRepository: WarehouseRepository,
    private val deliveryLogRepository: DeliveryLogRepository,
    private val kafkaProducts: KafkaTemplate<String, String>,
    private val kafkaRollback: KafkaTemplate<String, String>,
    private val kafkaAlarmLevel: KafkaTemplate<String, String>,
) : WarehouseService {

    override fun getProductsTotals(): ProductListDTO {

        // Query to MongoDB
        val queriedProducts: List<Product> = warehouseRepository.getProductsTotals()

        // Convert result into ProductListDTO
        val productListDTO = ProductListDTO(products = mutableListOf())
        for (product in queriedProducts) {
            var availableQuantity = 0
            for (st in product.stock!!) {
                availableQuantity += st.availableQuantity!!
            }
            val prodDTO = ProductDTO(id = product.id.toString(), name = product.name, category = product.category, currentPrice = product.currentPrice)
            productListDTO.products?.add(ProductQuantityDTO(prodDTO, availableQuantity))
        }
        return productListDTO
    }

//    override fun getProductsPerWarehouse(): ProductListAdminDTO {
    override fun getProductsPerWarehouse(): ProductListWarehouseDTO {

        // Query to MongoDB
        val queriedProducts: List<Product> = warehouseRepository.getProductsPerWarehouse()

        val products = mutableListOf<ProductWarehouseDTO>()
        for (product in queriedProducts) {
            val stock = mutableListOf<WarehouseStockDTO>()
            for (st in product.stock!!) {
                stock.add(WarehouseStockDTO(
                    warehouseName = st.warehouseName,
                    warehouseAddress = st.warehouseAddress,
                    availableQuantity = st.availableQuantity,
                    alarmLevel = st.alarmLevel
                ))
            }
            val prodDTO = ProductWarehouseDTO(
                id = product.id.toString(),
                name = product.name,
                category = product.category,
                currentPrice = product.currentPrice,
                description = product.description,
                picture = product.picture,
                stock = stock
            )
            products.add(prodDTO)
        }
        return ProductListWarehouseDTO(products = products)
    }

    override fun insertNewProductInWarehouse(productAdminDTO: ProductAdminDTO): ProductAdminDTO? {

        // Check if product already exist (by name and category)
        if (getProductByNameAndCategoryAndDescription(productAdminDTO.product!!) != null) {
            println("Product already exists (same name, same category, same description)")
            return null
        }

        // Convert ProductAdminDTO to Product
        val warehouse = WarehouseStock(
            warehouseName = productAdminDTO.warehouse?.name, warehouseAddress = productAdminDTO.warehouse?.address,
            availableQuantity = productAdminDTO.warehouseQuantity, alarmLevel = productAdminDTO.warehouseQuantity
        )
        val productToStore = Product(
            name = productAdminDTO.product?.name, category = productAdminDTO.product?.category,
            description = productAdminDTO.product?.description, currentPrice = productAdminDTO.product?.currentPrice,
            stock = mutableListOf<WarehouseStock>(warehouse)
        )

        // Save to MongoDB
        var result : ProductAdminDTO? = null
        try {
            warehouseRepository.save(productToStore)
            result = productAdminDTO
        } catch (e: Exception) {
            println("Exception on warehouseRepository.save(productToStore) : $e")
        }
        return result
    }

//    override fun getDeliveries(orderDTO: OrderDTO) : DeliveryListDTO? {
    override fun getDeliveries(orderID : String, purchases : List<PurchaseDTO>?, deliveryAddress: String?) : DeliveryListDTO? {

        when (checkAvailability(purchases!!) ) {
            null -> {
                println("ERROR: Requested products not in MongoDB")
                return null
            }
            false -> {
                println("Requested products not available")
                return DeliveryListDTO(orderID, null, deliveryAddress = null)
            }
            else -> {
                println("Requested products available: preparing deliveries from each warehouse")

                val deliveryLog : Optional<DeliveryLog>
                try {
                    deliveryLog = deliveryLogRepository.getDeliveryLogByOrderID(orderID!!)
                    if (!deliveryLog.isEmpty) {
                        println("ERROR: This orderID was already requested for delivery.")
                        return null
                    }
                } catch (e: Exception) {
                    println("getDeliveries Exception: $e")
                    return null
                }

                val mapDeliveries : MutableMap<String, MutableList<PurchaseDTO>> = mutableMapOf() // To be converted in DeliveryListDTO? before return
                for (purchase in purchases) {
                    val requestedProduct = this.getProductByID(purchase.productID!!)!!
                    var remainingQuantity = purchase.quantity!!
                    var i = 0
                    while (remainingQuantity > 0) {
                        if (requestedProduct.stock != null) {
                            var givenQuantity = 0
                            if (remainingQuantity > requestedProduct.stock[i].availableQuantity!!) {
                                givenQuantity = requestedProduct.stock[i].availableQuantity!!
                                remainingQuantity -= requestedProduct.stock[i].availableQuantity!!
                                requestedProduct.stock[i].availableQuantity = 0
                                val message = "ALERT! Product ${requestedProduct.id} in warehouse ${requestedProduct.stock[i].warehouseName} : available quantities are 0."
                                kafkaAlarmLevel.send("alarm_level", message)
                            } else {
                                givenQuantity = remainingQuantity
                                requestedProduct.stock[i].availableQuantity =
                                    requestedProduct.stock[i].availableQuantity!! - remainingQuantity
                                if (requestedProduct.stock[i].availableQuantity!! <= requestedProduct.stock[i].alarmLevel!!) {
                                    val message = "ALERT! Product ${requestedProduct.id} in warehouse ${requestedProduct.stock[i].warehouseName} : available products are ${requestedProduct.stock[i].availableQuantity!!}, alarm level is ${requestedProduct.stock[i].alarmLevel!!}"
                                    kafkaAlarmLevel.send("alarm_level", message)
                                }
                                remainingQuantity = 0
                            }
                            if (givenQuantity!=0) {
                                val warehouseName = requestedProduct.stock[i].warehouseName!!
                                val purchaseWithWarehouseQuantity = PurchaseDTO(productID = purchase.productID, quantity = givenQuantity, sellingPrice = purchase.sellingPrice)
                                if (mapDeliveries[warehouseName] == null) {
                                    mapDeliveries[warehouseName] = mutableListOf(purchaseWithWarehouseQuantity)
                                } else {
                                    mapDeliveries[warehouseName]!!.add(purchaseWithWarehouseQuantity)
                                }
                            }
                        }
                        i += 1
                    }
                    // Update Product on MongoDB
                    warehouseRepository.save(requestedProduct)
                }
                // Convert map to DeliveryList and return
                val deliveryList : MutableList<DeliveryDTO> = mutableListOf()
                for ((key, value) in mapDeliveries) {
                    deliveryList.add(DeliveryDTO(key, value))
                }
                val deliveryListDTO = DeliveryListDTO(orderID, deliveryList, deliveryAddress)

                deliveryLogRepository.save(
                    DeliveryLog(orderID = orderID, deliveries =  deliveryList, status = DeliveryLogStatus.SHIPPED, timestamp = Date())
                )
                return deliveryListDTO
            }
        }
    }



    override fun updateStocksAfterDeliveriesCancellation(orderID: String) : Boolean {

        val deliveryLog : Optional<DeliveryLog>
        try {
            deliveryLog = deliveryLogRepository.getDeliveryLogByOrderID(orderID)
        }
        catch (e: Exception) {
            println("getDeliveries Exception: $e")
            println("-> Skipping rollback")
            return false
        }
        if (deliveryLog.isEmpty) {
            println("OrderID not found in the log repository -> skipping rollback")
            return false
        }
        val deliveries = deliveryLog.get().deliveries!!
        for (deliveryDTO in deliveries) {
            val warehouseOfOrigin = deliveryDTO.warehouseID
            for (purchase in deliveryDTO.purchases!!) {
                    val documentToUpdate = this.getProductByID(purchase.productID!!) // Assume the product exists
                    val stockToUpdate = documentToUpdate?.stock

                    // Update the product of the interested warehouse
                    for (stock in stockToUpdate!!) {
                        if (stock.warehouseName == warehouseOfOrigin) // Assume name of the warehouse is the primary key
                        {
                            stock.availableQuantity = stock.availableQuantity!! + purchase.quantity!!
                            if (stock.availableQuantity!! <= stock.alarmLevel!!) {
                                val message = "ALERT! Product ${purchase.productID} in warehouse ${stock.warehouseName} : available products are ${stock.availableQuantity!!}, alarm level is ${stock.alarmLevel!!}"
                                kafkaAlarmLevel.send("alarm_level", message)
                            }
                            break
                        }
                    }
                    // Update result in MongoDB
                    warehouseRepository.save(documentToUpdate)
            }
        }
        deliveryLogRepository.save(
            DeliveryLog(orderID = orderID, deliveries = deliveries, status = DeliveryLogStatus.CANCELED, timestamp = Date())
        )
        return true
    }


    override fun updateProductInWarehouse(productID: String, productAdminDTO: ProductAdminDTO): ProductAdminDTO? {
        val productToUpdate : Product
        try {
            productToUpdate = warehouseRepository.findById(productID).get()
        } catch (e : Exception) {
            println("Exception on warehouseRepository.findById(productID).get() : product does not exist")
            return null
        }
        val updateStock = productToUpdate.stock
        var requestedWarehouseAlreadyExist = false
        for (stock in updateStock!!) { // The product is already in that warehouse
            if (stock.warehouseName == productAdminDTO.warehouse!!.name) {
                stock.warehouseAddress = productAdminDTO.warehouse!!.address
                stock.availableQuantity = productAdminDTO.warehouseQuantity
                stock.alarmLevel = productAdminDTO.warehouseQuantity
                requestedWarehouseAlreadyExist = true
                break
            }
        }
        if (!requestedWarehouseAlreadyExist) { // The product is not stored in that warehouse
            updateStock.add(WarehouseStock(
                productAdminDTO.warehouse!!.name, productAdminDTO.warehouse!!.address,
                productAdminDTO.warehouseQuantity, productAdminDTO.warehouseQuantity
            ))
        }
        val documentToUpdate = Product(
            // ID field does not change
            id = ObjectId(productID),

            // Eventually change price, picture, stock
            name = productAdminDTO.product!!.name,
            category = productAdminDTO.product!!.category,
            currentPrice = productAdminDTO.product!!.currentPrice,
            picture = productAdminDTO.product!!.picture,
            stock = updateStock
        )

        // Save to MongoDB
        var result : ProductAdminDTO? = null
        try {
            warehouseRepository.save(documentToUpdate)
            result = productAdminDTO
        } catch (e: Exception) {
            println(" Exception on warehouseRepository.save(documentToUpdate) : $e")
        }
        return result
    }


    fun getProductByNameAndCategoryAndDescription(product: ProductDTO): Product? {
        val result = warehouseRepository.getProductByNameAndCategoryAndDescription(product.name, product.category, product.description)
        try {
            return result.get()
        } catch (e: Exception) {
            println(" Exception on warehouseRepository.getProductByNameAndCategory : $e")
            return null
        }
    }

    override fun getProductByID(productID: String) : Product? {
        val queriedProduct = warehouseRepository.findById(productID)
        if (queriedProduct.isPresent)
            return queriedProduct.get()
        else {
            println("getProductByID return null")
            return null
        }
    }

    override fun checkAvailability(purchases: List<PurchaseDTO>): Boolean? {
        var areProductQuantitiesAvailable: Boolean = true
        for (purchase in purchases) {
        val requestedProduct = this.getProductByID(purchase.productID!!)
            if (requestedProduct == null)
                return null
            val arrayOfQuantities = mutableListOf<Int>()
            (requestedProduct.stock!!).forEach {
                arrayOfQuantities.add(it.availableQuantity!!)
            }
            if (purchase.quantity!! > arrayOfQuantities.sum()) {
                areProductQuantitiesAvailable = false
                break
            }
        }
        return areProductQuantitiesAvailable
    }


    /**
        Kafka listeners are implemented here
     */

    @KafkaListener(groupId = "warehouseservice", topics = ["create_order"])
    fun listener_create_order(placedOrderDTOString: String?) {
        println("Create order requested.")
        val placedOrderDTO = jacksonObjectMapper().readValue<PlacedOrderDTO>(placedOrderDTOString!!)

        val result = getDeliveries(orderID = placedOrderDTO?.sagaID!!, purchases = placedOrderDTO.purchaseList, deliveryAddress = placedOrderDTO.deliveryAddress)
        if (result==null) {
            println("DeliveryList is null : emitting Rollback Request.")
            val rollbackDTO = RollbackDTO(placedOrderDTO.sagaID, MicroService.WAREHOUSE_SERVICE)
            kafkaRollback.send("rollback", jacksonObjectMapper().writeValueAsString(rollbackDTO))
        } else {
            if (result.deliveryList!=null) {
                println("KafkaProducts : emitting the deliveryList.")
                kafkaProducts.send("products_ok", jacksonObjectMapper().writeValueAsString(result))

            } else {
                println("Requested products not available: emitting Rollback Request.")
                val rollbackDTO = RollbackDTO(placedOrderDTO.sagaID, MicroService.WAREHOUSE_SERVICE)
                kafkaRollback.send("rollback", jacksonObjectMapper().writeValueAsString(rollbackDTO))
            }
        }
    }

    @KafkaListener(groupId = "warehouseservice", topics = ["rollback"])
//    fun listener_rollback(orderID: String) {
    fun listener_rollback(rollbackDTOString : String) {
        val rollbackDTO = jacksonObjectMapper().readValue<RollbackDTO>(rollbackDTOString!!)
        println("Rollback Request received from ${rollbackDTO.sender}.")
        if (updateStocksAfterDeliveriesCancellation(rollbackDTO.sagaID!!)) {
            println("Rollback correctly done.")
        }
    }

    @KafkaListener(groupId = "warehouseservice", topics = ["cancel_order"])
    fun listener_cancel_order(orderID: String) {
        println("Cancel of order requested.")
        if (updateStocksAfterDeliveriesCancellation(orderID)) {
            println("Cancel of order correctly done.")
        }
    }

}




