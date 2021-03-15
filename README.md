# eCommerce API 
#### Advanced programming project of the Master in AI & Cloud - PoliTO Reply

eCommerce APi is an headless (just API) eCommerce web application powered by Spring boot, with a microservice architecture.

## Services
### Description
The application is composed by the following services.
#### Catalog Service
Customers interact with this service only. They can list products, get
their features and availability (availability comes from warehouses)
To each user we associate a wallet they can use to purchase products
Customer can place any order, given there is enough money in their
wallet and the products are available in the warehouses. As result they
receive and Order object with a tracking id they can use to check the
status or cancel the order until shipping is not started.
Customers also receive an email each time their order is updated
Admins can add and edit product properties
Both Customers and Admins already have an account using their email
is the username. For simplicity this is the only service handling
authentication.
####Order Service
It is the core of the system. It stores orders and their status and it is responsible
of enforcing the rules on orders.
Orders can be placed only if the customer has enough money in his/her wallet
and products are available in warehouses. Order placing and all the related
operations should be atomically isolated at a global level and be transactional,
i.e. if some service fails, for any reason, every other operation already done must
be rolled back (see later for details).
After the purchase, the order status must be updated accordingly to the
progress (picked up from the warehouse, shipping in progress, shipped,
canceled, error - you can choose any set of states useful for modeling order
handling) and admins and users must be notified via an email
In case of fail or when an order is canceled the customer must be rechargeed and
the items must be returned to the warehouse.
This service APIs can be used only by other internal services (all services are
considered trusted and they can perform any operation)
Customers and Admins can query and modify order status only through the the
CatalogService accordingly to their permissions.
#### Wallet Service
Wallets handle customer money, they have simple API: you can
query the total, the transaction list, and add a transaction.
Negative transaction are issued during purchase, positive ones
(recharges) are issued by admins only.
#### Warehouse Service
It handles the list of products stored in any warehouse.
Products can be in more than one warehouse, with different
quantities
Each warehouse has list of alarm levels for any product; when
the quantity of product is below the alarm level a notification
must be sent to the admins
The APIs allows the listing of products and their quantities,
loading and unloading items and updating alarms.
For simplicity you can use a single WarehouseService handling
more than one warehouse, but in real life there can a separate
instance for each warehouse.

### Communication
The following image reports how the communication among the microservices take places and which data are exchanged.
For each microservice, there are reported the classes that are defined in the microservice itself (<ins>underlined</ins>), and the classes that are needed from other services, defined as DTOs. 

![alt text](./images/serviceSchema.png "Workflow")

### Endpoints
#### Catalog service endpoints
|EP|Payload| Description|
|---|---|---|
|`GET /catalog/products/show`| response: ProductListDTO |Shows the catalog (the same for all users)|
|`GET /catalog/admin/products/show`| response: ProductListAdminDTO |Shows the catalog for an admin user with warehouse information|
|`POST /catalog/admin/products`| request: ProductAdminDTO |Admin adds a product specifying the warehouse|
|`PUT /catalog/admin/products/{productID}`| request: ProductAdminDTO |Admin modify information of an existing product (eventually updating the alarm level)|
|`GET /catalog/orders/{userID}`| response: PlacedOrderListDTO |Shows the orders associated with `userID`|
|`POST /catalog/orders`| request: PlacedOrderDTO |Create an order for the currently logged user with the details specified in `PlacedOrderDTO`|
|`DELETE /catalog/orders/{orderID}`| response: PlacedOrderDTO |Cancel an order `orderID` for the currently logged user (update its STATUS)|
|`GET /catalog/wallet/{userID}`| response: WalletDTO |Retrieve the wallet information (total and transaction list) for the currently logged user|
|`POST /catalog/admin/wallet/recharge/{userID}`| request: RechargeDTO |Recharge the user specified in `userID`|
#### Wallet service endpoints
|EP|Payload| Description|
|---|---|---|
|`POST /wallet/{userID}`| request: TransactionDTO response: TransactionID|Order insert a new transaction in the `userID`'s wallet|
|`GET /wallet/{userID}`| response: WalletDTO|Catalog requests`userID`'s wallet and transaction list|
|`POST /wallet/recharge/{userID}`| request: RechargeDTO|Catalog insert in the `userID`'s wallet a recharge|
#### Warehouse service endpoints
|EP|Payload| Description|
|---|---|---|
|`GET /warehouse/products/totals`| response: ProductListDTO |Catalog requests a list of products (overall quantity)|
|`GET /warehouse/products/perwarehouse`| response: ProductListAdminDTO |Catalog requests a list of products (for each warehouse) |
|`POST /warehouse/products`| request: ProductAdminDTO |Catalog insert a new product in a specific warehouse |
|`PUT /warehouse/products/{productID}`| request: ProductAdminDTO |Catalog modifies a product in a specific warehouse (eventually updating the alarm level) |
|`POST /warehouse/orders`| request: OrderDTO response: DeliveryListDTO |Order request a new order and receives a list of deliveries|
|`DELETE /warehouse/orders`| request: OrderDTO |Order deletes a previously requested order (it has been canceled by the user) |
#### Order service endpoints
|EP|Payload| Description|
|---|---|---|
|`POST /order/orders/`| request: PlacedOrderDTO |Catalog insert a new order|
|`GET /order/orders/{userID}`| response: PlacedOrderListDTO |Catalog requests the orders of `userID`|
|`DELETE /order/orders/{orderID}`| response: OrderDTO |Catalog requests to cancel the order `orderID` (updating its STATUS, if it has not been shipped yet)|

### DTOs definition
```
WalletDTO(UserDTO, total, List<Transaction>, ??time??)
ProductDTO(name, description, category, picture, currentPrice)
ProductAdminDTO(ProductDTO, WarehouseDTO, alarm level, warehouseQuantity)
ProductListDTO(Dict<ProductDTO, totalQuantity>)
ProductListAdminDTO(List<ProductAdminDTO>)

UserDTO(userID, name, surname, email, role)
PurchaseDTO(ProductDTO, quantity, sellingPrice)
TransactionDTO(UserDTO, amount, time, causale, List<PurchaseDTO>)
RechargeDTO(UserDTO, amount, time, causale,??UserDTO(who charges)??)
WarehouseDTO(name, address)

OrderDTO(OrderID, Dict<ProductDTO, quantity>)
DeliveryDTO(WarehouseDTO, Dict<ProductDTO,quantity>)
DeliveryListDTO(OrderDTO, List<DeliveryDTO>)

PlacedOrderDTO(UserDTO, List<PurchaseDTO>, deliveryAddress)
PlacedOrderListDTO(Dict<orderID,List<PurchaseDTO>>)
```

