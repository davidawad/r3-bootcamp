package net.corda.examples.yo.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.examples.yo.contracts.GameStateContract;
import org.checkerframework.common.aliasing.qual.Unique;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

// *********
// * State *
// *********
@BelongsToContract(GameStateContract.class)
public class GameState implements ContractState, LinearState {
    private final Party[] players;
    private final String time;
    private final String winner;
    private final String gameName;
    private final UniqueIdentifier linearId;

    public static final String DEFAULT_TIME = "4/20 Blaze it O' clock.";
    public static final String DEFAULT_WINNER = "NONE";

    @ConstructorForDeserialization

    public GameState(Party [] players, String winner, String gameName, String time, UniqueIdentifier linearId) {
        this.players = players;
        this.winner = winner;
        this.time = time;
        this.gameName = gameName;
        this.linearId = linearId;
    }


    public GameState(Party [] players, String winner, String gameName, String time) {
        this(players, winner, gameName, time, new UniqueIdentifier());
    }

    public GameState(Party[] players, String winner, String gameName) {
        this(players, winner, gameName, DEFAULT_TIME);
    }

    public GameState(Party[] players, String gameName) {
        this(players, DEFAULT_WINNER, gameName, DEFAULT_TIME);
    }


    public Party[] getPlayers() {
        return players;
    }

    public String getWinner() {
        return winner;
    }

    public String getGame() {
        return gameName;
    }

    public String getTime() {
        return time;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }


    // @NotNull
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(players);
    }

    @Override
    public String toString() {
        return generateQuip() + winner;
    }

    public String winnerPrompt() {
        return generateQuip() + winner;
    }

    public String generateQuip() {

        // NOTE a space is added to the end of this!
        String [] quips = {
                "Heck yes, the winner was",
                "The winner was obviously",
                "No one believed the winner would be",
                "Even the blockchain didn't think this would happen, nice work",
                "You should have lost,"
        };

        int rnd = new Random().nextInt(quips.length);
        return quips[rnd] + " ";
    }

}
