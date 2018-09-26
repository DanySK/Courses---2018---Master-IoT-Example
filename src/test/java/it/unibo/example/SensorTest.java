package it.unibo.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class SensorTest {

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

    @Test
    public void testSensorSendsDataOncePerSecond() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new TestKit(system) {{
            final Props props = Props.create(SensorActor.class, () -> new SensorActor<Double>((v) -> v.orElse(0.0)+1.5, Duration.ofSeconds(1L)));
            final ActorRef subject = system.actorOf(props);

            subject.tell(new SensorActor.AddObserver(getRef()), getRef());

            expectNoMsg();

            subject.tell(new SensorActor.Start(), ActorRef.noSender());

            within(Duration.ofMillis(5500), () -> {
               Assert.assertArrayEquals(Arrays.asList(1.5, 3.0, 4.5, 6.0, 7.5).toArray(), receiveN(5).stream().map(obj -> ((SensorActor.Notify<Double>)obj).getValue()).toArray());
               return null;
            });
        }};
    }

}
