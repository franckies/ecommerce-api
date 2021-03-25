package it.polito.master.ap.group6.ecommerce.warehouseservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.DeliveryLog
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.DeliveryLogStatus
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Product
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.WarehouseStock
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.DeliveryLogRepository
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.WarehouseRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.util.*

interface WarehouseService {

    fun getProductsTotals(): ProductListDTO
    fun getProductsPerWarehouse(): ProductListAdminDTO
    fun insertNewProductInWarehouse(productAdminDTO: ProductAdminDTO): ProductAdminDTO? // return productAdminDTO if inserted, null otherwise
    fun checkAvailability(orderDTO: OrderDTO): Boolean?
    fun getDeliveries(orderDTO: OrderDTO): DeliveryListDTO? // return filled DeliveryList if requested products are available, empty DeliveryList if not available, null if do not exist
    fun updateStocksAfterDeliveriesCancellation(orderID: String) : Boolean // return true if restore of product quantities is ok, false otherwise
    fun updateProductInWarehouse(productID:String, productAdminDTO: ProductAdminDTO) : ProductAdminDTO? // return productAdminDTO if updated, null otherwise
    fun getProductByID(productID: String) : Product?
}

@Service
class WarehouseServiceImpl(
    private val warehouseRepository: WarehouseRepository, private val deliveryLogRepository: DeliveryLogRepository
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

    override fun getProductsPerWarehouse(): ProductListAdminDTO {

        // Query to MongoDB
        val queriedProducts: List<Product> = warehouseRepository.getProductsPerWarehouse()

        // Convert result into ProductListAdminDTO
        val productList = mutableListOf<ProductAdminDTO>()
        for (product in queriedProducts) {
            val prodDTO = ProductDTO(id = product.id.toString(), name = product.name, category = product.category, currentPrice = product.currentPrice)
            for (st in product.stock!!) {
                val productAdminDTO = ProductAdminDTO(
                    product = prodDTO,
                    warehouse = WarehouseDTO(name = st.warehouseName!!, address = st.warehouseAddress),
                    warehouseQuantity = st.availableQuantity!!,
                    alarmLevel = getAlarmLevel(st.availableQuantity!!)
                )
                productList.add(productAdminDTO)
            }
        }
        return ProductListAdminDTO(productList = productList)
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
            availableQuantity = productAdminDTO.warehouseQuantity, alarmLevel = getAlarmLevel(productAdminDTO.warehouseQuantity)
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

    override fun getDeliveries(orderDTO: OrderDTO) : DeliveryListDTO? {

        when (checkAvailability(orderDTO) ) {
            null -> {
                println("ERROR: Requested products not in MongoDB")
                return null
            }
            false -> {
                println("Requested products not available")
//                deliveryLogRepository.save(
//                    DeliveryLog(orderID = orderDTO.orderID, deliveries =  null, status = DeliveryLogStatus.NOT_AVAILABLE, timestamp = Date())
//                )
                return DeliveryListDTO(orderDTO.orderID, null)
            }
            else -> {
                println("Requested products available: preparing deliveries from each warehouse")

                // TODO: Check in deliveryLog if Order was already shipped??

                val mapDeliveries : MutableMap<String, MutableList<PurchaseDTO>> = mutableMapOf() // To be converted in DeliveryListDTO? before return
                for (purchase in orderDTO.purchases!!) {
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
                                requestedProduct.stock[i].alarmLevel = getAlarmLevel(0)
                            } else {
                                givenQuantity = remainingQuantity
                                requestedProduct.stock[i].availableQuantity =
                                    requestedProduct.stock[i].availableQuantity!! - remainingQuantity
                                requestedProduct.stock[i].alarmLevel = getAlarmLevel(requestedProduct.stock[i].availableQuantity!!)
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
                val deliveryListDTO = DeliveryListDTO(orderDTO.orderID, deliveryList)

                deliveryLogRepository.save(
                    DeliveryLog(orderID = orderDTO.orderID, deliveries =  deliveryList, status = DeliveryLogStatus.SHIPPED, timestamp = Date())
                )
                return deliveryListDTO
            }
        }
    }



    // Assume the products and the corresponding warehouse are actually present in the DB
    override fun updateStocksAfterDeliveriesCancellation(orderID: String) : Boolean {

        val deliveryLog = deliveryLogRepository.getDeliveryLogByOrderID(orderID)
        if (deliveryLog.isEmpty) {
            println("ERROR: Order not found in the log repository")
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
                            stock.alarmLevel = getAlarmLevel(stock.availableQuantity!!)
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
                stock.alarmLevel = getAlarmLevel(productAdminDTO.warehouseQuantity)
                requestedWarehouseAlreadyExist = true
                break
            }
        }
        if (!requestedWarehouseAlreadyExist) { // The product is not stored in that warehouse
            updateStock.add(WarehouseStock(
                productAdminDTO.warehouse!!.name, productAdminDTO.warehouse!!.address,
                productAdminDTO.warehouseQuantity, getAlarmLevel(productAdminDTO.warehouseQuantity)
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

    override fun checkAvailability(orderDTO: OrderDTO): Boolean? {
        var areProductQuantitiesAvailable: Boolean = true
        for (purchase in orderDTO.purchases!!) {
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

    fun getAlarmLevel(availableQuantity: Int?) : Int? {
        if (availableQuantity!=null) {
            var alarmLevel : Int? = null
            when {
                availableQuantity >= 50 -> alarmLevel = 0
                availableQuantity >= 10 -> alarmLevel = 1
                availableQuantity < 10 -> alarmLevel = 2
            }
            return alarmLevel
        } else
            return null
    }
}




