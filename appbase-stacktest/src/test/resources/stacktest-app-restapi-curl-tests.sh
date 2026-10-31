echo 'Running appbase-stacktest: app REST API curl tests...'


##############################
### Product
##############################
echo 'Creating product...'
curl -X 'POST' \
  'http://localhost:2409/stacktest/restapi/v1/products' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "sku": "sku1",
  "description": "desc1",
  "price": 10,
  "traceId": "tid1"
}'


echo 'Reading products...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/products?limit=10' \
  -H 'accept: application/json'


echo 'Updating product...'
curl -X 'PUT' \
  'http://localhost:2409/stacktest/restapi/v1/products' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "product": {
      "sku": "sku1",
      "description": "updated desc1",
      "price": 12
    }
  }'


echo 'Reading product by SKU...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/products/sku1' \
  -H 'accept: application/json'


echo 'Searching product by SKU...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/products/search/sku1' \
  -H 'accept: application/json'


echo 'Searching product...'
curl -X 'POST' \
  'http://localhost:2409/stacktest/restapi/v1/search' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "product": {
    "sku": "sku1"
  }
}'




##############################
### Customer
##############################
echo 'Creating customer...'
curl -X 'POST' \
  'http://localhost:2409/stacktest/restapi/v1/customers' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "cust1@dom.com"
  }'


echo 'Reading customers...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/customers?limit=10' \
  -H 'accept: application/json'


echo 'REPLACE IDS - Updating customers...'
#curl -X 'PUT' \
#  'http://localhost:2409/stacktest/restapi/v1/customers' \
#  -H 'accept: application/json' \
#  -H 'Content-Type: application/json' \
#  -d '{
#  "customer": {
#    "customerId": 1004,
#    "email": "updated_cust1@dom1.com"
#  }
#}'


echo 'REPLACE IDs - Reading customer by ID...'
#curl -X 'GET' \
#  'http://localhost:2409/stacktest/restapi/v1/customers/1004' \
#  -H 'accept: application/json'


echo 'REPLACE IDs - Searching customer by email...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/customers/search/updated_cust1' \
  -H 'accept: application/json'


echo 'REPLACE IDs - Searching customer...'
curl -X 'POST' \
  'http://localhost:2409/stacktest/restapi/v1/search' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "customer": {
    "email": "updated_cust1"
  }
}'


echo 'Reading customer statistics ...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/customer_statistics/1005' \
  -H 'accept: application/json'

echo 'TODO - Uploading customer photo ...'
echo 'TODO - Downloading customer photo ...'


echo 'Reading customers...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/customers?limit=10' \
  -H 'accept: application/json'



##############################
### Order
##############################
echo 'REPLACE IDs - Creating order...'
curl -X 'POST' \
  'http://localhost:2409/stacktest/restapi/v1/orders' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "customerId": 1004,
  "sku": "sku3",
  "price": 16
}'


echo 'Reading orders...'
curl -X 'GET' \
  'http://localhost:2409/stacktest/restapi/v1/orders?limit=10' \
  -H 'accept: application/json'


echo 'REPLACE IDs - Reading customer orders ...'
#curl -X 'GET' \
#  'http://localhost:2409/stacktest/restapi/v1/customers/1004/orders' \
#  -H 'accept: application/json'


echo 'REPLACE IDs - Updating order...'
#curl -X 'PUT' \
#  'http://localhost:2409/stacktest/restapi/v1/orders' \
#  -H 'accept: application/json' \
#  -H 'Content-Type: application/json' \
#  -d '{
#    "order": {
#      "orderId": 1000,
#      "customerId": 1004,
#      "sku": "sku2",
#      "price": 21
#    }
#  }'


echo 'REPLACE IDs - Reading order by ID...'
#curl -X 'GET' \
#  'http://localhost:2409/stacktest/restapi/v1/orders/1000' \
#  -H 'accept: application/json'



##############################
### Delete all
##############################
echo 'REPLACE IDs - Deleting order by ID...'
#curl -X 'DELETE' \
#  'http://localhost:2409/stacktest/restapi/v1/orders/1002' \
#  -H 'accept: application/json'

echo 'REPLACE IDs - Deleting product by SKU...'
#curl -X 'DELETE' \
#  'http://localhost:2409/stacktest/restapi/v1/products/sku1' \
#  -H 'accept: application/json'


echo 'REPLACE IDs - Deleting customer by ID...'
#curl -X 'DELETE' \
#  'http://localhost:2409/stacktest/restapi/v1/customers/1005' \
#  -H 'accept: application/json'
