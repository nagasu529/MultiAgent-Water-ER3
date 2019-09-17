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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Type2BidderCombinatorial extends Agent {
    //The list of farmer who are seller (maps the water volume to its based price)
    FileInput randValue = new FileInput();
    DatabaseConn app = new DatabaseConn();
    DecimalFormat df = new DecimalFormat("#.##");

    //Farmer information on each agent.
    agentInfo farmerInfo;
    String mornitoringMsg;
    //agentInfo farmerInfo = new agentInfo("", randValue.getRandDoubleRange(300, 2000),randValue.getRandDoubleRange(10,16), randValue.getRandDoubleRange(5,12));
    //Global bidding parameter
    ArrayList<Agents> sortedListSeller = new ArrayList<Agents>();
    ArrayList<Agents> refuseNewList = new ArrayList<Agents>();
    //All behaviour is running on here. It's working on setup price for bidding process.


    
    //Creating list integer for random reduction percentage.
    Integer[] arrReducList = new Integer[]{50,60,70,80,90,100};
    int pctPriceReduce = 0;
    List<Integer> pctPriceReducList = Arrays.asList(arrReducList);
    
    //Import Random
    Random rand = new Random();
    
    int randomNum;
    String winner;

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );
        app.selectBiddersRandom();
        farmerInfo = new agentInfo(getAID().getLocalName(), app.Name, app.FarmSize, app.ConsentPrice, app.WaterReq, app.TotalProfitValue, app.TotalFarmGrossMargin, 
        		app.PctReduction, app.WaterReqAfterReduction, app.ProfitAfterReduction, "bidder",(0.1 * app.ConsentPrice)/100, (app.WaterReq - app.WaterReqAfterReduction));
        
        mornitoringMsg = farmerInfo.farmerName + "-" + farmerInfo.buyingVol + "-" + farmerInfo.buyingPrice + "-" + farmerInfo.profitAfterReduction;
        
        //Setting up new price with all new random in percentage.
        pctPriceReduce = pctPriceReducList.get(rand.nextInt(pctPriceReducList.size()));

        double oldPrice = farmerInfo.buyingPrice;
        farmerInfo.buyingPrice = (pctPriceReduce * farmerInfo.buyingPrice)/100;
        String outputPctTypeTwo = String.format("Neutral price is: %s   Covetous is: %s", String.valueOf(df.format(oldPrice)),String.valueOf(df.format(farmerInfo.buyingPrice)));
        System.out.println(outputPctTypeTwo);
        //System.out.println("Neutral price is:  " + oldPrice + "  type two price is:  " + farmerInfo.buyingPrice);
        
        //Start Agent
        // Register the book-selling service in the yellow pages
        //farmerInfo.farmerName = getAID().getLocalName();
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bidder");
        //sd.setType(farmerInfo.agentType);
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //Monitoring information sending.
        addBehaviour(new AdverInfoMsg());

        //Bidding process.
        //Add the behavior serving queries from Water provider about current price.
        addBehaviour(new OfferRequestsServer());

        //Add the behavior serving purchase orders from water provider agent.
        addBehaviour(new PurchaseOrdersServer());
        //addBehaviour(new StopProcess());
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // De-register from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Close the GUI
        //myGUI.dispose();
        // Printout a dismissal message
        doSuspend();
        System.out.println(getAID().getName()+" terminating.");
    }

    private class OfferRequestsServer extends Behaviour {
        //search agent in DF
        private AID[] sellerList;
        private int replyCnt;
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            //Search Sellers
        	String serviceName = "seller";
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(serviceName);
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
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if (msg.getPerformative() == ACLMessage.CFP && msg.getSender().getLocalName().equals("monitor")==false) {
                            //System.out.println(msg);
                            replyCnt++;
                            //ACLMessage reply = msg.createReply();
                            //Price Per MM. and the number of volume to sell from Seller.
                            String currentOffer = msg.getContent();
                            System.out.println(msg);
                            double tempVolumn = Double.parseDouble(msg.getContent());
                            //String[] arrOfstr = currentOffer.split("-");
                            //double tempVolumn = Double.parseDouble(arrOfstr[0]);
                            System.out.println(msg.getSender().getLocalName() + " OfferVol:  " + tempVolumn + "  " + farmerInfo.buyingVol);
                            sortedListSeller.add(new Agents(tempVolumn, msg.getSender().getLocalName(), "none"));

                        }
                        System.out.println("\n");

                        if (replyCnt >= sellerList.length) {
                            //Collections.sort(sortedListSeller, new SortbyTotalVol());
                            System.out.println("start +++++++++++++++++++++++++++++++++++++++++++" + "\n");
                            
                            //count and loop.
                            
                            for (int i = 0; i <= sortedListSeller.size() -1; i++){
                                System.out.println(sortedListSeller.get(i).toString());
                                if(sortedListSeller.get(i).totalVolume < farmerInfo.buyingVol){
                                    refuseNewList.add(sortedListSeller.get(i));
                                	sortedListSeller.remove(i);
                                }
                            }

                            if(refuseNewList.size() > 0){
                                System.out.println("Do not have matching water to buy from all sellers.");
                                step = 1;
                            }else {
                            	randomNum = ThreadLocalRandom.current().nextInt(0, sortedListSeller.size());
                                System.out.println("The best option for sending offer is  " + sortedListSeller.get(randomNum).toString());
                                sortedListSeller.get(randomNum).status = "accept";
                                step = 1;
                            }
                        }

                    } else {
                        block();
                    }
                    break;

                case 1:
                    //Sending PROPOSE message to Seller (only the best option for volume requirement.

                    for(int i = 0; i <= sortedListSeller.size() -1; i++){
                        for (int j = 0; j < sellerList.length; j++) {
                            if (sortedListSeller.get(i).name.equals(sellerList[j].getLocalName()) && sortedListSeller.get(i).status.equals("accept")) {
                                ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
                                //reply.setContent(proposeSortedList.get(0).totalVolume + "-" + farmerInfo.buyingPricePerMM + "-" + farmerInfo.profitLossPct);
                                reply.setContent(farmerInfo.buyingVol + "-" + farmerInfo.buyingPrice + "-" + (farmerInfo.totalProfitValue - farmerInfo.profitAfterReduction) + "-" + farmerInfo.farmerName + "-" + pctPriceReduce);
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[j]);
                                myAgent.send(reply);
                                //System.out.println(reply);
                            }else if (sortedListSeller.get(i).name.equals(sellerList[j].getLocalName())) {
								ACLMessage reply  = new ACLMessage(ACLMessage.REFUSE);
								reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[j]);
                                myAgent.send(reply);
							}
                        }
                    }
                    
                    for(int i = 0; i <= refuseNewList.size() -1; i++){
                        for (int j = 0; j < sellerList.length; j++) {
                            if (refuseNewList.get(i).name.equals(sellerList[j].getLocalName())) {
                            	ACLMessage reply  = new ACLMessage(ACLMessage.REFUSE);
								reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[j]);
                                myAgent.send(reply);
							}
                        }
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
                System.out.println("");
                System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                myAgent.send(reply);
                System.out.println(reply);
                myAgent.doSuspend();
                takeDown();
                System.out.println(getAID().getName() + " terminating.");
            }else {
                block();
            }
        }
    }
    
    private class StopProcess extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
            	System.out.println(msg);
                System.out.println("\n" + getAID().getLocalName() + " lose biding");
                System.out.println(getAID().getName() + " terminating.");
                myAgent.doSuspend();
                takeDown();
                
            }else {
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
            // Send the cfp to all sellers (Sending water volume required to all bidding agent)
            ACLMessage mornitorReply = new ACLMessage(ACLMessage.PROPOSE);
            for (int i = 0; i < mornitorAgent.length; ++i) {
                mornitorReply.addReceiver(mornitorAgent[i]);
            }
            //double tempProfitLoss = farmerInfo.totalProfitValue - farmerInfo.profitAfterReduction;
            mornitorReply.setContent(farmerInfo.farmerName + "-" + farmerInfo.buyingVol + "-" + farmerInfo.buyingPrice + "-" + 
            farmerInfo.totalProfitValue + "-" + farmerInfo.profitAfterReduction);
            myAgent.send(mornitorReply);
            System.out.println(mornitorReply.toString()+"\n");
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
        //double varieVolume;
        //int fivehundredFeq;
        double totalVolume;
        String name;
        String status;

        //Constructor
        public Agents(double totalVolume, String name, String status) {
            //this.varieVolume = varieVolume;
            //this.fivehundredFeq = fivehundredFeq;
            this.totalVolume = totalVolume;
            this.name = name;
            this.status = status;
        }

        public String toString() {
            //return this.name + " " + this.varieVolume + " " + this.fivehundredFeq + "  total Volume: " + (this.varieVolume + (this.fivehundredFeq * 500));
            return this.name + " " + "  total Volume: " + this.totalVolume + " bidder name :" + farmerInfo.farmerName + "minimum vol req: " +farmerInfo.buyingVol;
        }
    }
    class SortbyTotalVol implements Comparator<Agents> {
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b) {
            return Double.compare(a.totalVolume, b.totalVolume);
        }
    }
}