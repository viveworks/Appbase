# Reactive showcase stacktest

The reactive showcase is a small, real HTTP example of the native Reactor Netty face. It is kept in
the ordinary `appbase-stacktest` module so a route, fixture, or streamed-response regression is caught
by the normal verification surface rather than a private shell runner.

## Run it

From the repository root, run:

```sh
mvn test -pl appbase-stacktest -am \
  -Dappbase-stacktest.skipTests=false \
  -Dtest=ReactiveShowcaseStackTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

The `-am` flag builds the required Appbase modules from this checkout. The scenario uses an
ephemeral-port Reactor Netty server and controlled reactive account/log fixtures, so it does not require
the Docker-backed infrastructure used by the broader stacktest suite.

## What it demonstrates

The test starts `GET /reactive-showcase/activity`, authorizes the request, reads an account through
`ReactiveUserAccountStore`, and streams two activity records through the reactive `LogAccessor` path.
The response is newline-delimited JSON (`application/x-ndjson`): first one allowlisted account event,
then one event for each activity record. The test asserts the status, content type, event order, account
email, and both record messages.

The stream is demand-aware. Reactor Netty writes records as the client requests them; cancelling the
client subscription cancels the log publisher, and the Lucene implementation avoids fetching later
documents. If the activity source fails after streaming begins, the response ends with one safe NDJSON
error event instead of leaking an exception or trying to replace an already-committed HTTP response.

## Native Reactor Netty versus the servlet face

This is the native reactive example: the route is registered directly on Reactor Netty and composes
reactive publishers end to end. Choose it for a new streaming or reactive HTTP surface where downstream
demand and cancellation should reach the backend.

The existing Jetty, Undertow, and Tomcat stacktest paths remain the right choice for servlet resources,
filters, compatibility work, and applications that already use the servlet API. The showcase does not
replace or bridge those paths; it gives a separate, runnable reference for the native reactive face.
