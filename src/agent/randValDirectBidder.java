package agent;

import calcAuction.FileInput;
import database.DatabaseConn;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class randValDirectBidder extends Agent {
    //FileInput randValue = new FileInput();
    DatabaseConn app = new DatabaseConn();
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<Agents> sortedListSeller = new ArrayList<Agents>();

    agentInfo bidderInfo;

    /***
    int cnt = 0;
    int fiveHundredVol = 500;
    int fiveHundredVolFreq;
    double varieVol;
    int varieVolFreq = 1;
     ***/

    protected void setup() {
        //Farmer information is loaded from database.
        //Randomized data input.
        app.selectBiddersRandom();

        //Fixed data input
        //app.selectBidders(getLocalName());

        bidderInfo = new agentInfo(getAID().getLocalName(), app.Name, app.FarmSize, app.ConsentPrice, app.WaterReq, app.TotalProfitValue, app.TotalFarmGrossMargin,
                app.PctReduction, app.WaterReqAfterReduction, app.ProfitAfterReduction, "bidder",(0.1 * app.ConsentPrice)/100, (app.WaterReq - app.WaterReqAfterReduction));

        System.out.println(getAID().getLocalName() + "  is ready");

        //Start Agent
        // Register the service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        System.out.println(bidderInfo.toString());
        System.out.println();
        sd.setType("bidder");

        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //Bidding process.

        //Information to monitor.
        addBehaviour(new AdverInfoMsg());

        //Add the behavior serving queries from Water provider about current price.
        addBehaviour(new OfferRequestsServer());
        //Add the behavior serving purchase orders from water provider agent.
        addBehaviour(new PurchaseOrdersServer());

        //addBehaviour(new RejectProposalProcess());

        //addBehaviour(new RejectandReset());
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // De-register from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(getAID().getName() + " terminating.");
    }

    private class OfferRequestsServer extends Behaviour {
        //search agent in DF
        private AID[] sellerList;
        private int replyCnt;
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            //Search Sellers
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("seller");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                sellerList = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    sellerList[i] = result[i].getName();
                    //System.out.println(sellerList[i]);

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            switch (step) {
                case 0:
                    //receive CFP
                    //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);

                    if (msg != null) {
                        if (msg.getPerformative() == ACLMessage.CFP && msg.getSender().getLocalName().equals("monitor")==false) {
                            System.out.println(msg);
                            replyCnt++;
                            //ACLMessage reply = msg.createReply();
                            //Price Per MM. and the number of volume to sell from Seller.
                            String currentOffer = msg.getContent();
                            String[] arrOfstr = currentOffer.split("-");
                            double tempSellVol = Double.parseDouble(arrOfstr[0]);
                            double tempPrice = Double.parseDouble(arrOfstr[1]);
                            String tempNameDB = arrOfstr[2];

                            System.out.println( "Agent Name: " + getLocalName() + "  NameDB: " + tempNameDB + " Offer Vol:  " + tempSellVol + " Selling price: " + tempPrice);

                            //Comparing the price between reserved price and office price.
                            if(tempPrice < bidderInfo.buyingPrice && ((bidderInfo.buyingVol * 1.2) < tempSellVol)){
                                sortedListSeller.add(new Agents(msg.getSender().getLocalName(),tempNameDB,tempSellVol,tempPrice));
                            }
                        }

                        if (replyCnt == sellerList.length) {
                            System.out.println(getLocalName() + "  receive all CFP " + "size  :  " + sortedListSeller.size());
                            Collections.sort(sortedListSeller, new SortedByPrice());

                            if(sortedListSeller.size() == 0){
                                System.out.println("Do not have any matching offers");
                                step = 1;
                            }

                            System.out.println("Sorted List Result which from sellers request:>>>>>>>>>>>>>>>>" + "\n" + getLocalName() + "\n");
                            for (int i = 0; i <= sortedListSeller.size() - 1; i++) {
                                System.out.println(sortedListSeller.get(i) + "\n");
                            }
                            step = 1;
                        }

                    } else {
                        block();
                    }
                    break;

                case 1:
                    //Sending PROPOSE message to Seller (only the best option for volume requirement).

                    //decision for PROPOSE message sending.
                    System.out.println("\n" + "Decision making starting >>>>>>>");

                    for (int i = 0; i < sellerList.length; i++) {
                        if(sortedListSeller.size() != 0){
                            if (sellerList[i].getLocalName().equals(sortedListSeller.get(0).name)) {
                                ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
                                reply.setContent(bidderInfo.farmerName + "-" + sortedListSeller.get(0).sellingVol + "-" + bidderInfo.buyingPrice);
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[i]);
                                myAgent.send(reply);
                                System.out.println(reply);
                            }
                        } else {
                            ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                            //reply.setContent(getLocalName() + "-" + sortedListSeller.get(j).sellingVol + "-" + 0 + "-" + 0);
                            reply.setConversationId("bidding");
                            reply.setReplyWith("reply" + System.currentTimeMillis());
                            reply.addReceiver(sellerList[i]);
                            myAgent.send(reply);
                            System.out.println(reply);
                        }
                    }
                    step = 2;
                    break;
            }
        }

        public boolean done() {
            if(step==2 && sortedListSeller.size() == 0){
                doSuspend();
            }
            return step == 2;
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("");
                System.out.println("");
                System.out.println("\n" + getAID().getLocalName() + " accpted to buy water from " + msg.getSender().getLocalName());
                myAgent.send(reply);
                System.out.println(reply);
                System.out.println(getAID().getName() + " terminating.");
                myAgent.doSuspend();
                takeDown();

            } else {
                block();
            }

            //
            //myGUI.dispose();
            //
        }
    }

    private class RejectProposalProcess extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                addBehaviour(new OfferRequestsServer());
            } else {
                block();
            }
        }
    }

    private class AdverInfoMsg extends OneShotBehaviour {
        private AID[] mornitorAgent;
        public void action(){
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("mornitor");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //System.out.println("Found auctioneer agents:");
                mornitorAgent = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    mornitorAgent[i] = result[i].getName();
                    //System.out.println(mornitorAgent[i].getName());
                }
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            // Send the CFP to all sellers (Sending water volume required to all bidding agent)
            ACLMessage mornitorReply = new ACLMessage(ACLMessage.PROPOSE);
            for (int i = 0; i < mornitorAgent.length; ++i) {
                mornitorReply.addReceiver(mornitorAgent[i]);
            }

            mornitorReply.setContent(bidderInfo.farmerName + "-" + bidderInfo.buyingVol + "-" + bidderInfo.buyingPrice + "-"
                    + bidderInfo.totalProfitValue + "-" + bidderInfo.profitAfterReduction);
            myAgent.send(mornitorReply);
        }
    }

    public class agentInfo {
        String agentName;
        String farmerName;
        double farmSize;
        Double consentPrice;
        Double waterReq;
        Double totalProfitValue;
        Double totalGrossMargin;
        Double pctReduction;
        Double waterReqAfterReduction;
        Double profitAfterReduction;
        String agentType;
        Double buyingPrice;
        Double buyingVol;

        agentInfo(String agentName, String farmerName, double farmSize, double consentPrice, double waterReq, double totalProfitValue, double totalGrossMargin, double pctReduction,
                  double waterReqAfterReduction, double profitAfterReduction, String agentType, double buyingPrice, double buyingVol) {
            this.agentName = agentName;
            this.farmerName = farmerName;
            this.farmSize = farmSize;
            this.consentPrice = consentPrice;
            this.waterReq = waterReq;
            this.totalProfitValue = totalProfitValue;
            this.totalGrossMargin = totalGrossMargin;
            this.pctReduction = pctReduction;
            this.waterReqAfterReduction = waterReqAfterReduction;
            this.profitAfterReduction = profitAfterReduction;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVol = buyingVol;
        }

        public String toString() {
            return "Bidder Name: " + this.agentName + " " + "DB order: " + this.farmerName + "  " + "Buying Volume: " + df.format(this.buyingVol) + " " + "Price: " + this.buyingPrice + " Profit loss: " + (this.totalProfitValue - this.profitAfterReduction);
        }
    }

    //adding new class for sorted seller agent data.
    class Agents {
        String localName;
        String name;
        double sellingVol;
        double sellingPrice;

        //Constructor
        public Agents(String localName, String nameDB, double sellingVol, double sellingPrice) {
            this.localName = localName;
            this.name = nameDB;
            this.sellingVol = sellingVol;
            this.sellingPrice = sellingPrice;
        }

        public String toString() {
            return "Agent name: " + this.name + "  " + " Dict order no. : " + this.sellingVol + "  " + " Volume: " + this.sellingPrice;
        }
    }

    //Sorted by volume (descending).
    class SortedByPrice implements Comparator<Agents> {
        //Used for sorting in ascending order of the volume.
        public int compare(Agents a, Agents b) {
            return Double.compare(a.sellingPrice, b.sellingPrice);
        }
    }
}