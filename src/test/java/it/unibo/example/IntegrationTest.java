package it.unibo.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.PatternsCS;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import it.unibo.patterns.ManyToManyBridge;
import it.unibo.patterns.ObservableAbstractActor;
import org.junit.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class IntegrationTest {
    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    ActorRef sensor1, sensor2, bridge, aggregator, thisTest;

    @Before
    public void setupActors(){
        sensor1 = system.actorOf(Props.create(SensorActor.class, () -> new SensorActor<Double>((v) -> 5.00)));
        sensor2 = system.actorOf(Props.create(SensorActor.class, () -> new SensorActor<Double>((v) -> 10.0)));
        aggregator = system.actorOf(Props.create(MeanAggregatorActor.class));

        bridge = system.actorOf(Props.create(ManyToManyBridge.class, () -> new ManyToManyBridge<Object,Object>(d -> new AggregatorActor.SensorData(d))));
        bridge.tell(new ManyToManyBridge.Observe(sensor1), ActorRef.noSender());
        bridge.tell(new ManyToManyBridge.Observe(sensor2), ActorRef.noSender());
        bridge.tell(new ManyToManyBridge.ForwardTo(aggregator), ActorRef.noSender());
    }

    @After public void teardownActors(){
        system.stop(bridge);
        system.stop(sensor1);
        system.stop(sensor2);
        system.stop(aggregator);
    }

    @Test
    public void testSystem() {
        // Arrange
        TestKit probe = new TestKit(system);
        aggregator.tell(new ManyToManyBridge.ForwardTo(probe.getRef()), ActorRef.noSender());

        // Act
        sensor1.tell(new SensorActor.Start(), ActorRef.noSender());
        sensor2.tell(new SensorActor.Start(), ActorRef.noSender());

        // Assert
        probe.expectMsg(Duration.ofSeconds(7), new ObservableAbstractActor.Notify(7.50));

        // Act
        sensor1.tell(new SensorActor.Stop(), ActorRef.noSender());
        sensor1.tell(new SensorActor.Stop(), ActorRef.noSender());

        // Assert
        probe.expectNoMessage();
    }

    @Test
    public void testStorage() throws InterruptedException {
        // Arrange
        TestActorRef<StorageActor.BasicStorageActor> storage = TestActorRef.create(system,
              Props.create(StorageActor.BasicStorageActor.class, () -> new StorageActor.BasicStorageActor()));
        StorageActor.BasicStorageActor object = storage.underlyingActor();
        TestKit probe = new TestKit(system);
        aggregator.tell(new ManyToManyBridge.ForwardTo(storage), ActorRef.noSender());
        aggregator.tell(new ManyToManyBridge.ForwardTo(probe.getRef()), ActorRef.noSender());

        // Act
        sensor1.tell(new SensorActor.Start(), ActorRef.noSender());
        sensor2.tell(new SensorActor.Start(), ActorRef.noSender());

        probe.awaitCond(Duration.ofSeconds(6), probe::msgAvailable);
        System.out.println(object.storage);

        // Assert
        Assert.assertTrue(!object.storage.isEmpty());
    }

}
