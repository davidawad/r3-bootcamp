package net.corda.samples.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.yo.flows.GameIssueFlow;
import net.corda.examples.yo.flows.GameUpdateFlow;
import net.corda.examples.yo.states.GameState;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.corda.finance.workflows.GetBalances.getCashBalances;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/games") // The paths for HTTP requests are relative to this base path.
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public MainController(NodeRPCConnection rpc) {
        this.proxy = rpc.getProxy();
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    private String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    private String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    /*
    @GetMapping(value = "/ious",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<IOUState>> getIOUs() {
        // Filter by state type: IOU.
        return proxy.vaultQuery(IOUState.class).getStates();
    }
    @GetMapping(value = "/cash",produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<Cash.State>> getCash() {
        // Filter by state type: Cash.
        return proxy.vaultQuery(Cash.State.class).getStates();
    }


    @GetMapping(value = "/cash-balances",produces = APPLICATION_JSON_VALUE)
        return getCashBalances(proxy);
    }
     */






    @PutMapping(value =  "/create-game" , produces = TEXT_PLAIN_VALUE )
    public ResponseEntity<String> createGame(@RequestParam(value = "players") List<String> playersListParam, // NOTE always send other player as first party in list.
                                             @RequestParam(value = "gameName") String gameName,
                                             @RequestParam(value = "time") String time) throws IllegalArgumentException {

        // Get party objects for myself and the counterparty.
        Party target;
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);

        // Party lender = Optional.ofNullable(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party))).orElseThrow(() -> new IllegalArgumentException("Unknown party name."));
        List<Party> players = new ArrayList<>();

        for (String submittedPartyName : playersListParam) {
            Party p = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(submittedPartyName));

            if (p == null) {
                throw new IllegalArgumentException("Unknown party name : " + submittedPartyName);
            }

            // add our new valid party to the list
            players.add(p);
        }

        // TODO find a better way to do this lol
        target = players.get(0);

        if (time == null) {
            time = GameState.DEFAULT_TIME;
        }

        // Create a new Game State using the parameters given.
        try {
            //GameState state = new GameState(Party target, String game, String time);
            // SignedTransaction result = proxy.startTrackedFlowDynamic(GameIssueFlow.InitiatorFlow.class, state).getReturnValue().get();

            SignedTransaction result = proxy.startTrackedFlowDynamic(GameIssueFlow.InitiatorFlow.class, target, gameName, time).getReturnValue().get();

            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Game id "+ result.getId() +" committed to ledger.\n " + result.getTx().getOutput(0));
            // For the purposes of this demo app, we do not differentiate by exception type.

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    /**
     * Settles an IOU. Requires cash in the right currency to be able to settle.
     * Example request:
     * curl -X GET 'http://localhost:10007/api/iou/settle-iou?id=705dc5c5-44da-4006-a55b-e29f78955089&amount=98&currency=USD'
     */
    @GetMapping(value =  "/update-game" , produces = TEXT_PLAIN_VALUE )
    public ResponseEntity<String> updateGame(@RequestParam(value = "id") String id,
                                             @RequestParam(value = "winner") String winner) throws IllegalArgumentException {

        UniqueIdentifier linearId = new UniqueIdentifier(null, UUID.fromString(id));

        if (winner == null) {
            throw new IllegalArgumentException("No Winner Param Submitted.");
        }

        try {
            SignedTransaction result = proxy.startTrackedFlowDynamic(GameUpdateFlow.InitiatorFlow.class, linearId, winner).getReturnValue().get();

            GameState outputState = result.getTx().outputsOfType(GameState.class).get(0);

            return ResponseEntity.status(HttpStatus.CREATED).body(outputState.winnerPrompt());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }




}
