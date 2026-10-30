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
import com.viveworks.appbase.stacktest.data.model.Customer;
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
public interface CustomerJDBA {
    @GetGeneratedKeys("customer_id")
    @SqlUpdate("INSERT INTO " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME +
            " (email, photoId) VALUES (:email, :photoId)")
    int createCustomer(@Bind("email") String email, @Bind("photoId") String photoId) throws JdbiException;

    @GetGeneratedKeys("customer_id")
    @SqlUpdate("INSERT INTO " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME +
            " (email) VALUES (:email)")
    int createCustomer(@Bind("email") String email) throws JdbiException;

    @RegisterBeanMapper(Customer.class)
    @SqlQuery("SELECT * FROM " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME +
            " WHERE customer_id=:customerId")
    Customer readById(@Bind("customerId") Long customerId) throws JdbiException;

    @RegisterBeanMapper(Order.class)
    @SqlQuery("SELECT o.order_id, o.customer_id, o.sku, o.price" +
            " FROM " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME + " c" +
            " JOIN " + StackTestConstants.STACK_TEST_ORDER_TABLE_NAME + " o ON" +
            " c.customer_id = o.customer_id WHERE c.customer_id=:customerId")
    List<Order> readCustomerOrders(@Bind("customerId") Long customerId);

    @RegisterBeanMapper(Customer.class)
    @SqlQuery("SELECT * FROM " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME + " LIMIT :limit")
    List<Customer> readCustomers(@Bind("limit") int limit) throws JdbiException;

    @SqlUpdate("UPDATE " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME +
            " SET email = :email WHERE customer_id = :customerId")
    int updateEmail(@Bind("email") String email, @Bind("customerId") Long customerId) throws JdbiException;

    @SqlUpdate("UPDATE " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME +
            " SET photoId = :photoId WHERE customer_id = :customerId")
    int updatePhotoId(@Bind("photoId") String photoId, @Bind("customerId") Long customerId) throws JdbiException;

    @SqlUpdate("UPDATE " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME +
            " SET email = :email, photoId = :photoId WHERE customer_id = :customerId")
    int updateCustomer(@BindBean Customer customer) throws JdbiException;

    @SqlUpdate("DELETE FROM " + StackTestConstants.STACK_TEST_CUSTOMER_TABLE_NAME +
            " WHERE customer_id = :customerId")
    boolean deleteById(@Bind("customerId") Long customerId) throws JdbiException;
}
