package agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import calcAuction.*;
import database.*;

public class randValSealbidedSeller extends Agent {
    randValSealbidedSellerGui myGui;
    DatabaseConn app = new DatabaseConn();

    //Initialize data for monitoring agent.
    public double totalVolumeLeftAfterAcution = 0.0;
    public double totalValueSellAfterAuction = 0.0;

    //General papameter information
    DecimalFormat df = new DecimalFormat("###,###.##");
    FileInput randValue = new FileInput();
    //agentInfo sellerInfo = new agentInfo("", "seller", randValue.getRandDoubleRange(10,12), randValue.getRandDoubleRange(10000,30000));
    String log = "";
    int FreqCnt = 2;

    ArrayList<Agents> informMessageList = new ArrayList<>();
    int informCnt = 0;
    agentInfo sellerInfo;

    protected void setup(){

        //Initialized the data input randomly.
        app.selectSellerRandom();

        //Initialized the fixed data input.
        //app.selectSeller(getLocalName());

        sellerInfo = new agentInfo(getLocalName(), app.Name, app.FarmSize, app.ConsentPrice, app.WaterReq, app.TotalProfitValue, app.TotalCost,
                app.grossMaginValue, app.PctReduction, app.WaterReqAfterReduction, app.ProfitAfterReduction, "Seller", app.SellingVol, app.SellingPrice);

        totalVolumeLeftAfterAcution = sellerInfo.sellingVol;
        // Create and show the GUI
        myGui = new randValSealbidedSellerGui(this);
        myGui.show();
        //sellerInfo.sellingVolumn = sellerInfo.sellingVolumn/2;

        //Start agent
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("seller");
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println(sellerInfo.farmerName + "  is ready" + "\n" + "Stage is  " + sellerInfo.agentType + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 10000){
            protected void onTick() {
                myGui.displayUI("Name: " + sellerInfo.farmerName + "\n");
                myGui.displayUI("Status: " + sellerInfo.agentType + "\n");
                myGui.displayUI("Volumn to sell: " + sellerInfo.sellingVol + "\n");
                myGui.displayUI("Selling price: " + sellerInfo.sellingPrice + "\n");
                myGui.displayUI("\n");

                /*
                 ** Selling water process
                 */
                addBehaviour(new RequestPerformer());
                addBehaviour(new PurchaseOrdersServer());
                // Add the behavior serving purchase orders from buyer agents
                //addBehaviour(new PurchaseOrdersServer());
            }
        } );
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
                //System.out.println("Found acutioneer agents:");
                mornitorAgent = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    mornitorAgent[i] = result[i].getName();
                    System.out.println("ggggggggggggggggggggggggggggg" + mornitorAgent[i].getName());
                }
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
            ACLMessage mornitorReply = new ACLMessage(ACLMessage.PROPOSE);
            for (int i = 0; i < mornitorAgent.length; ++i) {
                mornitorReply.addReceiver(mornitorAgent[i]);
            }
            //Adding information and sending to monitor.
            String message = sellerInfo.sellingPrice + "-" + sellerInfo.sellingVol + "-" + totalValueSellAfterAuction + "-" + totalVolumeLeftAfterAcution;

            mornitorReply.setContent(message);
            myAgent.send(mornitorReply);
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                myGui.displayUI("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                for(int i= 0; i <= informMessageList.size() - 1;i++){
                    if(informMessageList.get(i).name.equals(msg.getSender().getLocalName())){
                        myGui.displayUI(informMessageList.get(i).toString());
                    }
                }
                informCnt--;
                if (informCnt == 0){
                    System.out.println(getAID().getName() + " terminating.");
                    myAgent.doSuspend();
                }
            }else {
                block();
            }
        }
    }

    private class RequestPerformer extends Behaviour {
        //The list of known water selling agent
        private AID[] bidderAgent;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        int countTick;

        ArrayList<Agents> bidderReplyList = new ArrayList<>();
        ArrayList<String> output = new ArrayList<String>();


        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    //update bidder list
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("bidder");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if(result.length > 1){
                            countTick = countTick+1;
                        }
                        myGui.displayUI("Found acutioneer agents:\n");
                        bidderAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgent[i] = result[i].getName();
                            //myGui.displayUI(bidderAgent[i].getName()+ "\n");
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgent.length; ++i) {
                        if (bidderAgent[i].getName().equals(sellerInfo.farmerName)== false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    //cfp.setContent(String.valueOf(Double.toString(sellerInfo.sellingVolumn) + "-" + Double.toString((sellerInfo.sellingPrice))));
                    cfp.setContent(sellerInfo.sellingVol.toString());
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    //myGui.displayUI(cfp.toString());
                    //System.out.println("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    //mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    //ACLMessage reply = myAgent.receive(mt);
                    //mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            //myGui.displayUI("Receive message: \n" + reply + "\n");
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            double tempVolume = Double.parseDouble(arrOfStr[0]);
                            double tempPrice = Double.parseDouble(arrOfStr[1]);
                            double tempValue = tempPrice * tempVolume;
                            String tempDBName = arrOfStr[2];


                            //adding data to dictionary, compairing and storing data.
                            bidderReplyList.add(new Agents(tempVolume,tempPrice, tempValue, tempDBName, reply.getSender().getLocalName()));

                        }

                        if (repliesCnt >= bidderAgent.length) {
                            myGui.displayUI("done" + "\n");
                            Collections.sort(bidderReplyList, new SortbyValue());
                            Collections.reverse(bidderReplyList);

                            //Writting output.

                            String temmpString = getLocalName() + "," + sellerInfo.farmerName + "," + sellerInfo.sellingVol + "," + sellerInfo.sellingPrice + ",";
                            Agents tempOutputArray = new Agents(0.0, 0.0, 0.0, "none", "none");
                            if(bidderReplyList.size() != 0) {
                                tempOutputArray.name = bidderReplyList.get(0).name;
                                tempOutputArray.dbName = bidderReplyList.get(0).dbName;
                                tempOutputArray.totalVolume = bidderReplyList.get(0).totalVolume;
                                tempOutputArray.price = bidderReplyList.get(0).price;
                                temmpString = temmpString + tempOutputArray.name + "," + tempOutputArray.dbName + "," + tempOutputArray.totalVolume + "," + tempOutputArray.price;

                            }else {
                                temmpString = temmpString + tempOutputArray.name + "," + tempOutputArray.dbName + "," + tempOutputArray.totalVolume + "," + tempOutputArray.price;
                            }
                            output.add(temmpString);

                            myGui.displayUI("the best value from bidders is   " + temmpString);
                            myGui.displayUI("\n");

                            step = 2;
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:

                    //output file location.
                    String outputFile = "/Users/nagasu/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 		//Macbook
                    //String outputFile = "F:/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 	//Home PC
                    //String outputFile = "C:/Users/chiewchk/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv";  	//Office

                    //Writing the all bidder result calculation side to file.
                    try {
                        WriteToFile(output.toString(), outputFile);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    myGui.displayUI("\n" + "stop it!!!!!!" + "\n");
                    myAgent.doSuspend();

                    //Sorted propose message and matching to reply INFORM Message.

                    if(bidderReplyList.size() == 0) {
                        step = 4;
                        break;
                    }

                    /*
                     * calulating and adding accepted water volumn for bidder based on highest price.
                     * Sendding message to bidders wiht two types (Accept proposal or Refuse) based on
                     * accepted water volumn to sell.
                     */

                    informMessageList.add(bidderReplyList.get(0));
                    myGui.displayUI("bidder inform list: " + "\n");
                    for(int i = 0; i <= informMessageList.size()-1;i++){
                        myGui.displayUI(informMessageList.get(i).toString() + "\n");
                    }
                    informCnt = informMessageList.size();


                    //Sending PROPOSE message to Seller (only the best option for volume requirement.

                    for (int i = 0; i < bidderAgent.length; i++) {
                        for (int j = 0; j <= informMessageList.size() - 1; j++) {
                            if (bidderAgent[i].getLocalName().equals(informMessageList.get(j).name)) {
                                ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                //acceptMsg.setContent("500" + "-" + informMessageList.get(j).fivehundredFeq + "-" + informMessageList.get(j).varieVolume + "-" + "1" + "-" + informMessageList.get(i).price);
                                acceptMsg.setConversationId("bidding");
                                acceptMsg.setReplyWith("reply" + System.currentTimeMillis());
                                acceptMsg.addReceiver(bidderAgent[i]);
                                myAgent.send(acceptMsg);
                                System.out.println(acceptMsg);
                            }
                        }
                    }

                    for (int i = 0; i < bidderAgent.length; i++) {
                        for (int j = 0; j <= bidderReplyList.size() - 1; j++) {
                            if (bidderAgent[i].getLocalName().equals(bidderReplyList.get(j).name)) {
                                ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                //rejectMsg.setContent("500" + "-" + bidderReplyList.get(j).fivehundredFeq + "-" + bidderReplyList.get(j).varieVolume + "-" + "1");
                                rejectMsg.setConversationId("bidding");
                                rejectMsg.setReplyWith("reply" + System.currentTimeMillis());
                                rejectMsg.addReceiver(bidderAgent[i]);
                                myAgent.send(rejectMsg);
                                System.out.println(rejectMsg);
                            }
                        }
                    }

                    step = 3;
                    break;

                case 3:
                    // Receive the purchase order reply

                    reply = myAgent.receive(mt);
                    //int informCnt = informMessageList.size();
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            for(int i = 0; i <= informMessageList.size() -1; i++){
                                if(informMessageList.get(i).name.equals(reply.getSender().getLocalName())){
                                    String tempFreq = getAID().getLocalName() + "  Selling water to  " + reply.getSender().getLocalName() + "  " + informMessageList.get(i).totalVolume + "\n";
                                    totalValueSellAfterAuction = totalValueSellAfterAuction + informMessageList.get(i).totalValue;
                                    totalVolumeLeftAfterAcution = totalVolumeLeftAfterAcution - informMessageList.get(i).totalVolume;
                                    log = log + tempFreq;
                                    sellerInfo.sellingVol = sellerInfo.sellingVol - informMessageList.get(i).totalVolume;
                                    informMessageList.remove(i);
                                }
                            }
                        }
                        if(informMessageList.size() == 0){
                            myGui.displayUI(log);
                            myGui.displayUI("volume left: " + totalVolumeLeftAfterAcution + "   " + "value in total: " + totalValueSellAfterAuction);
                            //addBehaviour(new AdverInfoMsg());
                            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                            step = 4;
                            myAgent.doSuspend();

                        }
                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;

            }
        }
        public boolean done() {
            if (step == 4 && bidderReplyList.size() == 0) {
                myGui.displayUI("Do not buyer who provide the matching price.");
                myGui.displayUI("volume left: " + totalVolumeLeftAfterAcution + "   " + "value in total: " + totalValueSellAfterAuction);
                //addBehaviour(new AdverInfoMsg());
                //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                myAgent.doSuspend();
                takeDown();

                //myGui.dispose();
                //myGui.displayUI("Attempt failed: do not have bidder now" + "\n");
            }
            return step == 0;
            //return ((step == 2 && acceptedName == null) || step == 4) ;
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println(getAID().getName()+" terminating.");
    }

    // function to sort hashmap by values
    public class agentInfo {
        String agentName;
        String farmerName;
        Double farmSize;
        Double consentPrice;
        Double waterReq;
        Double totalProfitValue;
        Double totalCost;
        Double totalGrossMargin;
        Double pctReduction;
        Double waterReqAfterReduction;
        Double profitAfterReduction;
        String agentType;
        Double sellingVol;
        Double sellingPrice;

        agentInfo(String agentName, String farmerName, double farmSize, double consentPrice, double waterReq, double totalProfitValue, double totalCost, double totalGrossMargin, double pctReduction,
                  double waterReqAfterReduction, double profitAfterReduction, String agentType, double sellingVol, double sellingPrice) {
            this.agentName = agentName;
            this.farmerName = farmerName;
            this.farmSize = farmSize;
            this.consentPrice = consentPrice;
            this.waterReq = waterReq;
            this.totalProfitValue = totalProfitValue;
            this.totalCost = totalCost;
            this.totalGrossMargin = totalGrossMargin;
            this.pctReduction = pctReduction;
            this.waterReqAfterReduction = waterReqAfterReduction;
            this.profitAfterReduction = profitAfterReduction;
            this.agentType = agentType;
            this.sellingVol = sellingVol;
            this.sellingPrice = sellingPrice;
        }

        public String toString() {
            return "Seller Name: " + this.agentName + " " + "DB order: " + this.farmerName + "  " + "Buying Volume: " + df.format(this.sellingVol) + " " + "Price: " + this.sellingPrice + " Profit loss: " + (this.totalProfitValue - this.profitAfterReduction);
        }
    }

    //Sorted by volumn (descending).
    class SortbyValue implements Comparator<Agents> {
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b){
            return Double.compare(a.totalValue, b.totalValue);
        }
    }

    class SortbyTotalVol implements Comparator<Agents>{
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b){
            return Double.compare(a.totalVolume, b.totalVolume);
        }
    }

    //adding new class for sorted seller agent data.
    class Agents{
        //double varieVolume;
        //int fivehundredFeq;
        double totalVolume;
        double price;
        double totalValue;
        String dbName;
        String name;
        //Constructor
        public Agents(double totalVolume, double price, double totalValue, String dbName, String name){
            //this.varieVolume = varieVolume;
            //this.fivehundredFeq = fivehundredFeq;
            this.totalVolume = totalVolume;
            this.price = price;
            this.totalValue = totalValue;
            this.dbName = dbName;
            this.name = name;
        }
        public String toString(){
            //return this.name + " " + this.varieVolume + " " + this.fivehundredFeq + "  Total Volume: " + this.totalVolume + "  Total Value: " + this.totalValue + " Price: " + this.price;
            return this.name + "   " + "Total Volume: " + this.totalVolume + "  Total Value:  " + this.totalValue + " Price: " + this.price + " DB Name: " + this.dbName;
        }
    }

    //Writing output method.
    public void WriteToFile(String bidderDBList, String outputPath)
            throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, true));
        writer.newLine();
        writer.write(bidderDBList);
        writer.close();
    }
}