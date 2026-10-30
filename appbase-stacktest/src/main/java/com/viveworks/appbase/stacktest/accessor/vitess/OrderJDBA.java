/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viveworks.appbase.stacktest.accessor.vitess;

import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.model.Order;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface OrderJDBA {
    @GetGeneratedKeys("order_id")
    @SqlUpdate("INSERT INTO " + StackTestConstants.STACK_TEST_ORDER_TABLE_NAME + " (customer_id, sku, price) " +
            "VALUES (:customerId, :sku, :price)")
    int createOrder(
            @Bind("customerId") Long customerId,
            @Bind("sku") String sku,
            @Bind("price") Double price)
            throws JdbiException;

    @RegisterBeanMapper(Order.class)
    @SqlQuery("SELECT * FROM " + StackTestConstants.STACK_TEST_ORDER_TABLE_NAME + " WHERE order_id = :orderId")
    Order readById(@Bind("orderId") Long orderId) throws JdbiException;

    @RegisterBeanMapper(Order.class)
    @SqlQuery("SELECT * FROM " + StackTestConstants.STACK_TEST_ORDER_TABLE_NAME + " LIMIT :limit")
    List<Order> readOrders(@Bind("limit") int limit) throws JdbiException;

    @SqlUpdate("UPDATE " + StackTestConstants.STACK_TEST_ORDER_TABLE_NAME +
            " SET sku = :sku, price = :price" +
            " WHERE customer_id = :customerId AND order_id = :orderId")
    int updateOrder(@BindBean Order order) throws JdbiException;

    @SqlUpdate("DELETE FROM " + StackTestConstants.STACK_TEST_ORDER_TABLE_NAME + " WHERE order_id = :orderId")
    boolean deleteById(@Bind("orderId") Long orderId) throws JdbiException;
}
