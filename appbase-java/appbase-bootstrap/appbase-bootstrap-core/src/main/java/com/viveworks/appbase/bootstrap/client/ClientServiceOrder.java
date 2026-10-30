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

package com.viveworks.appbase.bootstrap.client;

/**
 * Canonical initialization order for built-in client service factories.
 *
 * <p>Lower values initialize first. Values are spaced by 10 so custom factories can
 * slot between built-in ones without modifying this enum — pass the numeric value
 * directly to {@link AppbaseClientServiceFactory#initializationOrder()}.
 *
 * <p>Ordering reflects dependency chains:
 * <ul>
 *   <li>AWS SDK HTTP clients initialize before S3 (which uses the HTTP client)</li>
 *   <li>Firebase App initializes before Firebase Auth (which requires the app)</li>
 *   <li>JDBC initializes before JDBI and JPA (which sit on top of JDBC)</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
public enum ClientServiceOrder {

    /** AWS SDK synchronous HTTP client — foundation for S3 and other AWS service clients. */
    AWS_SDK_HTTP(10),

    /** AWS SDK asynchronous HTTP client — foundation for async AWS service clients. */
    AWS_SDK_ASYNC_HTTP(20),

    /** Amazon S3 clients — depends on {@link #AWS_SDK_HTTP} and {@link #AWS_SDK_ASYNC_HTTP}. */
    S3(30),

    /** Firebase App — singleton Firebase application context; must precede {@link #FIREBASE_AUTH}. */
    FIREBASE_APP(40),

    /** Firebase Auth client service — depends on {@link #FIREBASE_APP}. */
    FIREBASE_AUTH(50),

    /** Aerospike client service. */
    AEROSPIKE(60),

    /** Cassandra client service. */
    CASSANDRA(70),

    /** ClamAV antivirus client service. */
    CLAMAV(80),

    /** JAsync MySQL asynchronous client service. */
    JASYNC(90),

    /** Elasticsearch REST client service. */
    ELASTICSEARCH(100),

    /** Vitess JDBC client service — uses Vitess JDBC driver via {@code JdbcClientService}. */
    VITESS_JDBC(110),

    /**
     * Vitess JDBI client service — uses JDBI on top of {@link #VITESS_JDBC}.
     *
     * <p>The factory's config-based constructor requires a {@code JdbcClientService} argument
     * because {@code DefaultJdbiClientService} must receive it at construction time.
     */
    VITESS_JDBI(120),

    /** Vitess gRPC client service. */
    VITESS_GRPC(130),

    /** JPA / Hibernate client service. */
    JPA(140),

    /** Druid SQL client service. */
    DRUID(150),

    /** Kafka producer client service. */
    KAFKA_PRODUCER(160),

    /** Kafka consumer client service. */
    KAFKA_CONSUMER(170),

    /** SMTP mail client service. */
    SMTP(180),

    /** Apache Spark client service. */
    SPARK(190),

    /** Apache ZooKeeper client service. */
    ZOOKEEPER(200),

    /** Redis client service. */
    REDIS(210);

    private final int order;

    ClientServiceOrder(int order) {
        this.order = order;
    }

    /**
     * Returns the numeric initialization order value.
     *
     * @return order value; lower values initialize first
     */
    public int order() {
        return order;
    }
}
