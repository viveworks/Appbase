echo 'Running appbase-stacktest: app servlets curl tests...'


##############################
### Product
##############################
echo 'Creating product...'
curl -X 'POST' \
  'http://localhost:2409/product' \
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
  'http://localhost:2409/product?limit=10' \
  -H 'accept: application/json'


echo 'Updating product...'
curl -X 'PUT' \
  'http://localhost:2409/product' \
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
  'http://localhost:2409/product?id=sku1' \
  -H 'accept: application/json'


echo 'Searching product by SKU...'
curl -X 'GET' \
  'http://localhost:2409/product_search?id=sku1' \
  -H 'accept: application/json'


echo 'Searching product...'
curl -X 'POST' \
  'http://localhost:2409/search' \
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
  'http://localhost:2409/customer' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "cust1@dom.com"
  }'


echo 'Reading customers...'
curl -X 'GET' \
  'http://localhost:2409/customer?limit=10' \
  -H 'accept: application/json'


echo 'REPLACE IDs - Updating customers...'
#curl -X 'PUT' \
#  'http://localhost:2409/customer' \
#  -H 'accept: application/json' \
#  -H 'Content-Type: application/json' \
#  -d '{
#  "customer": {
#    "customerId": 1104,
#    "email": "updated_cust1@dom1.com"
#  }
#}'


echo 'REPLACE IDs - Reading customer by ID...'
#curl -X 'GET' \
#  'http://localhost:2409/customer?id=1104' \
#  -H 'accept: application/json'


echo 'REPLACE IDs - Searching customer by email...'
curl -X 'GET' \
  'http://localhost:2409/customer_search?email=updated_cust1' \
  -H 'accept: application/json'


echo 'Searching customer...'
curl -X 'POST' \
  'http://localhost:2409/search' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "customer": {
      "email": "updated_cust1"
    }
}'


echo 'Reading customer statistics ...'
curl -X 'GET' \
  'http://localhost:2409/customer_orders?id=1104' \
  -H 'accept: application/json'

echo 'TODO - Uploading customer photo ...'
echo 'TODO - Downloading customer photo ...'



##############################
### Order
##############################
echo 'REPLACE IDs - Creating order...'
#curl -X 'POST' \
#  'http://localhost:2409/order' \
#  -H 'accept: application/json' \
#  -H 'Content-Type: application/json' \
#  -d '{
#  "customerId": 1104,
#  "sku": "sku3",
#  "price": 16
#}'


echo 'Reading orders...'
curl -X 'GET' \
  'http://localhost:2409/order?limit=10' \
  -H 'accept: application/json'


echo 'REPLACE IDs - Reading customer orders ...'
#curl -X 'GET' \
#  'http://localhost:2409/customer_orders?id=1104' \
#  -H 'accept: application/json'


echo 'REPLACE IDs - Updating order...'
#curl -X 'PUT' \
#  'http://localhost:2409/order' \
#  -H 'accept: application/json' \
#  -H 'Content-Type: application/json' \
#  -d '{
#    "order": {
#      "orderId": 1000,
#      "customerId": 1104,
#      "sku": "sku2",
#      "price": 21
#    }
#  }'


echo 'REPLACE IDs - Reading order by ID...'
#curl -X 'GET' \
#  'http://localhost:2409/order?id=1000' \
#  -H 'accept: application/json'



##############################
### Delete all
##############################

