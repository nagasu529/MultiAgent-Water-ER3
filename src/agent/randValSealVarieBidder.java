package agent;

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
import calcAuction.FileInput;
import database.DatabaseConn;

public class randValSealVarieBidder extends Agent {
    FileInput randValue = new FileInput();
    DatabaseConn app = new DatabaseConn();
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<Agents> sortedListSeller = new ArrayList<Agents>();
    ArrayList<Agents> proposeSortedList = new ArrayList<Agents>();
    ArrayList<catalogInfo> bidingCatalog = new ArrayList<catalogInfo>();



    //agentInfo bidderInfo = new agentInfo("", "bidder", randValue.getRandDoubleRange(10, 16), randValue.getRandDoubleRange(300, 2000));
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
        //app.selectBiddersRandom();

        //Fixed data input
        app.selectBidders(getLocalName());

        bidderInfo = new agentInfo(getAID().getLocalName(), app.Name, app.FarmSize, app.ConsentPrice, app.WaterReq, app.TotalProfitValue, app.TotalFarmGrossMargin,
                app.PctReduction, app.WaterReqAfterReduction, app.ProfitAfterReduction, "bidder",(0.1 * app.ConsentPrice)/100, (app.WaterReq - app.WaterReqAfterReduction));

        System.out.println(getAID().getLocalName() + "  is ready");

        //Selling volume spited by conditions (each group is not over 500 mm^3).
        //adding the splitting volume to catalog.
        double tempNumBuyingCatalogItem = bidderInfo.buyingVol/500;
        if(tempNumBuyingCatalogItem < 1) {
            bidingCatalog.add(new catalogInfo(0, bidderInfo.buyingVol, "none", 0.0, 0.0));
        }else {
            int fiveHundredVolFreq = (int)(tempNumBuyingCatalogItem);
            double varieVol = bidderInfo.buyingVol - (fiveHundredVolFreq * 500);
            bidingCatalog.add(new catalogInfo(0, varieVol, "none", 0.0, 0.0));
            while (fiveHundredVolFreq > 0) {
                bidingCatalog.add(new catalogInfo(fiveHundredVolFreq, 500.00, "none", 0.0, 0.0));
                fiveHundredVolFreq--;
            }
        }

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
                            int tempNumofFiveHundredVol = Integer.parseInt(arrOfstr[0]);
                            double tempVarieValue = Double.parseDouble(arrOfstr[1]);
                            System.out.println( getLocalName() + "   " + msg.getSender().getLocalName() + " Offer Vol:  " + ((tempNumofFiveHundredVol * 500) + tempVarieValue));

