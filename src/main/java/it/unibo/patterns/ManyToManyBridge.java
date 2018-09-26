package it.unibo.patterns;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import it.unibo.patterns.ObservableAbstractActor;

import java.util.function.Function;

public class ManyToManyBridge<T,R> extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final Function<T,R> processor;
    private Router router = new Router(new BroadcastRoutingLogic());

    public ManyToManyBridge(){ this(x -> (R)x); }

    public ManyToManyBridge(Function<T,R> processor){
        this.processor = processor;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(Observe.class, o -> o.getWho().tell(new ObservableAbstractActor.AddObserver(self()), self()))
                .match(ForwardTo.class, f -> router = router.addRoutee(f.getTarget()))
                .match(ObservableAbstractActor.Notify.class, v -> process(v))
                .build();
    }

    private void process(ObservableAbstractActor.Notify<T> notify){
        R procRes = processor.apply(notify.getValue());
        log.debug("Processing " + procRes + "; forwarding to " + router.routees());
        router.route(procRes, sender()); // keeps original sender
    }

    public static class ForwardTo {
        private final ActorRef target;

        public ForwardTo(ActorRef target){
            this.target = target;
        }

        public ActorRef getTarget() {
            return target;
        }
    }

    public static class Observe {
        private final ActorRef who;

        public Observe(ActorRef who){
            this.who = who;
        }

        public ActorRef getWho() {
            return who;
        }
    }
}

