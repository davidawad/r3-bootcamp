package net.corda.examples.yo.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.yo.states.GameState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;




// NOTE unused.


public class GameModifyContract {


    // Used to identify our contract when building a transaction.
    public static final String ID = "net.corda.examples.yo.contracts.GameModifyContract";

    // Contract code.
    // @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<GameStateContract.Commands.Send> command = requireSingleCommand(tx.getCommands(), GameStateContract.Commands.Send.class);
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
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Send implements GameStateContract.Commands {}
    }





}
