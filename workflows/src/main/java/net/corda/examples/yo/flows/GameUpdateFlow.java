package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.yo.contracts.GameStateContract;
import net.corda.examples.yo.states.GameState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;
import java.util.UUID;


// *********
// * Flows *
// *********

public class GameUpdateFlow {


    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private final ProgressTracker.Step RETRIEVING = new ProgressTracker.Step("Retrieving our old Game State!");
        private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the new Game State!");
        private final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verfiying the new Game State!");
        private final ProgressTracker.Step FINALIZING = new ProgressTracker.Step("Sending the new Game State!") {

            @Nullable
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        ProgressTracker progressTracker = new ProgressTracker(
                RETRIEVING,
                SIGNING,
                VERIFYING,
                FINALIZING
        );

        @Nullable
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }


        private final UniqueIdentifier stateLinearId;
        private final String winner;

        // consume our previous transaction id and add our winner.
        public InitiatorFlow(UniqueIdentifier stateLinearId, String winner) {
            this.stateLinearId = stateLinearId;
            this.winner = winner;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            Party me = getOurIdentity();

            progressTracker.setCurrentStep(RETRIEVING);

            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(stateLinearId), Vault.StateStatus.UNCONSUMED, null);
            StateAndRef inputStateAndRef = getServiceHub().getVaultService().queryBy(GameState.class, inputCriteria).getStates().get(0);

            GameState input = (GameState) inputStateAndRef.getState().getData();

            //Creating the output
            // Party counterparty = (getOurIdentity().equals(input.getProposer()))? input.getProposee() : input.getProposer();
            GameState output = new GameState(input.getPlayers(), this.winner, input.getGame());

            //collecting our required signers
            List<PublicKey> requiredSigners = new ArrayList<>();

            for (int i = 0; i < input.getPlayers().length; i++) {
                requiredSigners.add(input.getPlayers()[i].getOwningKey());
            }

            progressTracker.setCurrentStep(VERIFYING);

            // use the Modify feature of our contract.
            Command command = new Command(new GameStateContract.Commands.Modify(), requiredSigners);

            //Building the transaction
            Party notary = inputStateAndRef.getState().getNotary();
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(output, GameStateContract.ID)
                    .addCommand(command);


            progressTracker.setCurrentStep(SIGNING);
            //Signing the transaction ourselves
            SignedTransaction partStx = getServiceHub().signInitialTransaction(txBuilder);


            // TODO remove this block
            //Gathering the counterparty's signatures
            // FlowSession counterpartySession = initiateFlow(counterparty);
            //SignedTransaction fullyStx = subFlow(new CollectSignaturesFlow(partStx, ImmutableList.of(counterpartySession)));


            progressTracker.setCurrentStep(FINALIZING);
            List<FlowSession> sessionList = new ArrayList<>();

            //Collect all of the required signatures from other Players / Corda nodes using the CollectSignaturesFlow
            for (int i = 0; i < input.getPlayers().length; i++) {
                Party p = input.getPlayers()[i];

                // TODO players may contain self?

                if (p.equals(me)) {
                    continue;
                }

                FlowSession session = initiateFlow(p);

                new IdentitySyncFlow.Send(session, partStx.getTx());

                sessionList.add(session);
            }

            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(partStx, sessionList));

            //Finalising the transaction
            SignedTransaction finalTx = subFlow(new FinalityFlow(fullySignedTransaction, sessionList));
            return finalTx;
        }

    }

    ///////////////// TODO WALL

    @InitiatedBy(GameUpdateFlow.InitiatorFlow.class)
    public static class ResponderFlow extends FlowLogic<SignedTransaction> {
        private FlowSession counterpartySession;

        private SecureHash txWeJustSignedId;

        public ResponderFlow(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            class SignTxFlow extends SignTransactionFlow {

                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    txWeJustSignedId = stx.getId();
                }
            }

            // Create a sign transaction flow
            SignTxFlow signTxFlow = new SignTxFlow(counterpartySession, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flow to sign the transaction
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            return subFlow(new ReceiveFinalityFlow(counterpartySession, txWeJustSignedId));
        }

    }
}
