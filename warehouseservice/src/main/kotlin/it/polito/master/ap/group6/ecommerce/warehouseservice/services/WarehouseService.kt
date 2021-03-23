package it.polito.master.ap.group6.ecommerce.warehouseservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Product
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.WarehouseStock
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.WarehouseRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestBody

interface WarehouseService {

    fun getProductsTotals(): ProductListDTO?
    fun getProductsPerWarehouse(): ProductListAdminDTO?
    fun insertNewProduct(productAdminDTO: ProductAdminDTO): ProductAdminDTO? // return productAdminDTO if inserted, null otherwise
    fun getProduct(product: ProductDTO): Product?
    fun checkAvailability(orderDTO: OrderDTO): Boolean
    fun getDeliveries(orderDTO: OrderDTO): DeliveryListDTO
    fun updateStocksAfterDeliveriesCancellation(deliveryListDTO: DeliveryListDTO) : Boolean
}

@Service
class WarehouseServiceImpl(private val warehouseRepository: WarehouseRepository) : WarehouseService {

    override fun getProductsTotals(): ProductListDTO? {

        // Query to MongoDB
        val queriedProducts: List<Product> = warehouseRepository.getProductsTotals()

        // Convert result into ProductListDTO
        val productListDTO = ProductListDTO(products = mutableMapOf())
        for (product in queriedProducts) {
            var availableQuantity = 0
            for (st in product.stock!!) {
                availableQuantity += st.availableQuantity!!
            }
            val pdto = ProductDTO(name = product.name, category = product.category, currentPrice = product.currentPrice)
            productListDTO.products[pdto] = availableQuantity
        }

        return productListDTO
    }

    override fun getProductsPerWarehouse(): ProductListAdminDTO? {

        // Query to MongoDB
        val queriedProducts: List<Product> = warehouseRepository.getProductsPerWarehouse()

        // Convert result into ProductListAdminDTO
        val productListP = mutableListOf<ProductAdminDTO>()
        for (product in queriedProducts) {
            val pdto = ProductDTO(name = product.name, category = product.category, currentPrice = product.currentPrice)
            for (st in product.stock!!) {
                val productAdminDTO = ProductAdminDTO(
                    product = pdto,
                    warehouse = WarehouseDTO(name = st.warehouseName!!),
                    warehouseQuantity = st.availableQuantity!!,
                    alarmLevel = st.alarmLevel!!
                )
                productListP.add(productAdminDTO)
            }
        }
        val productListAdminDTO = ProductListAdminDTO(productList = productListP)
        return productListAdminDTO
    }

    override fun insertNewProduct(productAdminDTO: ProductAdminDTO): ProductAdminDTO? {

        // Check if product already exist (by name and category)
        if (getProduct(productAdminDTO.product!!) != null) {
                println("Product already exists")
            return null
        }

        // Convert ProductAdminDTO to Product
        val warehouse = WarehouseStock(
            warehouseName = productAdminDTO.warehouse?.name, warehouseAddress = productAdminDTO.warehouse?.address,
            availableQuantity = productAdminDTO.warehouseQuantity, alarmLevel = productAdminDTO.alarmLevel
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
            println(e)
        }
        return result
    }

    override fun getDeliveries(orderDTO: OrderDTO) : DeliveryListDTO {

        // Check if products are available
        if (checkAvailability(orderDTO) == false) {
            // throw exception
            return DeliveryListDTO()
        }

        // Create list of deliveries for each purchase of the order (each purchase contains one product, one product can have associated more than one delivery)

        val deliveryList = mutableListOf<DeliveryDTO>() // List of DeliveryDTO to put into the DeliveryListDTO object
        val productsToUpdate = mutableListOf<Product>() // List of documents to update on MongoDB

        for (purchase in orderDTO.purchases!!) {

            val requestedProduct = this.getProduct(purchase.product!!)
            val stockToUpdate = requestedProduct?.stock

            var remainingQuantity = purchase.quantity!!
            for (warehouse in stockToUpdate!!) {
                if (warehouse.availableQuantity!! >= remainingQuantity) {

                    // Update stock
                    warehouse.availableQuantity = warehouse.availableQuantity!! - remainingQuantity

                    // Append delivery for this warehouse
                    val warehouseDTO = WarehouseDTO(name=warehouse.warehouseName, address = warehouse.warehouseAddress)
//                    val deliveryDTO = DeliveryDTO(warehouseDTO, mapOf( purchase.product!! to remainingQuantity ))
                    val deliveryDTO = DeliveryDTO(warehouseDTO, orderDTO.purchases)
                    deliveryList.add(deliveryDTO)
                    break

                } else {
                    // Update stock
                    remainingQuantity -= warehouse.availableQuantity!!
                    warehouse.availableQuantity = 0

                    // Append delivery for this warehouse
                    val warehouseDTO = WarehouseDTO(name=warehouse.warehouseName, address = warehouse.warehouseAddress)
//                    val deliveryDTO = DeliveryDTO(warehouseDTO, mapOf( purchase.product!! to remainingQuantity ))
                    val deliveryDTO = DeliveryDTO(warehouseDTO, orderDTO.purchases)
                    deliveryList.add(deliveryDTO)
                }
            }

            productsToUpdate.add(requestedProduct)
        }

        // Update documents on MongoDB
        productsToUpdate.forEach {
            warehouseRepository.save(it)
        }

        return DeliveryListDTO(orderDTO.orderID, deliveryList)
    }

    // Assume the products and the corresponding warehouse are actually present in the DB
    override fun updateStocksAfterDeliveriesCancellation(deliveryListDTO: DeliveryListDTO): Boolean {
        // Check purchases and update stocks in mongoDB
        for (deliveryDTO in deliveryListDTO.deliveryList!!) {

            val warehouseOfOrigin = deliveryDTO.warehouse

            for (purchase in deliveryDTO.delivery!!) {

                val documentToUpdate = this.getProduct(purchase.product!!) // Assume the product exists
                val stockToUpdate = documentToUpdate?.stock

                // Update the product of the interested warehouse
                for (stock in stockToUpdate!!) {
                    if (stock.warehouseName == warehouseOfOrigin?.name) // Assume name of the warehouse is the primary key
                    {
                        stock.availableQuantity = stock.availableQuantity!! + purchase.quantity!!

                        //TODO: Update also the alarm level

                        break
                    }
                }
                // Update result in MongoDB
                warehouseRepository.save(documentToUpdate)
            }
        }
        return true
    }


    override fun getProduct(product: ProductDTO): Product? {
        val result = warehouseRepository.getProductByNameAndCategory(product.name!!, product.category!!)
        try {
            return result.get()
        } catch (e: Exception) {
            println(e)
            return null
        }
    }

    override fun checkAvailability(orderDTO: OrderDTO): Boolean {

        var areProductQuantitiesAvailable: Boolean = true

        for (purchase in orderDTO.purchases!!) {
            val requestedProduct = this.getProduct(purchase.product!!) // Assume the product exists

            val arrayOfQuantities = mutableListOf<Int>()
            (requestedProduct?.stock!!).forEach {
                arrayOfQuantities.add(it.availableQuantity!!)
            }
            if (purchase.quantity!! > arrayOfQuantities.sum()) {
                areProductQuantitiesAvailable = false
                break
            }
        }
        return areProductQuantitiesAvailable
    }


}