                            if(tempNumofFiveHundredVol == 0){
                                sortedListSeller.add(new Agents(msg.getSender().getLocalName(), 0, tempVarieValue));
                            }else{
                                while(tempNumofFiveHundredVol > 0){
                                    sortedListSeller.add(new Agents(msg.getSender().getLocalName(), tempNumofFiveHundredVol, 500));
                                    tempNumofFiveHundredVol--;
                                }
                            }
                        }

                        if (replyCnt == sellerList.length) {
                            System.out.println(getLocalName() + "  receive all CFP ");
                            Collections.sort(sortedListSeller, new SortbyVolume());

                            System.out.println("Sorted List Result which from sellers request:>>>>>>>>>>>>>>>>" + "\n" + getLocalName() + "\n");
                            for (int i = 0; i <= sortedListSeller.size() - 1; i++) {
                                System.out.println(sortedListSeller.get(i) + "\n");
                            }

                            //decision for PROPOSE message sending.
                            System.out.println("\n" + "Decision making starting>>>>>>>");

                            for(int i = 0; i <= bidingCatalog.size() - 1; i++) {
                                catalogInfo tempBiddingCatalog = bidingCatalog.get(i);
                                for (int j = 0; j <= sortedListSeller.size() - 1; j++) {
                                    if(sortedListSeller.get(j).dictOrderNo == tempBiddingCatalog.dictOrder) {
                                        if(sortedListSeller.get(j).volume >= tempBiddingCatalog.volume) {
                                            tempBiddingCatalog.winnerName = sortedListSeller.get(j).name;
                                            tempBiddingCatalog.winnerVol = sortedListSeller.get(j).volume;
                                            bidingCatalog.remove(i);
                                            bidingCatalog.add(i,tempBiddingCatalog);
                                            sortedListSeller.remove(j);
                                            break;
                                        }
                                    }
                                    tempBiddingCatalog.winnerVol = 0.0;
                                    bidingCatalog.remove(i);
                                    bidingCatalog.add(i, tempBiddingCatalog);

                                }
                            }


                            //Verified result.
                            System.out.println("Verified result>>>>>>>>>>>>>>>>>>>>>>>       " + getLocalName() + "\n");
                            for (int i = 0; i <= bidingCatalog.size() - 1; i++) {
                                System.out.println("Result from " + getLocalName() + " Winner is " + bidingCatalog.get(i).winnerName + "  " + bidingCatalog.get(i).dictOrder +
                                        "  " + bidingCatalog.get(i).winnerName + "  " + bidingCatalog.get(i).winnerVol);
                            }

                            step = 1;
                        }

                    } else {
                        block();
                    }
                    break;

                case 1:
                    //Sending PROPOSE message to Seller (only the best option for volume requirement.

                    for (int i = 0; i < sellerList.length; i++) {
                        for (int j = 0; j <= bidingCatalog.size() - 1; j++) {
                            if (sellerList[i].getLocalName().equals(bidingCatalog.get(j).winnerName)) {
                                ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
                                reply.setContent(bidderInfo.farmerName + "-" + bidingCatalog.get(j).dictOrder + "-" + bidingCatalog.get(j).winnerVol + "-" + bidderInfo.buyingPrice);
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[i]);
                                myAgent.send(reply);
                                System.out.println(reply);
                            }else {
                                ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                                reply.setContent(getLocalName() + "-" + bidingCatalog.get(j).dictOrder + "-" + 0 + "-" + 0);
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[i]);
                                myAgent.send(reply);
                                System.out.println(reply);
                            }
                        }
                    }


                    while(sortedListSeller.size() > 0) {
                        Agents tempSortedListSeller = sortedListSeller.get(0);
                        String agentName = tempSortedListSeller.name;
                        int tempDictNo = tempSortedListSeller.dictOrderNo;
                        for (int i = 0; i < sellerList.length; i++) {
                            if(sellerList[i].getLocalName().equals(agentName)) {
                                ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                                reply.setContent(getLocalName() + "-" + tempDictNo + "-" + 0 + "-" + 0);
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[i]);
                                myAgent.send(reply);
                                System.out.println(reply);
                            }
                        }
                        sortedListSeller.remove(0);
                    }


                    step = 2;
                    break;
            }
        }

        public boolean done() {
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

    public class catalogInfo{
        int dictOrder;
        double volume;
        String winnerName;
        double winnerVol;
        double winnerPrice;
        catalogInfo(int dictOrder, double volume, String winnerName, double winnerVol, double winnerPrice){
            this.dictOrder = dictOrder;
            this.volume = volume;
            this.winnerName = winnerName;
            this.winnerVol = winnerVol;
            this.winnerPrice = winnerPrice;
        }
        public String toString() {
            return "Dict no.: " + this.dictOrder + "  Vol" + this.volume + "   Winner: " + this.winnerName + "  Vol: " + this.winnerVol + "\n";
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
        String name;
        int dictOrderNo;
        double volume;

        //Constructor
        public Agents(String name, int dictOrderNo, double volume) {
            this.name = name;
            this.dictOrderNo = dictOrderNo;
            this.volume = volume;
        }

        public String toString() {
            return "Agent name: " + this.name + "  " + " Dict order no. : " + this.dictOrderNo + "  " + " Volume: " + this.volume;
        }
    }

    //Sorted by volume (descending).
    class SortbyVolume implements Comparator<Agents> {
        //Used for sorting in ascending order of the volume.
        public int compare(Agents a, Agents b) {
            return Double.compare(a.volume, b.volume);
        }
    }
}