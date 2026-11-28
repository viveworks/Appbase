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

package com.example.appbase.quickstart.jetty;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal Tasks REST resource, same persistence pattern as the Reactor Netty example's
 * TasksEndpoint, but registered as a real JAX-RS resource on embedded Jetty + Jersey.
 */
@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TasksResource {

    private final EntityManagerFactory entityManagerFactory;

    public TasksResource(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @POST
    public Response create(final Map<String, Object> request) {
        final String title = String.valueOf(request.get("title"));
        final Task created = withEntityManager(em -> {
            final Task task = new Task(title);
            em.persist(task);
            return task;
        });
        return Response.status(Response.Status.CREATED).entity(taskBody(created)).build();
    }

    @GET
    public List<Map<String, Object>> list() {
        final List<Task> tasks = withEntityManager(em -> em
                .createQuery("select t from Task t order by t.id", Task.class)
                .getResultList());
        return tasks.stream().map(TasksResource::taskBody).toList();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") final long id) {
        final Task task = withEntityManager(em -> em.find(Task.class, id));
        return task == null
                ? Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "not found")).build()
                : Response.ok(taskBody(task)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") final long id, final Map<String, Object> request) {
        final Task task = withEntityManager(em -> {
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
        });
        return task == null
                ? Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "not found")).build()
                : Response.ok(taskBody(task)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") final long id) {
        final boolean deleted = withEntityManager(em -> {
            final Task existing = em.find(Task.class, id);
            if (existing == null) {
                return false;
            }
            em.remove(existing);
            return true;
        });
        return deleted
                ? Response.ok(Map.of("deleted", true)).build()
                : Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "not found")).build();
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

    private <T> T withEntityManager(final EntityManagerWork<T> work) {
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
