package com.cfaaato;

import com.connectors.*;
import com.data.*;
import com.port.*;
import com.services.*;
import com.utils.*;
import fr.sorbonne_u.components.*;
import fr.sorbonne_u.components.annotations.*;
import fr.sorbonne_u.components.exceptions.*;
import fr.sorbonne_u.components.helpers.*;

import java.util.*;

/**
 * The type Participant.
 */
@RequiredInterfaces(required={RegistrationCI.class, CommunicationCI.class})
@OfferedInterfaces(offered = {CommunicationCI.class})
public class Participant extends AbstractComponent {

    /**
     * The Prop.
     */
//Ports declaration
    protected ParticipantRegistrationOutboundPort prop;
    /**
     * The Pcop.
     */
    protected ParticipantCommunicationOutboundPort pcop;
    /**
     * The Pcip.
     */
    protected ParticipantCommunicationInboundPort pcip;
    /**
     * The Prtip.
     */
    protected ParticipantRoutageInboundPort prtip;
    /**
     * The Prtop.
     */
    protected ParticipantRoutageOutboundPort prtop;

    /**
     * The Routage connector.
     */
//Cnnectors informations
    RoutageConnector routageConnector;

    //Connections Informations of this element
    private ConnectionInfo myInformations;
    private Set<ConnectionInfo> neighbors;
    //
    private HashMap<P2PAddressI, String> comAddressPortTable = new HashMap<>();
    private HashMap<P2PAddressI, String> routingAddressPortTable = new HashMap<>();
    private RoutingTable myRoutingTable = new RoutingTable();
    private Position pos;
    private Logger myLogger;

    /**
     * Description: Instantiates a new Participant.
     * @param nbThreads of type int - the number of threads
     * @param nbSchedulableThreads of type int - the number schedulable threads
     * @param pos of type Position - the position of the particiant
     * @throws Exception the exception
     */
    protected Participant(int nbThreads, int nbSchedulableThreads, Position pos) throws Exception {
        super(nbThreads, nbSchedulableThreads);
        this.neighbors = new HashSet<>();
        this.pos = pos;
        //creation des ports de communication
        this.pcip = new ParticipantCommunicationInboundPort(UUID.randomUUID().toString(),this);
        this.pcop = new ParticipantCommunicationOutboundPort(this);
        //publication des ports de communication
        this.pcip.publishPort();
        this.pcop.publishPort();
        //creation des ports de routage
        this.prtip = new ParticipantRoutageInboundPort(UUID.randomUUID().toString(),this);
        this.prtop = new ParticipantRoutageOutboundPort(this);
        //publication des ports de routage
        this.prtip.publishPort();
        this.prtop.publishPort();

        myLogger = new Logger(Integer.toString(this.hashCode()));
        this.setLogger(myLogger);
    }

    /**
     * Description: Registrate on network, notice all the reachable participant of his existence by computing
     * a table of neighbors
     * @throws Exception the exception
     */
    public void registrateOnNetwork() throws Exception {
        this.prop = new ParticipantRegistrationOutboundPort(this);   //creation du port
        this.prop.publishPort();     //publication du port
        this.doPortConnection(this.prop.getPortURI(), ConstantsValues.URI_REGISTRATION_SIMULATOR_PORT, RegistrationConnector.class.getCanonicalName());

        P2PAddress P2PAddress_init = new P2PAddress();
        this.myInformations = new ConnectionInfo(P2PAddress_init,
                this.pcip.getPortURI(),
                this.pos,
                ConstantsValues.RANGE_MAX_A,
                this.prtip.getPortURI());
        //Iniitialisation de la liste de voisins directs
        this.neighbors = this.prop.registerInternal(
                this.myInformations.getAddress(),
                this.myInformations.getCommunicationInboundPortURI(),
                this.myInformations.getInitialPosition(),
                this.myInformations.getInitialRange(),
                this.myInformations.getRoutingInboundPortURI()
        );

        //Pour chaque voisin reçu du simulateur, on rempli notre table de port et on initialise notre table de routage
        for (ConnectionInfo coi : this.neighbors){
            this.comAddressPortTable.put(coi.getAddress(), coi.getCommunicationInboundPortURI());
        }
    }

