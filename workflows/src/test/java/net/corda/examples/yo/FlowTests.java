package net.corda.examples.yo;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.yo.flows.GameIssueFlow;
import net.corda.examples.yo.flows.GameIssueFlowResponder;
import net.corda.examples.yo.flows.GameUpdateFlow;
import net.corda.examples.yo.states.GameState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
            TestCordapp.findCordapp("net.corda.examples.yo.contracts"),
            TestCordapp.findCordapp("net.corda.examples.yo.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();

    public FlowTests() {
        ImmutableList.of(a, b).forEach(it -> {
            it.registerInitiatedFlow(GameIssueFlowResponder.class);
        });
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    //basic test to ensure we can create a transaction
    //This test will check if the input list is empty
    @Test
    public void basicGameIssueFlowTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new GameIssueFlow(b.getInfo().getLegalIdentities().get(0), "chess"));
        network.runNetwork();
        SignedTransaction ptx = future.get();
        assert(ptx.getTx().getInputs().isEmpty());
    }


    // checkers is a bad game. Lets make sure no one can play it.
    // TODO how to test for a contract verification failure
    /*
    @Test
    public void noCheckersTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new GameIssueFlow(b.getInfo().getLegalIdentities().get(0), "checkers"));
        network.runNetwork();
        SignedTransaction ptx = future.get();
        // GameState state = ptx.outputsOfType(GameState.class).get(0);
        // assert();
    }
    */

    @Test
    public void basicGameUpdateFlowTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new GameUpdateFlow(, "chess"));
        network.runNetwork();
        SignedTransaction ptx = future.get();
        assert(ptx.getTx().getInputs().isEmpty());
    }

}
