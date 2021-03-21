package it.polito.master.ap.group6.ecommerce.warehouseservice.services

import it.polito.master.ap.group6.ecommerce.common.dtos.*
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.Product
import it.polito.master.ap.group6.ecommerce.warehouseservice.model.WarehouseStock
import it.polito.master.ap.group6.ecommerce.warehouseservice.repositories.WarehouseRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

interface WarehouseService {
//    fun getAll() : MutableList<Warehouse>

    fun getProductsTotals() : ProductListDTO?
    fun getProductsPerWarehouse() : ProductListAdminDTO?
    fun insertNewProduct(productAdminDTO: ProductAdminDTO) : ResponseEntity<ProductAdminDTO>
//    fun getProduct(product: ProductDTO) : Product?
//    fun checkAvailability(orderDTO : OrderDTO) : Boolean
}

@Service
class WarehouseServiceImpl(private val warehouseRepository: WarehouseRepository) : WarehouseService {
//
////    override fun insertProduct(product: ProductAdminDTO): Product =
////        warehouseRepository.save(product.toModel())
//

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

    override fun insertNewProduct(productAdminDTO: ProductAdminDTO) : ResponseEntity<ProductAdminDTO> {

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
        warehouseRepository.save(productToStore)
        return ResponseEntity.ok(productAdminDTO)
    }

//    override fun getProduct(product: ProductDTO) : Product? {
//        val result = warehouseRepository.getProductByNameAndCategory(product.name!!, product.category!!)
//        try {
//            return result.get()
//        } catch (e: Exception) {
//            println(e)
//            return Product()
//        }
//    }

//    override fun checkAvailability(orderDTO : OrderDTO) : Boolean {
//        var areProductQuantitiesAvailable : Boolean = true
//        for (purchase in orderDTO.purchases!!) {
//            val requestedProduct = this.getProduct(purchase.product!!)
//            var totAvailbleQuantity = 0
//            for (el in requestedProduct?.stock!!) {
//                totAvailbleQuantity += el.availableQuantity!!
//            }
//            if (purchase.quantity!! > totAvailbleQuantity) {
//                areProductQuantitiesAvailable = false
//                break
//            }
//        }
//        return areProductQuantitiesAvailable
//    }


}


