package net.corda.examples.yo;


import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.finance.*;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;


import net.corda.examples.yo.flows.*;


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


public class StateTests {

    private MockNetwork network;
    private StartedMockNode player1;
    private StartedMockNode player2;

    private Party p1, p2;

    public String GAMENAME = "chess";
    public String WINNER = "Donkey Kong";

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.examples.yo.contracts"),
                TestCordapp.findCordapp("net.corda.examples.yo.flows")
        )));

        // in our example network we only use two nodes, representing two players of a game.
        player1 = network.createNode();
        player2 = network.createNode();

        p1 = player1.getInfo().getLegalIdentities().get(0);
        p2 = player2.getInfo().getLegalIdentities().get(0);

        ImmutableList.of(player1, player2).forEach(it -> {
            // TODO is there a reason we register initiated Flows but not initiating flows?
            // it.registerInitiatedFlow(GameIssueFlow.InitiatorFlow.class);
            it.registerInitiatedFlow(GameIssueFlow.ResponderFlow.class);

            // it.registerInitiatedFlow(GameUpdateFlow.InitiatorFlow.class);
            it.registerInitiatedFlow(GameUpdateFlow.ResponderFlow.class);
        });

        network.runNetwork();
    }


    /**
     * Task 1
     * Let's make sure we can create a GameState without this thing falling over.
     */
    @Test
    public void hasIOUAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the amount field exist?

        Party [] players = new Party[0];

        players = ArrayUtils.add(players, p1);
        players = ArrayUtils.add(players, p2);


        GameState game = new GameState(players, GAMENAME);
        // Is the amount field of the correct type?
        assertTrue(game.getGame().equals("chess"));
    }


}
