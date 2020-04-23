package net.corda.examples.yo.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.yo.states.GameState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.*;

// ************
// * Contract *
// ************
// Contract and state.
public class GameStateContract implements Contract {

    // Used to identify our contract when building a transaction.
    public static final String ID = "net.corda.examples.yo.contracts.GameIssueContract";

    // Contract code.
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {


        // CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.Send.class);

        final CommandWithParties command = tx.getCommands().get(0);

        if(command.getValue() instanceof Commands.Send) {

            requireThat(req -> {

                GameState state = tx.outputsOfType(GameState.class).get(0);

                req.using("No playing games alone!", state.getPlayers().length > 1);
                req.using("Checkers is not a real game", !state.getGame().toLowerCase().equals("checkers"));

                // a valid game has more than one player, a timestamp and a winner.
                //req.using("A valid game has at least two players", state.getPlayers().length >= 2);

                //TODO implement more tests than this lol.

                // req.using("There can be no inputs when Yo'ing other parties", tx.getInputs().isEmpty());
                // req.using("There must be one output: The Yo!", tx.getOutputs().size() == 1);

                // YoState yo = tx.outputsOfType(YoState.class).get(0);
                // req.using("No sending Yo's to yourself!", !yo.getTarget().equals(yo.getOrigin()));
                // req.using("The Yo! must be signed by the sender.", command.getSigners().contains(yo.getOrigin().getOwningKey()));
                return null;
            });

        } else if (command.getValue() instanceof Commands.Modify) {

            requireThat(req -> {
                GameState input = tx.inputsOfType(GameState.class).get(0);
                GameState output = tx.outputsOfType(GameState.class).get(0);

                // There is always a winner.
                req.using("Winner can't be NONE!", !output.getWinner().toLowerCase().equals("NONE"));

                return null;
            });

        } else {
            throw new IllegalArgumentException("Command of incorrect type");
        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Send implements Commands {};
        class Modify implements Commands{};
    }
}
