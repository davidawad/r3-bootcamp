package net.corda.examples.yo.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.examples.yo.contracts.GameStateContract;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

// *********
// * State *
// *********
@BelongsToContract(GameStateContract.class)
public class GameState implements ContractState {
    private final Party[] players;
    private final String time;
    private final String winner;
    private final String gameName;

    @ConstructorForDeserialization
    public GameState(Party [] players, String winner, String gameName, String time) {
        this.players = players;
        this.winner = winner;
        this.time = time;
        this.gameName = gameName;
    }

    public GameState(Party[] players, String winner, String gameName) {
        this.players = players;
        this.winner = winner;
        this.time = "4/20 blaze it."; // default timestamp if time not provided
        this.gameName = gameName;
    }


    public GameState(Party[] players, String gameName) {
        this.players = players;
        this.winner = "NONE";
        this.time = "4/20 blaze it."; // default timestamp if time not provided
        this.gameName = gameName;
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


    // @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(players);
    }

    @Override
    public String toString() {
        return generateQuip() + winner;
    }


    public static int getRandom(int[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    public String generateQuip() {

        String [] quips = {
                "Heck yes, the winner was ",
                "The winner was obviously ",
                "No one believed the winner would be ",
                "Even the blockchain didn't think this would happen, nice work ",
                "You should have lost, "
        };

        int rnd = new Random().nextInt(quips.length);
        return quips[rnd];
    }



}