    /**
     * Description: add a ne member to the network
     * @throws Exception the exception
     */
    public void newOnNetwork() throws Exception {
        this.logMessage(this.myInformations.getAddress().toString());
        this.logMessage(" nb neighbors : "+ this.neighbors.size());

        int Tour =1;
        for (ConnectionInfo coi : this.neighbors){
            this.logMessage(" Tour :"+Tour+ " neighbor :"+ coi.getAddress());

            this.doPortConnection(
                    this.pcop.getPortURI(),
                    coi.getCommunicationInboundPortURI(),
                    CommunicationConnector.class.getCanonicalName()
            );

            this.pcop.connect(
                    this.myInformations.getAddress(),
                    this.pcip.getPortURI(),
                    this.prtip.getPortURI());


            // this.comAddressPortTable.put(
            //         coi.getAddress(),
            //         coi.getCommunicationInboundPortURI()
            // );


            // this.routingAddressPortTable.put(
            //         coi.getAddress(),
            //         coi.getRoutingInboundPortURI()
            // );

            this.logMessage(" fin Tour :"+Tour+ " neighbor :"+ coi.getAddress());
            Tour++;
        }
    }

    /**
     * Description: Connect a participant to another one and fill up the ports (routage and communication) table
     * @param address of type P2PAddressI - the address of the calling participant
     * @param communicationInboundPortURI of type String - the URI of the communication inbound port
     * @param routingInboundPortURI of type String - the URI of the routing inbound port
     * @throws Exception the exception
     */
    public void connect(P2PAddressI address, String communicationInboundPortURI, String routingInboundPortURI) throws Exception {
        if (!this.comAddressPortTable.containsKey(address)){
            this.logMessage("connect called from :"+ address);
            this.comAddressPortTable.put(address, communicationInboundPortURI);


            // this.doPortConnection(
            //         this.pcop.getPortURI(),
            //         communicationInboundPortURI,
            //         CommunicationConnector.class.getCanonicalName()
            // );
        }

        if (!this.routingAddressPortTable.containsKey(address)){
            this.routingAddressPortTable.put(address, routingInboundPortURI);
        }
    }

    /**
     * Description: Update routing network.
     * @param neighbour of type P2Pb - the address of neighbor on the network
     * @param routes    of type RouteInfo - a list of routes to a destination
     * @throws Exception the exception
     */
    public void updateRouting(P2PAddressI neighbour, Set<RouteInfo> routes) throws Exception{
        myLogger.logMessage("ma routingTablePort: " + this.routingAddressPortTable);
    }

    /**
     * Description: Update access point and the number of hops
     * @param neighbour    of type P2PAddressI - the neighbour
     * @param numberOfHops of type int - the number of hops o update
     * @throws Exception the exception
     */
    public void updateAccessPoint(P2PAddressI neighbour, int numberOfHops) throws Exception{

    }

    /**
     * Description: Flood the network to reach the receiver.
     * @param m of type Message - the message to send
     * @throws Exception the exception
     */
    public void floodMessageTransit(Message m) throws Exception {
        //if the message hit the receiver, it's done
        if (m.getAddress().equals(this.myInformations.getAddress())){
            myLogger.logMessage("Message arrived");
        }
        else {
            m.decrementHops();
            if (m.stillAlive()){
                //Iterating through HashMap
                for (Map.Entry<P2PAddressI, String> item : this.comAddressPortTable.entrySet()) {
                    if(pcop.connected()){
                        pcop.doDisconnection();
                    }
                    this.doPortConnection(
                            this.pcop.getPortURI(),
                            item.getValue(),
                            CommunicationConnector.class.getCanonicalName()
                    );


                    this.pcop.routeMessage(m);
                }
                pcop.doDisconnection();
                this.logMessage("Msg died");
            }
        }
    }

    /**
     * Description: Route the message by choosing the best way to the receiver (lightest coast)
     * @param m of Message - the message to send
     * @throws Exception the exception
     */
    /*this method should parse all address contained inside the table attribut
     */
    public void routeByTable(Message m) throws Exception {
        if(this.myInformations.getAddress().equals(m.getAddress())){
            myLogger.logMessage(
                    this.myInformations.getAddress().toString()
                            + " | Msg name : "
                            + m.hashCode()
                            + " | Msg send to : "
                            + m.getAddress()
            );
            myLogger.logMessage("Message arrived to destination");
        }
        if(this.myRoutingTable.getTable().isEmpty()){
            myLogger.logMessage("routing table of "+this.myInformations.getAddress()+" is empty");
            return;
        }
        m.decrementHops();
        int nbrHops = Integer.MAX_VALUE;
        //if the receiver is present on the neighbors table
        if(m.stillAlive() && this.myRoutingTable.getTable().keySet().contains(m.getAddress())){
            this.doPortConnection(this.pcop.getPortURI(),this.comAddressPortTable.get(m.getAddress())
                    ,CommunicationConnector.class.getCanonicalName());
            this.pcop.routeMessage(m);
        }
        //Parsing the
        AddressI next = null;
        HashMap<P2PAddressI,Set<RouteInfo>> map = this.myRoutingTable.getTable();
        for (Map.Entry<P2PAddressI,Set<RouteInfo>> table: map.entrySet()) {
            Set<RouteInfo> routeInfo = table.getValue();
            Iterator<RouteInfo> iterator = routeInfo.iterator();
            while(iterator.hasNext()){//Looping throught the collection
                RouteInfo actual = iterator.next();
                if(actual.getDestination().equals(m.getAddress())
                        && actual.getNumberOfHops() < nbrHops){
                    nbrHops = actual.getNumberOfHops();
                    next = table.getKey();
                }
            }
        }
        //If the value has not been updated, aka the aimed node is not known
        if(nbrHops == Integer.MAX_VALUE){
            floodMessageTransit(m);
            return;
        }
        this.doPortConnection(this.pcop.getPortURI(),this.comAddressPortTable.get(next)
                ,CommunicationConnector.class.getCanonicalName());
        this.pcop.routeMessage(m);
    }

