/*-
 * ========================LICENSE_START=================================
 * jgea-core
 * %%
 * Copyright (C) 2018 - 2025 Eric Medvet
 * %%
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
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.jgea.core.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Stopwatch {

  private final Map<String, Duration> durations;
  private final Map<String, Session> lastSessions;

  public Stopwatch() {
    durations = new ConcurrentHashMap<>();
    lastSessions = new ConcurrentHashMap<>();
  }

  @FunctionalInterface
  public interface Session {
    Duration stop();
  }

  public Session last(String name) {
    return lastSessions.get(name);
  }

  public Session start(String name) {
    Instant startInstant = Instant.now();
    Session session = () -> {
      Duration partial = Duration.between(startInstant, Instant.now());
      durations.merge(name, partial, Duration::plus);
      return partial;
    };
    lastSessions.put(name, session);
    return session;
  }

  public Optional<Duration> stop(String name) {
    Session session = last(name);
    if (session != null) {
      return Optional.of(session.stop());
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return durations.keySet()
        .stream()
        .map(
            name -> "%s=%.3fs".formatted(
                name,
                durations.get(name).toMillis() / 1_000f
            )
        )
        .collect(
            Collectors.joining("; ")
        );
  }
}
