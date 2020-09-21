package agent;

import calcAuction.FileInputValidation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import calcAuction.FileInput;
import database.DatabaseConn;


import java.util.*;

public class randValSealVarieSeller extends Agent {
    randValSealVarieSellerGui myGui;
    agentInfo sellerInfo;
    DatabaseConn app = new DatabaseConn();
    ArrayList<catalogInfo> sellingCatalog = new ArrayList<catalogInfo>();

    //General parameters information
    DecimalFormat df = new DecimalFormat("#.##");
    FileInputValidation randValue = new FileInputValidation();
    String log = "";
    int informCnt = 0;
    //int cfpCnt;

    String outputFile;




    protected void setup(){
        // Create and show the GUI

        //Selected data from databased
        //Randomised
        //app.selectSellerRandom();

        //Fixed
        app.selectSeller(getLocalName());
        
        sellerInfo = new agentInfo(getLocalName(), app.Name, app.FarmSize, app.ConsentPrice, app.WaterReq, app.TotalProfitValue, app.TotalCost, app.TotalFarmGrossMargin, app.PctReduction,
                app.WaterReqAfterReduction, app.ProfitAfterReduction, app.SellingVol, app.SellingPrice);

        //Spliting the selling volume to two groups based on total selling volume.
        if(sellerInfo.sellingVol <= 500){
            sellingCatalog.add(new catalogInfo(getLocalName(),sellerInfo.dbName,200,"none","none",0.0,0.0));
            sellingCatalog.add(new catalogInfo(getLocalName(),sellerInfo.dbName,sellerInfo.sellingVol - 200,"none","none",0.0,0.0));
        }else if (sellerInfo.sellingVol > 500 && sellerInfo.sellingVol < 800){
            sellingCatalog.add(new catalogInfo(getLocalName(),sellerInfo.dbName,350, "none","none",0.0,0.0));
            sellingCatalog.add(new catalogInfo(getLocalName(),sellerInfo.dbName,sellerInfo.sellingVol - 350,"none", "none",0.0,0.0));
        }else {
            sellingCatalog.add(new catalogInfo(getLocalName(),sellerInfo.dbName,500, "none", "none", 0.0, 0.0));
            sellingCatalog.add(new catalogInfo(getLocalName(),sellerInfo.dbName, sellerInfo.sellingVol - 500,"none", "none", 0.0,0.0));
        }

        /***
        //Selling volume spited by conditions (each group is not over 500 mm^3).
        //adding the splitting volume to catalog.

        double tempNumSellingCatalogItem = sellerInfo.sellingVol/500;
        if(tempNumSellingCatalogItem < 1) {
            sellingCatalog.add(new catalogInfo(0, sellerInfo.sellingVol, "none","none", 0.0, 0.0));
        }else {
            int fiveHundredVolFreq = (int)(tempNumSellingCatalogItem);
            double varieVol = sellerInfo.sellingVol - (fiveHundredVolFreq * 500);
            sellingCatalog.add(new catalogInfo(0, varieVol,"none", "none", 0.0, 0.0));
            int i  = 0;
            while (i < fiveHundredVolFreq) {
                sellingCatalog.add(new catalogInfo((i + 1), 500.00, "none", "none", 0.0, 0.0));
                i++;
            }

        }
         ***/

        myGui = new randValSealVarieSellerGui(this);
        myGui.show();

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

        System.out.println(sellerInfo.farmerName + "  is ready" + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 10000){
            protected void onTick() {
                myGui.displayUI("Name: " + sellerInfo.farmerName + "\n");
                myGui.displayUI("Volumn to sell: " + sellerInfo.sellingVol + "\n");
                myGui.displayUI("First Volume: " + sellingCatalog.get(0).toString() + "\n");
                myGui.displayUI("Last Volume: " + sellingCatalog.get(1).toString() + "\n");
                myGui.displayUI("Selling price: " + sellerInfo.sellingPrice + "\n");
                myGui.displayUI("\n");
                for (int i = 0; i <= sellingCatalog.size() - 1; i++) {
                    myGui.displayUI(sellingCatalog.get(i).toString());

                }

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

    private class RequestPerformer extends Behaviour {
        //The list of known water selling agent
        private AID[] bidderAgent;
        //private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        int replyCnt;
        int proposeCnt;
        int refuseCnt;


        //List of reply instance.
        //ArrayList<Agents> bidderReplyList = new ArrayList<>();
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
                        //myGui.displayUI("Found auctioneer agents:\n");
                        bidderAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgent[i] = result[i].getName();
                            myGui.displayUI(bidderAgent[i].getName()+ "\n");
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    //adding replyCnt number.
                    //replyCnt = sellingCatalog.size() * bidderAgent.length;

                    // Send the cfp to all sellers (Sending water volume required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgent.length; ++i) {
                        if (bidderAgent[i].getName().equals(sellerInfo.farmerName)== false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    //cfp.setContent(String.valueOf(Double.toString(sellerInfo.sellingVolumn) + "-" + Double.toString((sellerInfo.sellingPrice))));
                    cfp.setContent((sellingCatalog.get(0).sellerDBName + "-" + sellingCatalog.get(0).volume) + "-" + sellingCatalog.get(1).volume);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);

                    /***
                    cfpCnt++;
                    if(cfpCnt <= 1) {
                        myAgent.send(cfp);
                    }
                     ***/



                    //myGui.displayUI(cfp.toString());
                    //myGui.displayUI(cfp.toString());
                    //System.out.println("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    //mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    //Adding time counter for closing bided auction.

                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    //ACLMessage reply = myAgent.receive(mt);
                    //mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        replyCnt++;
                        //myGui.displayUI(reply.toString() + "\n" + "replyCnt =" + replyCnt + "\n");
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            proposeCnt++;
                            myGui.displayUI("Receive message: \n" + reply + "\n");
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            String tempBidderName = reply.getSender().getLocalName();
                            String tempNameDB = arrOfStr[0];
                            double tempBidVol = Double.parseDouble(arrOfStr[1]);
                            double tempBidPrice = Double.parseDouble(arrOfStr[2]);

                            //adding data to dictionary, comparing and storing data.
                            for (int i = 0; i <= sellingCatalog.size() - 1; i++){
                                if(tempBidVol == sellingCatalog.get(i).volume && sellingCatalog.get(i).winnerPrice < tempBidPrice){
                                    sellingCatalog.get(i).winnerPrice = tempBidPrice;
                                    sellingCatalog.get(i).winnerName = tempBidderName;
                                    sellingCatalog.get(i).winnerDBName = tempNameDB;
                                    sellingCatalog.get(i).winnerVol = tempBidVol;
                                }
                            }
                        }else {
                            refuseCnt++;
                        }

                        myGui.displayUI("\n" + "Verified result : >>>>>>>>>>>>>>>>>>> \n");
                        myGui.displayUI("Reply msg: " + replyCnt + "  " + "PROPOSE msg : " + proposeCnt + "   " + "REFUSE msg: " + refuseCnt + "\n");
                        //myGui.displayUI("\n" + "bidder reply size:  " + (bidderReplyList.size()) + "\n" + "bidder reply list:" + "\n");
                        myGui.displayUI("Starting to process: \n");

                        if ((replyCnt >= sellingCatalog.size() * bidderAgent.length)) {
                            //Collections.sort(sellingCatalog, new SortbyValue());
                            //Collections.reverse(bidderReplyList);

                            myGui.displayUI("\n" + "Verified result : >>>>>>>>>>>>>>>>>>> \n");
                            myGui.displayUI("Reply msg: " + replyCnt + "  " + "PROPOSE msg : " + proposeCnt + "   " + "REFUSE msg: " + refuseCnt + "\n");
                            //myGui.displayUI("\n" + "bidder reply size:  " + (bidderReplyList.size()) + "\n" + "bidder reply list:" + "\n");
                            for (int i = 0; i <= sellingCatalog.size() - 1; i++) {
                                myGui.displayUI("sssssss  " + sellingCatalog.get(i).toString() + "\n");
                            }
                            myGui.displayUI("\n");
                            step = 2;
                        }
                    }else {
                        block();
                    }

                    break;
                        /***
                        //calculated result
                        while (bidderReplyList.size() > 0) {
                            if(bidderReplyList.get(0).buyingPrice > sellingCatalog.get(0).winnerPrice){
                                sellingCatalog.get(0).winnerPrice = bidderReplyList.get(0).buyingPrice;
                                sellingCatalog.get(0).winnerName = bidderReplyList.get(0).name;
                                sellingCatalog.get(0).winnerDBName = bidderReplyList.get(0).nameDB;
                                bidderReplyList.remove(0);
                            }else if(bidderReplyList.get(0).buyingPrice > sellingCatalog.get(1).winnerPrice){
                                sellingCatalog.get(0).winnerPrice = bidderReplyList.get(1).buyingPrice;
                                sellingCatalog.get(0).winnerName = bidderReplyList.get(0).name;
                                sellingCatalog.get(0).winnerDBName = bidderReplyList.get(0).nameDB;
                                bidderReplyList.remove(0);
                            }
                        }
                         ***/
                case 2:
                    /*
                     * Calculating and adding accepted water volume for bidder based on highest price.
                     * Sending message to bidders with two types (Accept proposal or Refuse) based on
                     * accepted water volume to sell.
                     */

                    //Sorted propose message and matching to reply INFORM Message.

                    /***
                    while (bidderReplyList.size() > 0){
                        if((bidderReplyList.get(0).buyingVol == sellingCatalog.get(0).volume) && (bidderReplyList.get(0).buyingPrice > sellingCatalog.get(0).winnerPrice)){
                            sellingCatalog.get(0).winnerPrice = bidderReplyList.get(0).buyingPrice;
                            sellingCatalog.get(0).winnerName = bidderReplyList.get(0).name;
                            sellingCatalog.get(0).winnerDBName = bidderReplyList.get(0).nameDB;
                            bidderReplyList.remove(0);
                        }else if((bidderReplyList.get(0).buyingVol == sellingCatalog.get(1).volume) && (bidderReplyList.get(0).buyingPrice > sellingCatalog.get(1).winnerPrice)){
                            sellingCatalog.get(0).winnerPrice = bidderReplyList.get(1).buyingPrice;
                            sellingCatalog.get(0).winnerName = bidderReplyList.get(1).name;
                            sellingCatalog.get(0).winnerDBName = bidderReplyList.get(1).nameDB;
                            bidderReplyList.remove(0);
                        }
                    }
                     ***/

                    //Verified result.
                    myGui.displayUI("Verified auction. >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + "\n");
                    String logAccept = "ACCEPT PROPOSAL Message:" + "\n";
                    String logReject = "REJECT PROPOSAL Message:" + "\n";
                    ArrayList<String> output = new ArrayList<String>();
                    output.add(sellerInfo.farmerName + "," + sellerInfo.dbName + "," + sellerInfo.sellingVol);

                    for(int i = 0; i <= sellingCatalog.size() - 1 ;i++){
                        String tempOutput = "";
                        if(sellingCatalog.get(i).winnerName.equals("none")) {
                            logReject = logReject + sellingCatalog.get(i).toString() + "\n";
                        }else {
                            logAccept = logAccept + sellingCatalog.get(i).toString() + "\n";
                        }
                        tempOutput = tempOutput + (i + "," + sellingCatalog.get(i).winnerName +"," + sellingCatalog.get(i).winnerDBName +
                                "," + sellingCatalog.get(i).winnerVol +","+ sellingCatalog.get(i).winnerPrice);
                        output.add(tempOutput);
                    }

                    myGui.displayUI(logAccept + "\n");
                    myGui.displayUI(logReject + "\n");
                    myGui.displayUI(output.toString());

                    //output file location.
                    //String outputFile = "/Users/nagasu/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 		//Macbook
                    String outputFile = "F:/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 	//Home PC
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



                    //Sending PROPOSE message to Seller (only the best option for volume requirement.

                    for (int i = 0; i <= bidderAgent.length - 1; i++) {
                        for (int j = 0; j <= sellingCatalog.size() - 1; j++) {
                            if (bidderAgent[i].getLocalName().equals(sellingCatalog.get(j).winnerName)) {
                                ACLMessage replyToBidder = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                replyToBidder.setContent(getLocalName()  + "-" + sellingCatalog.get(j).winnerVol + "-" + sellingCatalog.get(j).winnerPrice);
                                replyToBidder.setConversationId("bidding");
                                replyToBidder.setReplyWith("reply" + System.currentTimeMillis());
                                replyToBidder.addReceiver(bidderAgent[i]);
                                myAgent.send(replyToBidder);
                                informCnt++;
                                //myGui.displayUI(replyToBidder.toString() + "\n");
                            }else {
                                ACLMessage replyToBidder = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                replyToBidder.setContent(getLocalName() + "-" + sellingCatalog.get(j).winnerVol + "-" + sellingCatalog.get(j).winnerPrice);
                                replyToBidder.setConversationId("bidding");
                                replyToBidder.setReplyWith("reply" + System.currentTimeMillis());
                                replyToBidder.addReceiver(bidderAgent[i]);
                                myAgent.send(replyToBidder);
                                //myGui.displayUI(replyToBidder.toString() + "\n");
                            }
                        }
                    }

                    step = 4;
                    break;

            }
        }
        private void While(boolean b) {
            // TODO Auto-generated method stub

        }
        public boolean done() {
            if (step == 4 && informCnt == 0) {
                //myGui.displayUI("Do not buyer who provide the matching price.");
                myGui.displayUI("Do not have any offer for this round" + "\n");
                myAgent.doSuspend();
                takeDown();

                //myGui.dispose();
                //myGui.displayUI("Attempt failed: do not have bidder now" + "\n");
            }
            return step == 4;
            //return ((step == 2 && acceptedName == null) || step == 4) ;
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                myGui.displayUI("\n" + getAID().getLocalName() + " finished aution for selling water to " + msg.getSender().getLocalName() + "\n");
                informCnt--;
                if (informCnt == 0){
                    myGui.displayUI(getLocalName() + " terminating.");
                    myAgent.doSuspend();
                }
            }else {

                block();
            }
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

    public class catalogInfo{
        String sellerName;
        String sellerDBName;
        double volume;
        String winnerName;
        String winnerDBName;
        double winnerVol;
        double winnerPrice;
        catalogInfo(String sellerName, String sellerDBName, double volume, String winnerName, String winnerDBName, double winnerVol, double winnerPrice){
            this.sellerDBName = sellerDBName;
            this.sellerName = sellerName;
            this.volume = volume;
            this.winnerName = winnerName;
            this.winnerDBName = winnerDBName;
            this.winnerVol = winnerVol;
            this.winnerPrice = winnerPrice;
        }
        public String toString() {
            return "Seller name: " + this.sellerName + "  DB name: " + this.sellerDBName + "  Vol" + this.volume + "   Winner: " + this.winnerName + "  DB Name: " + this.winnerDBName +
                    "  Vol: " + this.winnerVol + "  Price: " + this.winnerPrice + "\n";
        }
    }

    public class agentInfo{
        String farmerName;
        String dbName;
        double farmSize;
        double consentPrice;
        double waterReq;
        double totalProfitValue;
        double totalCost;
        double totalFarmGrossMargin;
        double pctReduction;
        double waterReqAfterReduction;
        double profitAfterReduction;
        double sellingVol;
        double sellingPrice;

        agentInfo(String farmerName, String dbName, double farmSize, double consentPrice, double waterReq, double totalProfitValue, double totalCost, double totalFarmGrossmargin,
                  double pctReduction, double waterReqAfterReduction, double profitAfterReduction, double sellingVol, double sellingPrice){
            this.farmerName = farmerName;
            this.dbName = dbName;
            this.consentPrice = consentPrice;
            this.waterReq = waterReq;
            this.totalProfitValue = totalProfitValue;
            this.totalCost = totalCost;
            this.totalFarmGrossMargin = totalFarmGrossmargin;
            this.pctReduction = pctReduction;
            this.waterReqAfterReduction = waterReqAfterReduction;
            this.profitAfterReduction = profitAfterReduction;
            this.sellingVol = sellingVol;
            this.sellingPrice = sellingPrice;
        }
        public String toString() {
            return "Seller Name: " + this.farmerName + " " + "DB order: " + this.dbName + "  " + "Selling Volume: " + df.format(this.sellingVol) + " " + "Price: " + this.sellingPrice + " Profit loss: " + (this.totalProfitValue - this.profitAfterReduction);
        }
    }


    //adding new class for sorted seller agent data.
    class Agents{
        String name;
        String nameDB;
        int catalogKey;
        double buyingVol;
        double buyingPrice;
        //Constructor
        public Agents(String name, String nameDB, int catalogKey, double buyingVol, double buyingPrice){
            this.name = name;
            this.nameDB = nameDB;
            this.catalogKey = catalogKey;
            this.buyingVol = buyingVol;
            this.buyingPrice = buyingPrice;
        }
        public String toString(){
            return this.name + "  DB Name is: " + this.nameDB +  "  bided for catalog key: " + this.catalogKey + "  Buying Volume: " + df.format(this.buyingVol) + "  Price: " + df.format(this.buyingPrice);
        }
    }

    //Sorted by volume (descending).
    class SortbyValue implements Comparator<Agents> {
        //Used for sorting in ascending order of the volume.
        public int compare(Agents a, Agents b){
            return Double.compare(a.buyingPrice, b.buyingPrice);
        }
    }

    class SortbyDictOreer implements Comparator<Agents>{
        public int compare(Agents a, Agents b) {
            return Integer.compare(a.catalogKey, b.catalogKey);
        }
    }

    //Adding writing method and outPut class implementation.
    public class outputImplementation{
        String sellerName;
        double sellingVol;
        double sellingPrice;
        String firstWinnerName;
        double firstWinnerVol;
        double firstWinnerPrice;
        String secondWinnerName;
        double secondWinnerVol;
        double secondWinnerPrice;
        String thirdWinnerName;
        double thirdWinnerVol;
        double thirdWinnerPrice;
        String forthWinnerName;
        double forthWinnerVol;
        double forthWinnerPrice;
        String fifthWinnerName;
        double fifthWinnerVol;
        double fifthWinnerPrice;

        public outputImplementation(String sellerName, double sellingVol, double sellingPrice, String firstWinnerName, double firstWinnerVol, double firstWinnerPrice, String secondWinnerName, double secondWinnerVol, double secondWinnerPrice, String thirdWinnerName,
                                    double thirdWinnerVol, double thirdWinnerPrice, String forthWinnerName, double forthWinnerVol, double forthWinnerPrice, String fifthWinnerName, double fifthWinnerVol, double fifthWinnerPrice) {
            this.sellerName = sellerName;
            this.sellingVol = sellingVol;
            this.sellingPrice = sellingPrice;
            this.firstWinnerName = firstWinnerName;
            this.firstWinnerVol = firstWinnerVol;
            this.firstWinnerPrice = firstWinnerPrice;
            this.secondWinnerName = secondWinnerName;
            this.secondWinnerVol = secondWinnerVol;
            this.secondWinnerPrice = secondWinnerPrice;
            this.thirdWinnerName = thirdWinnerName;
            this.thirdWinnerVol = thirdWinnerVol;
            this.thirdWinnerPrice = thirdWinnerPrice;
            this.forthWinnerName = forthWinnerName;
            this.forthWinnerVol = forthWinnerVol;
            this.forthWinnerPrice = forthWinnerPrice;
            this.fifthWinnerName = fifthWinnerName;
            this.fifthWinnerVol = fifthWinnerVol;
            this.fifthWinnerPrice = fifthWinnerPrice;
            this.fifthWinnerVol = fifthWinnerVol;
            this.fifthWinnerPrice = fifthWinnerPrice;
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