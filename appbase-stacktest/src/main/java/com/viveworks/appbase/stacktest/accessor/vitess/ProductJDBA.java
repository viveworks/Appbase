/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
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
import com.viveworks.appbase.stacktest.data.model.Product;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ProductJDBA {
    @SqlUpdate("INSERT INTO " + StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME + " (sku, description, price) " +
            "VALUES (:sku, :description, :price)")
    int createProduct(@BindBean Product product) throws JdbiException;

    @RegisterBeanMapper(Product.class)
    @SqlQuery("SELECT * FROM " + StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME + " WHERE sku=:sku")
    Product readById(@Bind("sku") String sku) throws JdbiException;

    @RegisterBeanMapper(Product.class)
    @SqlQuery("SELECT * FROM " + StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME + " LIMIT :limit")
    List<Product> readProducts(@Bind("limit") int limit) throws JdbiException;

    @SqlUpdate("UPDATE " + StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME +
            " SET description = :description WHERE sku = :sku")
    int updateDescription(
            @Bind("sku") String sku,
            @Bind("description") String description) throws JdbiException;

    @SqlUpdate("UPDATE " + StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME +
            " SET price = :price WHERE sku = :sku")
    int updatePrice(
            @Bind("sku") String sku,
            @Bind("price") Double price) throws JdbiException;

    @SqlUpdate("UPDATE " + StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME +
            " SET description = :description, price = :price" +
            " WHERE sku = :sku")
    int updateProduct(@BindBean Product product) throws JdbiException;

    @SqlUpdate("DELETE FROM " + StackTestConstants.STACK_TEST_PRODUCT_TABLE_NAME + " WHERE sku = :sku")
    boolean deleteById(@Bind("sku") String sku) throws JdbiException;
}
