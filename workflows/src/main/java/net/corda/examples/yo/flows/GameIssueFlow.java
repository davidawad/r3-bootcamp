package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
//import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.yo.contracts.GameStateContract;
import net.corda.examples.yo.states.GameState;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
public class GameIssueFlow extends FlowLogic<SignedTransaction> {

    private static final ProgressTracker.Step CREATING = new ProgressTracker.Step("Creating a new Game State!");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the Game State!");
    private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verfiying the Game State!");
    private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Sending the Game State!") {
        @Nullable
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    ProgressTracker progressTracker = new ProgressTracker(
            CREATING,
            SIGNING,
            VERIFYING,
            FINALISING
    );

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    // private final List<Party> players = new ArrayList<Party>();
    private Party[] players;
    private String time;
    private String winner;
    private String gameName;
    private Party target;


    public GameIssueFlow(Party target, String game) {

        System.out.println("Received target: " + target + ", gameName: " + game);
        this.players = ArrayUtils.add(players, target); // add the target to our players array
        this.gameName = game;
        this.winner = "NONE";
        this.target = target;

        // TODO if time is specified, add it.
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(CREATING);

        Party me = getOurIdentity();
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Command<GameStateContract.Commands.Send> command = new Command<>(new GameStateContract.Commands.Send(), Arrays.asList(me.getOwningKey()));

        // create the game state that includes you and the other player that you're starting a game with
        this.players = ArrayUtils.add(this.players, me);

        // create state object
        GameState state = new GameState(this.players, this.gameName);

        StateAndContract stateAndContract = new StateAndContract(state, GameStateContract.ID);
        TransactionBuilder utx = new TransactionBuilder(notary).withItems(stateAndContract, command);

        progressTracker.setCurrentStep(VERIFYING);
        utx.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING);
        SignedTransaction stx = getServiceHub().signInitialTransaction(utx);

        // send this created game state to the other party for finalizing
        progressTracker.setCurrentStep(FINALISING);
        FlowSession targetSession = initiateFlow(this.target);
        return subFlow(new FinalityFlow(stx, Arrays.asList(targetSession), Objects.requireNonNull(FINALISING.childProgressTracker())));

        // TODO add an observer to add the ability for there to be a leaderboard of some kind.
    }
}
