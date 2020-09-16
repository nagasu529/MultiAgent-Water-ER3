package agent;

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
import jade.util.leap.Collection;
import javafx.beans.binding.When;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;


import database.DatabaseConn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class MonitorNewAgent extends Agent {
    private MonitorNewAgentGui myGui;
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<agentInfoMornitor> resultList = new ArrayList<agentInfoMornitor>();

    //Summarizing results
    private int maxBiderAgents;
    private double maxBuyingVol;
    private double maxProfitLoss;
    private double maxPctProfitReduction;
    ArrayList<bidderCollection> bidderMornitor = new ArrayList<bidderCollection>();
    ArrayList<String> bidderNameDB = new ArrayList<String>();
    
    int redundantCnt = 0;
    DatabaseConn app = new DatabaseConn();
    
    protected void setup(){
        //Register Service.
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getAID().getName());
        sd.setType("mornitor");
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }
        //Create and show GUI
        myGui = new MonitorNewAgentGui(this);
        myGui.show();
        myGui.displayUI(getAID().getLocalName() + " Monitoring agent is active" + "\n");

        //addBehaviour(new ReceiveBidderInfo());

        //Adding the TickBehaviour for service monitoring.
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                addBehaviour(new AgentMornitoring());
                //addBehaviour(new SellerAdverInfo());
            }
        });
    }
    /***
    private class SellerAdverInfo extends CyclicBehaviour{
    	
    }***/

    private class AgentMornitoring extends Behaviour {
        //List of all agents in the environment.
        private AID[] bidderAgent;
        private MessageTemplate mt;
        private int repliesCnt = 0;
        private String bidderName;
        private double buyingVolumn;
        private double pricePerMM;
        private double profitBefore;
        private double profitAfter;
        private double totalProfitBefore;
        private double totalProfitAfter;
        private double totalProfitFinishedbiding;
        private double profitLostValue = 0;

        private double currentNumBidderAgent;
        private double currentBuyingVol;
        private double currentProfitLoss;
        private double currentPctProfitReduction;
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
                        bidderAgent = new AID[result.length];
                        int tempNumBidderAgent = bidderAgent.length;
                        if(tempNumBidderAgent > maxBiderAgents){
                            maxBiderAgents = tempNumBidderAgent;
                        }
                        currentNumBidderAgent = tempNumBidderAgent;
                        myGui.displayUI( "\n" + "Found acutioneer agents:" +"\n");
                        myGui.displayUI("Number of agent is " + bidderAgent.length + "\n");

                        for (int i = 0; i < currentNumBidderAgent; ++i) {
                            bidderAgent[i] = result[i].getName();
                            //myGui.displayUI(bidderAgent[i].getName() + "\n");
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    if(currentNumBidderAgent < maxBiderAgents){
                        step = 2;
                        break;
                    }

                    // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < currentNumBidderAgent; ++i) {
                        if (bidderAgent[i].getName().equals(getAID().getName()) == false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    step = 1;
                    //myGui.displayUI("\n" + step);
                    break;

                case 1:
                    //Receive all agent proposals.
                    ACLMessage reply = myAgent.receive(mt);
                    
                    if (reply != null) {
                        repliesCnt++;
                        //Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String bidderMessage = reply.getContent();
                            String[] arrOfStr = bidderMessage.split("-");
                            bidderName = arrOfStr[0];
                            buyingVolumn = Double.parseDouble(arrOfStr[1]);
                            pricePerMM = Double.parseDouble(arrOfStr[2]);
                            profitBefore = Double.parseDouble(arrOfStr[3]);
                            profitAfter = Double.parseDouble(arrOfStr[4]);
                            agentInfoMornitor xx = new agentInfoMornitor(bidderName, buyingVolumn, pricePerMM, profitBefore, profitAfter, 0, 0, 0);
                            myGui.displayUI(xx.farmerName + "  " + xx.buyingVolumn + "  " + xx.buyingPricePerMM + "\n");
                            maxBuyingVol = maxBuyingVol + buyingVolumn;
                            maxProfitLoss = maxProfitLoss + profitLostValue;
                            resultList.add(xx);
                            
                            totalProfitBefore = totalProfitBefore + profitBefore;
                            totalProfitAfter = totalProfitAfter + profitAfter;
                            
                            String tempString = reply.getSender().getLocalName() + "," + bidderName;
                            //String tempString = reply.getSender().getLocalName() + "," + bidderName + "," + profitBefore + "," + profitAfter;
                            bidderNameDB.add(tempString);

                            bidderMornitor.add(new bidderCollection(bidderName,buyingVolumn, pricePerMM, profitLostValue));
                        }
                        if (repliesCnt >= currentNumBidderAgent) {
                            //myGui.displayUI("\n" + "Max bidder number are " + currentNumBidderAgent + "\n");
                            myGui.displayUI("\n" + "Max bidder: " + maxBiderAgents + "     " + "Current biider no.: " +  currentNumBidderAgent + "\n");
                            myGui.displayUI("\n" + "Total bidder numbers is " + bidderMornitor.size() + "\n");
                            myGui.displayUI("Total water request from the groups of buyers:  " + df.format(maxBuyingVol) + "  " + df.format(((maxBuyingVol*100)/(maxBuyingVol))) + "%" + "\n");
                            myGui.displayUI("Total profit loss reduction from group of buyers:  " + df.format(100 - (100 * totalProfitAfter)/(totalProfitBefore)) + "\n");
                            
                            //Writing the all bidder result calculation side to file.
                            //output file location.
                            String outputFile = "/Users/nagasu/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/bidder.csv"; 		//Macbook
                            //String outputFile = "F:/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/bidder.csv"; 					//Home PC
                            //String outputFile = "C:/Users/chiewchk/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/bidder.csv";  	//Office
                            
                            Collections.sort(bidderNameDB);
                            bidderNameDB.add(String.valueOf(totalProfitBefore));
                            bidderNameDB.add(String.valueOf(totalProfitAfter));
                            myGui.displayUI(" bidder number :  " + bidderNameDB.size());
                            try {
        						WriteToFile(bidderNameDB.toString(), outputFile);
        					} catch (IOException e) {
        						// TODO Auto-generated catch block
        						e.printStackTrace();
        					}
        					
                            myGui.displayUI("Stop it!!!!!!");
                            myAgent.doSuspend();
                            
							
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    myGui.displayUI("\n" + "Max bidder: " + maxBiderAgents + "     " + "Current biider no.: " +  currentNumBidderAgent + "\n");
                    //Instant result value.
                    double currentTotalVolume = 0;
                    double currentProfitLostPct = 0;

                    for(int i = 0; i < bidderAgent.length; i++){
                        for(int j = 0; j <= bidderMornitor.size() - 1;j++){
                            if(bidderAgent[i].getLocalName().equals(bidderMornitor.get(j).name)){
                                currentTotalVolume = currentTotalVolume + bidderMornitor.get(j).buyingVolume;
                                currentProfitLostPct = currentProfitLostPct + bidderMornitor.get(j).pctProfitLoss;
                            }
                        }
                    }
                    myGui.displayUI("\n" + "Max bidder: " + maxBiderAgents + "     " + "Current biider no.: " +  currentNumBidderAgent + "\n");
                    myGui.displayUI("Total water request from the groups of buyers:  " + df.format(currentTotalVolume) + "  " + df.format(((currentTotalVolume*100)/(maxBuyingVol))) + "%" + "\n");
                    myGui.displayUI("Total profit loss from group of buyers:  " + df.format((100*currentProfitLostPct)/(maxProfitLoss)) + "\n");
                    
                    if(maxBiderAgents != currentNumBidderAgent){
                    	
                        redundantCnt++;
                    }
                    step = 3;
                    break;
                case 3:
                    if (bidderAgent.length > 0) {
                        step = 0;
                    } else {
                        step = 4;
                    }
                    if(redundantCnt == 3){
                    	////////////////////////////////////////////////////////////////////////////////////////
                        myAgent.doSuspend();
                    }
            }
        }

        public boolean done() {
            if (step == 4) {
                myGui.displayUI("Process is done" + "\n");
                myAgent.doSuspend();
            }
            return step == 0;
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

    class agentInfoMornitor{
        String farmerName;
        double buyingVolumn;
        double buyingPricePerMM;
        double profitBefore;
        double profitAfter;
        double sumVolumn;
        double sumPrice;
        double sumProfitPct;

        agentInfoMornitor(String farmerName, double buyingVolumn, double buyingPricePerMM, double profitBefore, double profitAfter, double sumVolumn, double sumPrice, double sumProfitPct){
            this.farmerName = farmerName;
            this.buyingVolumn = buyingVolumn;
            this.buyingPricePerMM = buyingPricePerMM;
            this.profitBefore = profitBefore;
            this.profitAfter = profitAfter;
            this.sumVolumn = sumVolumn;
            this.sumPrice = sumPrice;
            this.sumProfitPct = sumProfitPct;
        }
    }

    class bidderCollection{
        String name;
        double buyingVolume;
        double buyingPrice;
        double pctProfitLoss;

        bidderCollection(String name, double buyingVolume, double buyingPrice, double pctProfitLoss){
            this.name = name;
            this.buyingVolume = buyingVolume;
            this.buyingPrice = buyingPrice;
            this.pctProfitLoss = pctProfitLoss;
        }
        public String toString(){
            return this.name + "  " + this.buyingVolume + "  " + this.buyingPrice + "  " + this.pctProfitLoss;
        }
    }
    
    public void WriteToFile(String bidderDBList, String outputPath) 
    		  throws IOException {
    		    BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, true));
    		    writer.newLine();
    		    writer.write(bidderDBList);
    		    writer.close();
    		}
}
