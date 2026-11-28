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

package com.viveworks.appbase.example.reactornetty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.bootstrap.reactornetty.server.AppbaseRouteRegistry;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal Tasks REST resource registered on {@link AppbaseRouteRegistry}, backed by embedded
 * JPA + H2. The registry is exact-match only (no path variables), so item-level routes take
 * {@code id} as a query parameter -- the same convention the Appbase reactive showcase uses.
 *
 * <p>Every JPA call is blocking, so it runs on {@link Schedulers#boundedElastic()} rather than
 * the reactor-netty event-loop thread that invokes the handler.
 */
public final class TasksEndpoint {

    public static final String COLLECTION_PATH = "/tasks";
    public static final String ITEM_PATH = "/tasks/item";

    private TasksEndpoint() {
    }

    public static void register(
            final AppbaseRouteRegistry registry,
            final EntityManagerFactory entityManagerFactory,
            final ObjectMapper objectMapper) {

        registry.route("POST", COLLECTION_PATH, exchange -> exchange.readBody()
                .flatMap(bytes -> Mono.fromCallable(() -> {
                            final Map<?, ?> request = readJson(objectMapper, bytes);
                            final String title = String.valueOf(request.get("title"));
                            return withEntityManager(entityManagerFactory, em -> {
                                final Task created = new Task(title);
                                em.persist(created);
                                return created;
                            });
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(task -> {
                    exchange.status(201);
                    return exchange.writeJson(taskBody(task));
                }));

        registry.route("GET", COLLECTION_PATH, exchange -> Mono.fromCallable(() ->
                        withEntityManager(entityManagerFactory, em -> em
                                .createQuery("select t from Task t order by t.id", Task.class)
                                .getResultList()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tasks -> exchange.writeJson(tasks.stream().map(TasksEndpoint::taskBody).toList())));

        // Mono.fromCallable(() -> null) completes EMPTY, not with a null value (Reactive Streams
        // forbids null elements) -- that silently skips a downstream flatMap entirely, so a
        // nullable lookup must be wrapped in Optional before crossing the Mono boundary.
        registry.route("GET", ITEM_PATH, exchange -> Mono.fromCallable(() -> {
                    final long id = requireId(exchange);
                    return Optional.ofNullable(
                            withEntityManager(entityManagerFactory, em -> em.find(Task.class, id)));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(task -> respondWithTaskOrNotFound(exchange, task.orElse(null))));

        registry.route("PUT", ITEM_PATH, exchange -> exchange.readBody()
                .flatMap(bytes -> Mono.fromCallable(() -> {
                            final long id = requireId(exchange);
                            final Map<?, ?> request = readJson(objectMapper, bytes);
                            return Optional.ofNullable(withEntityManager(entityManagerFactory, em -> {
                                final Task existing = em.find(Task.class, id);
                                if (existing == null) {
                                    return null;
                                }
                                if (request.get("title") != null) {
                                    existing.setTitle(String.valueOf(request.get("title")));
                                }
                                if (request.get("done") != null) {
                                    existing.setDone(Boolean.parseBoolean(String.valueOf(request.get("done"))));
                                }
                                return existing;
                            }));
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(task -> respondWithTaskOrNotFound(exchange, task.orElse(null))));

        registry.route("DELETE", ITEM_PATH, exchange -> Mono.fromCallable(() -> {
                    final long id = requireId(exchange);
                    return withEntityManager(entityManagerFactory, em -> {
                        final Task existing = em.find(Task.class, id);
                        if (existing == null) {
                            return false;
                        }
                        em.remove(existing);
                        return true;
                    });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(deleted -> {
                    exchange.status(deleted ? 200 : 404);
                    return exchange.writeJson(Map.of("deleted", deleted));
                }));
    }

    private static Mono<Void> respondWithTaskOrNotFound(final AppbaseServiceExchange exchange, final Task task) {
        if (task == null) {
            exchange.status(404);
            return exchange.writeJson(Map.of("error", "not found"));
        }
        return exchange.writeJson(taskBody(task));
    }

    private static long requireId(final AppbaseServiceExchange exchange) {
        final String id = exchange.queryParam("id");
        if (id == null) {
            throw new IllegalArgumentException("Missing query parameter: id");
        }
        return Long.parseLong(id);
    }

    private static Map<?, ?> readJson(final ObjectMapper objectMapper, final byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, Map.class);
        } catch (final IOException exception) {
            throw new IllegalArgumentException("Invalid JSON body", exception);
        }
    }

    private static Map<String, Object> taskBody(final Task task) {
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", task.getId());
        body.put("title", task.getTitle());
        body.put("done", task.isDone());
        return body;
    }

    @FunctionalInterface
    private interface EntityManagerWork<T> {
        T apply(EntityManager entityManager);
    }

    private static <T> T withEntityManager(
            final EntityManagerFactory entityManagerFactory, final EntityManagerWork<T> work) {
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            final T result = work.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (final RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }
}
