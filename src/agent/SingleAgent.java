package agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.text.DecimalFormat;
import java.util.*;

import calcAuction.FileInput;

public class SingleAgent extends Agent {

    //The list of farmer who are seller (maps the water volume to its based price)
    private SingleAgentGui myGui;
    FileInput calCrops = new FileInput();
    DecimalFormat df = new DecimalFormat("#.##");

    //The list of known water selling agent
    private AID[] bidderAgent;

    //Counting list (single negotiation process)
    int countTick;

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", "", 0.0, 10, 0, 0, "avalable", 0.0, 0.0, 10, 10, 0.0, 0);

    //The list of information (buying or selling) from agent which include price and mm^3
    private HashMap catalogue = new HashMap();

    protected void setup(){
        System.out.println(getAID()+" is ready");

        //Creating catalog and running GUI
        myGui = new SingleAgentGui(this);
        myGui.show();
        //Start agent

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        farmerInfo.agentType = "Farmer";
        ServiceDescription sd = new ServiceDescription();
        sd.setType(farmerInfo.agentType);
        sd.setName(getAID().getName());
        farmerInfo.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        ArrayList<calcAuction.FileInput.cropType> outputList = new ArrayList<calcAuction.FileInput.cropType>();



        myGui.displayUI("Hello "+ farmerInfo.farmerName + "\n" + "Stage is " + farmerInfo.agentType + "\n");

        calCrops.randFarmFactorValues(outputList);

        double consentCost = calCrops.getRandDoubleRange(10000, 20000);
        //String log = calCrops.calcCropEU(outputList,0);
        //myGui.displayUI(log);
        calCrops.calcCropEU(outputList,1);
        //Display Farm information source arrays.
        myGui.displayUI("Source farming information" + "\n");
        for (int i = 0; i < outputList.size(); i++){
            myGui.displayUI(outputList.get(i).toStringSource() + "\n");
        }



        String resultLog1 = calCrops.calcWaterReduction(5, outputList, getAID().getLocalName(), consentCost);
        myGui.displayUI(resultLog1);

        String resultLog2 = calCrops.calcWaterReduction(10, outputList, getAID().getLocalName(), consentCost);
        myGui.displayUI(resultLog2);

        String resultLog3 = calCrops.calcWaterReduction(15, outputList, getAID().getLocalName(),consentCost);
        myGui.displayUI(resultLog3);

        String resultLog4 = calCrops.calcWaterReduction(20, outputList, getAID().getLocalName(), consentCost);
        myGui.displayUI(resultLog4);

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 50000){
            protected void onTick() {

                myGui.displayUI("Agent status is " + farmerInfo.agentType + "\n");
                if (farmerInfo.agentType=="owner"||farmerInfo.agentType=="Farmer-owner") {
                    //Register the seller description service on yellow pages.
                    farmerInfo.agentType = "Farmer-owner";
                    //farmerInfo.pricePerMM = 10;
                    sd.setType(farmerInfo.agentType);
                    sd.setName(getAID().getName());
                    farmerInfo.farmerName = getAID().getName();
                    farmerInfo.minPricePerMM = farmerInfo.pricePerMM;

                    myGui.displayUI("\n");
                    myGui.displayUI("Name: " + farmerInfo.farmerName + "\n");
                    myGui.displayUI("Status: " + farmerInfo.agentType + "\n");
                    myGui.displayUI("Volumn to sell: " + farmerInfo.waterVolumn + "\n");
                    myGui.displayUI("Selling price: " + farmerInfo.pricePerMM + "\n");
                    myGui.displayUI("Selling status: " + farmerInfo.sellingStatus + "\n");
                    myGui.displayUI("Maximum bidding: " + farmerInfo.maxPricePerMM + "\n");
                    myGui.displayUI("Providing price" + "\n");
                    myGui.displayUI("\n");

                    /*
                     ** Selling water process
                     */
                    addBehaviour(new RequestPerformer());
                    // Add the behavior serving purchase orders from buyer agents
                    //addBehaviour(new PurchaseOrdersServer());

                }
            }
        } );
    }

    /*
     * 	Request performer
     *
     * 	This behaviour is used by buyer mechanism to request seller agents for water pricing ana selling capacity.
     */
    private class RequestPerformer extends Behaviour {
        private AID bestBidder; // The agent who provides the best offer
        private double bestPrice;  // The best offered price
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        double waterVolFromBidder;
        double biddedPriceFromBidder;
        int proposeCnt, refuseCnt;


        private int step = 0;

        public void action() {
            switch (step) {
                case 0:

                    //update bidder list
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if(result.length > 1){
                            countTick = countTick+1;
                        }
                        System.out.println("Found acutioneer agents:");
                        bidderAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgent[i] = result[i].getName();
                            System.out.println(bidderAgent[i].getName());
                            System.out.println("tick time:" + countTick);
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Send the cfp to all sellers (Sending water volume required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgent.length; ++i) {
                        if (bidderAgent[i].getName().equals(farmerInfo.farmerName)== false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    if(farmerInfo.currentPricePerMM >= farmerInfo.pricePerMM){
                        cfp.setContent(String.valueOf(Double.toString(farmerInfo.waterVolumn)+ "-"
                                +Double.toString(farmerInfo.currentPricePerMM) + "-" + Integer.toString(farmerInfo.numBidder)));
                    }else {
                        cfp.setContent(String.valueOf(Double.toString(farmerInfo.waterVolumn) + "-" + Double.toString(farmerInfo.pricePerMM))+
                                "-" + Double.toString(farmerInfo.pricePerMM));
                    }
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    System.out.println("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    System.out.println(step);
                    break;

                case 1:

                    // Receive all proposals/refusals from bidder agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            proposeCnt++;
                            System.out.println("Receive message: " + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            waterVolFromBidder = Double.parseDouble(arrOfStr[0]);
                            biddedPriceFromBidder = Double.parseDouble(arrOfStr[1]);


                            if (bestBidder == null || biddedPriceFromBidder > bestPrice) {
                                // This is the best offer at present
                                bestPrice = biddedPriceFromBidder;
                                farmerInfo.currentPricePerMM = bestPrice;
                                bestBidder = reply.getSender();
                            }
                        }else if (reply.getPerformative() == ACLMessage.REFUSE){
                            refuseCnt++;
                        }
                        farmerInfo.numBidder = proposeCnt;
                        System.out.println("The number of current bidding is " + repliesCnt + "\n");
                        farmerInfo.numBidder = repliesCnt;
                        System.out.println("Surrender agent number is " + refuseCnt + "\n");
                        System.out.println("Best price is from " + bestBidder +"\n");
                        System.out.println("Price : " + bestPrice + "\n");

                        if (repliesCnt >= bidderAgent.length-1) {
                            // We received all replies
                            step = 2;
                            System.out.println(step);
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    if(refuseCnt >=1 && proposeCnt==1|| farmerInfo.numBidder ==1 && countTick > 5){
                        step = 3;
                        System.out.println(step);
                    }else {
                        step = 0;
                        System.out.println(step);
                        refuseCnt = 0;
                        proposeCnt = 0;
                        repliesCnt = 0;
                    }
                    break;
                case 3:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestBidder);
                    order.setContent(String.valueOf(farmerInfo.currentPricePerMM));
                    order.setConversationId("bidding");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));

                    step = 4;
                    System.out.println(step);
                    break;
                case 4:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName());
                            System.out.println("Price = "+farmerInfo.currentPricePerMM);
                            myGui.displayUI(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName().toString());
                            myGui.displayUI("Price = " + farmerInfo.currentPricePerMM);
                            //doSuspend();
                            //myAgent.doDelete();
                        }
                        else {
                            System.out.println("Attempt failed: requested water volumn already sold.");
                            myGui.displayUI("Attempt failed: requested water volumn already sold.");
                        }

                        step = 5;
                        System.out.println(step);
                        doSuspend();
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestBidder == null) {
                //System.out.println("Attempt failed: "+volumeToBuy+" not available for sale");
                myGui.displayUI("Attempt failed: do not have seller now".toString());
            }
            return ((step == 2 && bestBidder == null) || step == 5);
        }
    }

    /*
     * 	PurchaseOrderServer
     * 	This behavior is used by Seller agent to serve incoming offer acceptances (purchase orders) from buyer.
     * 	The seller agent will remove selling list and replies with an INFORM message to notify the buyer that purchase has been
     * 	successfully complete.
     */

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                myGui.displayUI(msg.toString());
                System.out.println(farmerInfo.sellingStatus);
                reply.setPerformative(ACLMessage.INFORM);
                if (farmerInfo.sellingStatus=="avalable") {
                    farmerInfo.sellingStatus = "sold";
                    //System.out.println(getAID().getName()+" sold water to agent "+msg.getSender().getName());
                    myGui.displayUI(getAID().getLocalName()+" sold water to agent "+msg.getSender().getLocalName());
                    //myGui.displayUI(farmerInfo.sellingStatus.toString());
                    //System.out.println(farmerInfo.sellingStatus);
                    doSuspend();
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available for sale");
                    myGui.displayUI("not avalable to sell");
                }

            }else {
                block();
            }
        }
    }

    public void updateCatalogue(final String agentName, final String agentType, final double waterVolumn, final double priceForSell){
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                //farmerInfo.
                //agentInfo agentInfo = new agentInfo(agentName, agentType, waterVolumn, priceForSell);
                //System.out.println(agentName+" need to sell water to others. The water volumn is = "+ volumeToSell);
                //System.out.println(agentInfo.agentType);
                //System.out.println(agentInfo.farmerName);
            }
        });
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println("Seller-agent "+getAID().getName()+" terminating.");
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        double waterVolumn;
        double currentLookingVolumn;
        double currentBidVolumn;
        double pricePerMM;
        String sellingStatus;
        double minPricePerMM;
        double maxPricePerMM;
        double currentPricePerMM;
        double bidedPrice;
        double previousPrice;
        int numBidder;

        agentInfo(String farmerName, String agentType, double waterVolumn, double currentLookingVolumn, double currentBidVolumn, double pricePerMM, String sellingStatus, double minPricePerMM, double maxPricePerMM,
                  double currentPricePerMM, double biddedPrice, double previousPrice, int numBidder){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.waterVolumn = waterVolumn;
            this.currentLookingVolumn = currentLookingVolumn;
            this.currentBidVolumn = currentBidVolumn;
            this.pricePerMM = pricePerMM;
            this.sellingStatus = sellingStatus;
            this.minPricePerMM = minPricePerMM;
            this.maxPricePerMM = maxPricePerMM;
            this.currentPricePerMM = currentPricePerMM;
            this.bidedPrice = biddedPrice;
            this.previousPrice = previousPrice;
            this.numBidder = numBidder;
        }
    }
}
