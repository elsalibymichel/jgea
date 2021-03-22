package it.units.malelab.jgea.core.listener;


import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@FunctionalInterface
public interface Listener<E> {
  void listen(E e);

  default void done() {
  }

  default Listener<E> deferred(ExecutorService executorService) {
    Listener<E> thisListener = this;
    final Logger L = Logger.getLogger(Listener.class.getName());
    return new Listener<>() {
      @Override
      public void listen(E e) {
        executorService.submit(() -> {
          try {
            thisListener.listen(e);
          } catch (RuntimeException ex) {
            L.warning(String.format(
                "Listener %s cannot list event: %s",
                thisListener.getClass().getSimpleName(),
                ex
            ));
          }
        });
      }

      @Override
      public void done() {
        executorService.submit(() -> {
          try {
            thisListener.done();
          } catch (RuntimeException ex) {
            L.warning(String.format(
                "Listener %s cannot list event: %s",
                thisListener.getClass().getSimpleName(),
                ex
            ));
          }
        });
      }
    };
  }

  default Listener<E> onLast() {
    Listener<E> thisListener = this;
    return new Listener<>() {
      E lastE;

      @Override
      public void listen(E e) {
        lastE = e;
      }

      @Override
      public void done() {
        thisListener.listen(lastE);
        thisListener.done();
      }
    };
  }

  static <E> Listener<E> deaf() {
    return e -> {
    };
  }

  static <E, F> Listener<F> forEach(Function<F, Collection<E>> splitter, Listener<E> listener) {
    return new Listener<>() {
      @Override
      public void listen(F f) {
        splitter.apply(f).forEach(listener::listen);
      }

      @Override
      public void done() {
        listener.done();
      }
    };
  }

  static <E> Listener<E> all(List<Listener<? super E>> listeners) {
    return new Listener<>() {
      @Override
      public void listen(E e) {
        listeners.forEach(l -> l.listen(e));
      }

      @Override
      public void done() {
        listeners.forEach(Listener::done);
      }
    };
  }

  interface Factory<E> {
    Listener<E> build();

    default void shutdown() {
    }

    default Factory<E> and(Factory<E> other) {
      return Factory.all(List.of(this, other));
    }

    default Factory<E> onLast() {
      Factory<E> thisFactory = this;
      return new Factory<>() {
        @Override
        public Listener<E> build() {
          return thisFactory.build().onLast();
        }

        @Override
        public void shutdown() {
          thisFactory.shutdown();
        }
      };
    }

    static <E> Factory<E> all(List<? extends Listener.Factory<? super E>> factories) {
      return new Factory<>() {
        @Override
        public Listener<E> build() {
          return Listener.all(factories.stream().map(Factory::build).collect(Collectors.toList()));
        }

        @Override
        public void shutdown() {
          factories.forEach(Factory::shutdown);
        }
      };
    }

    static <F, E> Factory<F> forEach(Function<F, Collection<E>> splitter, Listener.Factory<E> factory) {
      return new Factory<>() {
        @Override
        public Listener<F> build() {
          return Listener.forEach(splitter, factory.build());
        }

        @Override
        public void shutdown() {
          factory.shutdown();
        }
      };
    }

    static <E> Factory<E> deaf() {
      return Listener::deaf;
    }

  }

}