    /**
     * Description: Route the message on the network, should be called to transit the message.
     * @param m of type Message - the message to send
     * @throws Exception the exception
     */
    public void routeMessage(MessageI m) throws Exception {
        if (m instanceof MessageI){
            Message msg = (Message) m;
            routeByTable(msg);
        }
    }


    /**
     * Description: Update neighbors routing table.
     * @throws Exception the exception
     */
//Les tables de routage vont être mise a jour
    public void updateNeighborsRoutingTable() throws Exception {
        HashMap<P2PAddressI,Set<RouteInfo>> map = this.myRoutingTable.getTable();
        System.out.println("bonjour mon adresse: " + this.myInformations.getAddress());
        for (Map.Entry<P2PAddressI,Set<RouteInfo>> table: map.entrySet()) {
            System.out.println("mon adresse: " + this.myInformations.getAddress());
            Set<RouteInfo> routeInfo = table.getValue();
            this.doPortConnection(
                    this.prtop.getPortURI(),
                    this.routingAddressPortTable.get(table.getKey()),
                    RoutageConnector.class.getCanonicalName()
            );
            this.prtop.updateRouting(table.getKey(),routeInfo);
        }
    }

    @Override
    public void start() throws ComponentStartException {
        super.start();
        try {
            this.pcip = new ParticipantCommunicationInboundPort(UUID.randomUUID().toString(),this);
            this.pcip.publishPort();
            this.pcop = new ParticipantCommunicationOutboundPort(this);   //creation du port
            this.pcop.publishPort();     //publication du port
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute() throws Exception {


        // Random rd = new Random(); // creating Random object
        // rd.nextInt();

        // Thread.sleep(rd.nextInt(1000));
        // super.execute();
        this.toggleLogging();
        // this.logMessage(" Objet:"+ this.hashCode()+ " Start Execute");
        try {
            registrateOnNetwork();
            newOnNetwork();
            if (this.comAddressPortTable.keySet().toArray().length > 0){
                Object randomName = this.comAddressPortTable.keySet().toArray()[new Random().nextInt(this.comAddressPortTable.keySet().toArray().length)];
                Message msg = new Message((AddressI) randomName);
                routeMessage(msg);
            }
            updateNeighborsRoutingTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finalise() throws Exception
    {
        this.logMessage(" nb comAddressPortTable : "+this.comAddressPortTable.size() +"\n"+this.comAddressPortTable );
        this.printExecutionLog();
        //System.out.println(this.comAdressPortTable);
        //System.out.println("mon adresse: " + this.);
//        System.out.println("mon portInbound: " + this.prtip.getPortURI());
//        System.out.println("ma routingTablePort: " + this.routingAddressPortTable);
        System.out.println("Table de routage :");
        HashMap<P2PAddressI,Set<RouteInfo>> map = this.myRoutingTable.getTable();
        System.out.println(map);
        for (Map.Entry<P2PAddressI,Set<RouteInfo>> table: map.entrySet()) {
            //System.out.println("mes routes: " + this.myRoutingTable.getRoutes(table.getKey()));
            Set<RouteInfo> routeInfo = table.getValue();
            Iterator<RouteInfo> iterator = routeInfo.iterator();
            while(iterator.hasNext()) {
                RouteInfo actual = iterator.next();
                //System.out.println("mon adresse: " + this.myInformations.getAddress() + "mes voisins: " + table.getKey() + " ma destination " + actual.getDestination() + "");
            }
        }
        this.doPortDisconnection(this.prop.getPortURI());
        this.prop.unpublishPort();
        super.finalise();
    }
}