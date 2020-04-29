package net.corda.examples.yo;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.yo.flows.GameIssueFlow;
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

    // in our example network we only use two nodes, representing two players of a game.
    // TODO replace these with Player1 and Player2 ?
    private final StartedMockNode player1 = network.createNode();
    private final StartedMockNode player2 = network.createNode();

    public FlowTests() {
        ImmutableList.of(player1, player2).forEach(it -> {
            // TODO is there a reason we register initiated Flows but not initiating flows?
            // it.registerInitiatedFlow(GameIssueFlow.InitiatorFlow.class);
            it.registerInitiatedFlow(GameIssueFlow.ResponderFlow.class);

            // it.registerInitiatedFlow(GameUpdateFlow.InitiatorFlow.class);
            it.registerInitiatedFlow(GameUpdateFlow.ResponderFlow.class);
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
        CordaFuture<SignedTransaction> future = player1.startFlow(new GameIssueFlow.InitiatorFlow(player2.getInfo().getLegalIdentities().get(0), "chess"));
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

    // TODO find out how to get id from a previous flow

    // TODO move more setup into the @Before method directive

    // test that we can get the output of our transaction on here.
    @Test
    public void basicGameUpdateFlowTest() throws ExecutionException, InterruptedException {

        SignedTransaction tx;

        String GAMENAME = "chess";
        String WINNER = "Donkey Kong";

        CordaFuture<SignedTransaction> startedGame = player1.startFlow(new GameIssueFlow.InitiatorFlow(player2.getInfo().getLegalIdentities().get(0), GAMENAME));
        network.runNetwork();

        tx = startedGame.get();

        GameState inputState = tx.getTx().outputsOfType(GameState.class).get(0);

        assert(tx.getTx().getInputs().isEmpty()); // new games have no inputs

        // confirm a bunch of details about this game state.
        assert(inputState.getGame().equals(GAMENAME)); // confirm game name was stored successfully, for example.

        // responder flows are automatically triggered.

        // pass our previous state as an input state to the
        CordaFuture<SignedTransaction> finishedGame = player1.startFlow(new GameUpdateFlow.InitiatorFlow(inputState.getLinearId(), WINNER));

        // get our transaction linearid
        network.runNetwork();

        tx = finishedGame.get();

        GameState outputState = tx.getTx().outputsOfType(GameState.class).get(0);

        assert(!tx.getTx().getInputs().isEmpty()); // updating games have input states

        assert(outputState.getGame().equals(GAMENAME)); // confirm game name was stored successfully, for example.
        assert(outputState.getWinner().equals(WINNER)); // confirm winner was stored
        assert(outputState.getTime().equals(GameState.DEFAULT_TIME)); // confirm time is right, in this case it was not set.

    }


}
