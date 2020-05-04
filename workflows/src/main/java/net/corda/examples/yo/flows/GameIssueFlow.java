package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.UniqueIdentifier;
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
public class GameIssueFlow {

    @StartableByRPC
    @InitiatingFlow
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private final ProgressTracker.Step CREATING = new ProgressTracker.Step("Creating a new Game State!");
        private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the Game State!");
        private final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verfiying the Game State!");
        private final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Sending the Game State!") {
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

        private Party[] players;
        private String time;
        private String winner;
        private String gameName;
        private Party target;


        public InitiatorFlow(Party target, String winner, String gameName, String time) {

            this.players = ArrayUtils.add(this.players, target); // add the target to our players array
            this.target = target;
            this.winner = winner;
            this.time = time;
            this.gameName = gameName;

        }

        public InitiatorFlow(Party target, String game) {
            this(target, GameState.DEFAULT_WINNER, game, GameState.DEFAULT_TIME);
        }


        public InitiatorFlow(Party target, String game, String time) {
            this(target, GameState.DEFAULT_WINNER, game, time);
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(CREATING);

            Party me = getOurIdentity();

            // TODO make sure that the party initiating the flow is NOT the node itself.


            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Command<GameStateContract.Commands.Send> command = new Command<>(new GameStateContract.Commands.Send(), Arrays.asList(me.getOwningKey()));

            // create the game state that includes you and the other player that you're starting a game with
            this.players = ArrayUtils.add(this.players, me);

            // create state object
            GameState state = new GameState(this.players, this.winner, this.gameName, this.time);

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


    @InitiatedBy(GameIssueFlow.InitiatorFlow.class)
    public static class ResponderFlow extends FlowLogic<SignedTransaction> {
        private final FlowSession counterpartySession;

        public ResponderFlow(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }

}
