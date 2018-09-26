package it.unibo.patterns;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import scala.PartialFunction;

import java.util.HashSet;
import java.util.Set;

public abstract class ObservableAbstractActor<T> extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Router router = new Router(new BroadcastRoutingLogic());

    public static class AddObserver{
        private final ActorRef ref;

        public AddObserver(ActorRef ref){
            this.ref = ref;
        }

        public ActorRef getRef() {
            return ref;
        }
    }

    public static class RemoveObserver{
        private final ActorRef ref;

        public RemoveObserver(ActorRef ref){
            this.ref = ref;
        }

        public ActorRef getRef() {
            return ref;
        }
    }

    public static class Notify<T>{
        private final T value;

        public Notify(T value){
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Notify ? ((Notify) obj).value.equals(value) : false;
        }

        @Override
        public String toString() {
            return "Notify(" + value + ")";
        }
    }

    protected Receive ObservableBehaviour(){
        return ReceiveBuilder.create()
                .match(AddObserver.class, obs -> {
                    log.debug("Adding observer " + obs.getRef().path().name());
                    router = router.addRoutee(obs.getRef());
                })
                .match(RemoveObserver.class, obs -> {
                    log.debug("Removing observer " + obs.getRef().path().name());
                    router = router.removeRoutee(obs.getRef());
                })
                .build();

    }

    protected void notificationBroadcast(T value){
        log.debug("Notifying " + value + " to " +router.routees());
        router.route(new Notify(value), self());
    }
}
