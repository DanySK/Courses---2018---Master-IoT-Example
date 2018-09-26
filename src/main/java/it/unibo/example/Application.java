package it.unibo.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;
import it.unibo.patterns.ManyToManyBridge;
import it.unibo.patterns.ObserverActor;

public class Application {

    public static void main(String[] args){

        ActorSystem as = ActorSystem.create("iotSystem", ConfigFactory.parseString("akka.loglevel = \"DEBUG\""));
        ActorRef sensorActor1 = as.actorOf(Props.create(SensorActor.class, () -> new SensorActor<Double>((v) -> 1.5)), "sensor1");
        ActorRef sensorActor2 = as.actorOf(Props.create(SensorActor.class, () -> new SensorActor<Double>((v) -> v.map(x -> x>2 ? 0.0 : x+0.2).orElse(0.0))), "sensor2");
        ActorRef analyticsActor = as.actorOf(Props.create(AnalyticsActor.class), "analytics");
        ActorRef storageActor = as.actorOf(Props.create(StorageActor.class), "storage");
        ActorRef aggregatorActor = as.actorOf(Props.create(MeanAggregatorActor.class), "aggregator");

        aggregatorActor.tell(new ManyToManyBridge.ForwardTo(analyticsActor), ActorRef.noSender());
        aggregatorActor.tell(new ManyToManyBridge.ForwardTo(storageActor), ActorRef.noSender());

        ActorRef bridge = as.actorOf(Props.create(ManyToManyBridge.class, () -> new ManyToManyBridge<Object,Object>(d -> new AggregatorActor.SensorData(d))), "bridge");
        bridge.tell(new ManyToManyBridge.Observe(sensorActor1), ActorRef.noSender());
        bridge.tell(new ManyToManyBridge.Observe(sensorActor2), ActorRef.noSender());
        bridge.tell(new ManyToManyBridge.ForwardTo(aggregatorActor), ActorRef.noSender());

        //ActorRef observerActor = as.actorOf(Props.create(ObserverActor.class, () -> new ObserverActor<Double>(v -> System.out.println("Observing: " + v.toString()), sensorActor1)), "observer");
        sensorActor1.tell(new SensorActor.Start(), ActorRef.noSender());
        sensorActor2.tell(new SensorActor.Start(), ActorRef.noSender());
    }
}
