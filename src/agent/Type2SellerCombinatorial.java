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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import calcAuction.FileInput;
import database.DatabaseConn;



public class Type2SellerCombinatorial extends Agent{
    Type2SellerCombinatorialGui myGUI;
    
    //General parameters preparation.
    private double totalValueSell;
    DecimalFormat df = new DecimalFormat("###,###.##");
    private int decisionRule;
    FileInput randValue = new FileInput();
    agentInfo farmerInfo;
    DatabaseConn app = new DatabaseConn();
    int countTick;
    int decisionRules = 0;

    int informCnt = 0;
    ArrayList<Agents> replyInfoList = new ArrayList<Agents>();
    ArrayList<String> acceptNameList = new ArrayList<String>();
    ArrayList<String> output = new ArrayList<String>();
    Random r = new Random();
    int biddingBehaviourRule = getRandomNumberInRange(0,2);
    //int biddingBehaviourRule = 2;
    
    //Selling capacity (Maximum).
    double bestResult = 0.0;
	String bestResultArray = "";
	

    //Setting up and starting agent.
    protected void setup(){
        // Create and show the GUI
    	app.selectSeller(getLocalName());
        myGUI = new Type2SellerCombinatorialGui(this);
        myGUI.show();
        System.out.println(getAID().getLocalName() + " is ready");
        farmerInfo = new agentInfo(getLocalName(), app.Name, app.FarmSize, app.ConsentPrice, app.WaterReq, app.TotalProfitValue, app.TotalCost, app.TotalFarmGrossMargin, 
        		app.PctReduction, app.WaterReqAfterReduction, app.ProfitAfterReduction, app.SellingVol, app.SellingPrice);


        //Start agent
        DFAgentDescription dfd = new DFAgentDescription();
        farmerInfo.farmerName = getAID().getLocalName();
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

        System.out.println(farmerInfo.farmerName + "  is ready" + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 15000){
            protected void onTick() {
                myGUI.displayUI("Name: " + farmerInfo.farmerName + "\n");
                //myGUI.displayUI("Status: " + farmerInfo.agentType + "\n");
                myGUI.displayUI("Volumn to sell: " + farmerInfo.sellingVol + "\n");
                myGUI.displayUI("Selling price: " + farmerInfo.sellingPrice + "\n");
                myGUI.displayUI("\n");

                // Add the behaviour serving purchase orders from buyer agents
                addBehaviour(new PurchaseOrdersServer());

                /*
                 ** Selling water process
                 */
                if(biddingBehaviourRule == 0){
                    addBehaviour(new GenerousBehaviour());
                    myGUI.displayUI("Bidding behaviour is Generous Behaviour" + "\n");
                }else if(biddingBehaviourRule == 1){
                    addBehaviour(new NeutralBehaviour());
                    myGUI.displayUI("Bidding behaviour is Neutral Behaviour" + "\n");
                }else {
                    addBehaviour(new CovetousBehaviour());
                    myGUI.displayUI("Bidding behaviour is Covetous Behaviour" + "\n");
                }
            }
        } );
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            //String xx = getLocalName() + "," + farmerInfo.dbName + "," + bestResult;
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                myGUI.displayUI("\n" + getAID().getLocalName() + "accepted to buy water from" + msg.getSender().getLocalName());
                informCnt--;
                if (informCnt == 0){
                    myGUI.displayUI("Stop it!!!!!!");
                    myGUI.displayUI("\n");
                	takeDown();
                }
            }else {
                block();
            }
        }
    }


    private class NeutralBehaviour extends Behaviour {
        //The list of known water selling agent
        private AID[] bidderAgent;
        private int refuseCnt;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        ArrayList<String> bidderList = new ArrayList<String>();  //sorted list follows maximum price factor.
        //ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.

        //Creating dictionary for buyer volume and pricing
        Dictionary<String, Double> volumnDict = new Hashtable<String, Double>();
        Dictionary<String, Double> priceDict = new Hashtable<String, Double>();
        Dictionary<String, Double> profitLossDict = new Hashtable<>();
        

        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    //update bidder list
                	String serviceName = "bidder";
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(serviceName);
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
                    String tempSellingVol = String.valueOf(farmerInfo.sellingVol);
                    cfp.setContent(tempSellingVol);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    
                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                    	repliesCnt++;
                        //myGUI.displayUI(reply.toString());
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        	
                            System.out.println("Receive message: " + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            String tempName = reply.getSender().getLocalName();
                            double tempVolume = Double.parseDouble(arrOfStr[0]);
                            double tempPrice = Double.parseDouble(arrOfStr[1]);
                            double tempProfitLossValue = Double.parseDouble(arrOfStr[2]);
                            String tempNameDB = arrOfStr[3];
                            int tempPctPriceReduce = Integer.parseInt(arrOfStr[4]);
                            double tempValue = tempVolume * tempPrice;
                            //Normal combinatorial.
                            if(tempPrice > farmerInfo.sellingPrice) {
                            	replyInfoList.add(new Agents(tempVolume, tempPrice, tempValue, tempProfitLossValue, tempName, tempNameDB, "none", tempPctPriceReduce));
                                //adding data to dictionary
                                volumnDict.put(reply.getSender().getLocalName(),tempVolume);
                                priceDict.put(reply.getSender().getLocalName(),tempPrice);
                                profitLossDict.put(reply.getSender().getLocalName(), tempProfitLossValue);
                            }
                            
                            //Combinatorial type 2 (Seeker behavior)
                        }
                        
                        if (repliesCnt >= bidderAgent.length) {

                            //Show reply list (all bidders for this round).
                            myGUI.displayUI("Bidder reply for this stage:" + "\n");
                            myGUI.displayUI("\n" + getAID().getLocalName() + "    voldict  " + volumnDict.size() + "   price dict  " + priceDict.size());
                            for (int i = 0; i <= replyInfoList.size() - 1;i++){
                                myGUI.displayUI(replyInfoList.get(i).toString() + "\n");
                            }

                            // We received all replies
                            for(Enumeration e = volumnDict.keys(); e.hasMoreElements();){
                                String temp = e.nextElement().toString();
                                bidderList.add(temp);
                            }
                            String[] tempBidderList = GetStringArray(bidderList);
                            //Collections.reverse(Arrays.asList(tempBidderList));
                            
                            int maxBinaryNum = (int) Math.pow(2, tempBidderList.length);
                            myGUI.displayUI("Maximum round calculation: " + maxBinaryNum);
                            //Initialize result constructors.
                        	//Combinatorial process.
                        	while(maxBinaryNum > 1) {
                        		String tempResult = binaryToVale(maxBinaryNum -1, tempBidderList);
                        		double roundResult = 0.0;
                        		double roundVolume = 0.0;
                        		String[] arrOftempResult = tempResult.split(" ");
                        		int lenArrOfTempResult = arrOftempResult.length;
                        		while (lenArrOfTempResult >= 1) {
                        			double volFromDict = volumnDict.get(arrOftempResult[lenArrOfTempResult -1]);
                        			double priceFromDict = priceDict.get(arrOftempResult[lenArrOfTempResult -1]);
                        			double valueFromDict = priceFromDict * volFromDict;
                        			roundResult = roundResult + valueFromDict;
                        			roundVolume = roundVolume + volFromDict;
                        			//System.out.println(arrOftempResult[lenArrOfTempResult -1] + "\n");
                        			lenArrOfTempResult--;
                        		}
                        		//System.out.println("total value from this decimal orders: " + roundResult + " " + tempResult);
                        		
                        		
                        		if(roundResult > bestResult && roundVolume < farmerInfo.sellingVol) {
                        			bestResult = roundResult;
                        			bestResultArray = Arrays.deepToString(arrOftempResult);
                        		}
                        		maxBinaryNum--;
                        	}
                        	
                        	myGUI.displayUI("Best offer is: " + bestResultArray + " " + bestResult);
                        	
                        	//Adding information data for winner.
                        	bestResultArray = bestResultArray.replace(" ", "");
                        	bestResultArray = bestResultArray.replace("[", "");
                        	bestResultArray = bestResultArray.replace("]", "");
                            String[] tempAcceptName = bestResultArray.split(",");
                            for (int i = 0; i < tempAcceptName.length; i++) {
                            	for (int j = 0; j <=replyInfoList.size() - 1; j++){
                            		if(tempAcceptName[i].equals(replyInfoList.get(j).name)){
                            			replyInfoList.get(j).status = "accept";
                            		}
                            		myGUI.displayUI("\n" + getLocalName() + "  " + replyInfoList.get(j).name + "  " + replyInfoList.get(j).status );
                            	}
        					}
                            
                            step = 2;
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    /*
                     * Calculating and adding accepted water volume for bidder based on highest price.
                     * Sending message to bidders with two types (Accept proposal or Refuse) based on
                     * accepted water volume to sell.
                     */

                	String tempBehaviour= "";
                	if(biddingBehaviourRule == 0){
                	    tempBehaviour = "Generous";
                    }else if(biddingBehaviourRule == 1){
                	    tempBehaviour = "Neutral";
                    }else {
                	    tempBehaviour = "Covetous";
                    }


                	//Result Preparison.
                	String writingFileResult = getLocalName() + "," + farmerInfo.dbName + "," + tempBehaviour + "," + bestResult;
                    //String writingFileResult = getLocalName() + "," + farmerInfo.dbName + "," + bestResult;
                	int writCnt = 0;
                	
                	for (int i = 0; i <= replyInfoList.size() - 1; i++) {
                		myGUI.displayUI("ertyuhygtrfr" + replyInfoList.get(i).status + "\n");
						if(replyInfoList.get(i).status.equals("accept")) {
							writingFileResult = writingFileResult + "," + replyInfoList.get(i).name + "," + replyInfoList.get(i).nameDB +
									"," + replyInfoList.get(i).totalVolume + "," + replyInfoList.get(i).price + "," + replyInfoList.get(i).pctPriceReduce;
							writCnt++;
						}
						
					}
                	if(writCnt == 0) {
                		writingFileResult = writingFileResult + "," + 0.0;
                		output.add(writingFileResult);
                	}else {
						output.add(writingFileResult);
					}
                	
                    //Sending ACCEPT_PROPOSAL to winner.
                    for (int i = 0; i <= replyInfoList.size() -1; i++) {
                    	for (int j = 0; j < bidderAgent.length; j++) {
							if(bidderAgent[j].getLocalName().equals(replyInfoList.get(i).name) && replyInfoList.get(i).status=="accept"){
								// Send the purchase order to the seller that provided the best offer
                                ACLMessage acceptedRequest = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                acceptedRequest.addReceiver(bidderAgent[j]);
                                acceptedRequest.setConversationId("bidding");
                                acceptedRequest.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                myAgent.send(acceptedRequest);
                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo
                                        (acceptedRequest.getReplyWith()));
                                //myGUI.displayUI(acceptedRequest.toString() + "\n");
							}else if (bidderAgent[j].getLocalName().equals(replyInfoList.get(i).name)) {
								ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                rejectMsg.addReceiver(bidderAgent[i]);
                                rejectMsg.setConversationId("bidding");
                                rejectMsg.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                myAgent.send(rejectMsg);
							}
						}
                    }
                    
                    informCnt = acceptNameList.size();
                    
                    myGUI.displayUI( "\nssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss" + output.toString() + "\n");
                    
                    //Writing the all bidder result calculation side to file.
                    //output file location.
                    //String outputFile = "/Users/kitti.ch/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 		//Macbook
                    String outputFile = "F:/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 	//Home PC
                    //String outputFile = "C:/Users/chiewchk/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv";  	//Office
                        
                    try {
                    	WriteToFile(output.toString(), outputFile);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}	
                    myGUI.displayUI("Stop it!!!!!!");
                    myAgent.doSuspend();
                    
                    step = 3;
                    
                    break;
            }

        }
        
		public boolean done() {
        	 if (step == 3 && volumnDict.size() == 0) {
        	//if (step == 3 && maxEuList.size() == 0) {
                myGUI.displayUI("Do not buyer who provide the matching price");
                takeDown();

            }
            return step == 3 ;		//Normal is 0
        }
    }

    private class GenerousBehaviour extends Behaviour {
        //The list of known water selling agent
        private AID[] bidderAgent;
        private int refuseCnt;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        ArrayList<String> bidderList = new ArrayList<String>();  //sorted list follows maximum price factor.
        //ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.

        //Creating dictionary for buyer volume and pricing
        Dictionary<String, Double> volumnDict = new Hashtable<String, Double>();
        Dictionary<String, Double> priceDict = new Hashtable<String, Double>();
        Dictionary<String, Double> profitLossDict = new Hashtable<>();
        

        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    //update bidder list
                	String serviceName = "bidder";
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(serviceName);
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
                    String tempSellingVol = String.valueOf(farmerInfo.sellingVol);
                    cfp.setContent(tempSellingVol);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    
                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                    	repliesCnt++;
                        //myGUI.displayUI(reply.toString());
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        	
                            System.out.println("Receive message: " + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            String tempName = reply.getSender().getLocalName();
                            double tempVolume = Double.parseDouble(arrOfStr[0]);
                            double tempPrice = Double.parseDouble(arrOfStr[1]);
                            double tempProfitLossValue = Double.parseDouble(arrOfStr[2]);
                            String tempNameDB = arrOfStr[3];
                            int pctPriceReduce = Integer.parseInt(arrOfStr[4]);
                            double tempValue = tempVolume * tempPrice;
                            
                            //Generous Behaviour - needing to sharing water to others (do not set the reserved price)
                            
                            replyInfoList.add(new Agents(tempVolume, tempPrice, tempValue, tempProfitLossValue, tempName, tempNameDB, "none", pctPriceReduce));
                            //adding data to dictionary
                            volumnDict.put(reply.getSender().getLocalName(),tempVolume);
                            priceDict.put(reply.getSender().getLocalName(),tempPrice);
                            profitLossDict.put(reply.getSender().getLocalName(), tempProfitLossValue);
                            

                        }
                        
                        if (repliesCnt >= bidderAgent.length) {

                            //Show reply list (all bidders for this round).
                            myGUI.displayUI("Bidder reply for this stage:" + "\n");
                            myGUI.displayUI("\n" + getAID().getLocalName() + "    voldict  " + volumnDict.size() + "   price dict  " + priceDict.size());
                            for (int i = 0; i <= replyInfoList.size() - 1;i++){
                                myGUI.displayUI(replyInfoList.get(i).toString() + "\n");
                            }

                            // We received all replies
                            for(Enumeration e = volumnDict.keys(); e.hasMoreElements();){
                                String temp = e.nextElement().toString();
                                bidderList.add(temp);
                            }
                            String[] tempBidderList = GetStringArray(bidderList);

                            
                            int maxBinaryNum = (int) Math.pow(2, tempBidderList.length);
                            myGUI.displayUI("Maximum round calculation: " + maxBinaryNum);
                            //Initialize result constructors.
                        	//Combinatorial process.
                        	while(maxBinaryNum > 1) {
                        		String tempResult = binaryToVale(maxBinaryNum -1, tempBidderList);
                        		double roundResult = 0.0;
                        		double roundVolume = 0.0;
                        		String[] arrOftempResult = tempResult.split(" ");
                        		int lenArrOfTempResult = arrOftempResult.length;
                        		while (lenArrOfTempResult >= 1) {
                        			double volFromDict = volumnDict.get(arrOftempResult[lenArrOfTempResult -1]);
                        			double priceFromDict = priceDict.get(arrOftempResult[lenArrOfTempResult -1]);
                        			double valueFromDict = priceFromDict * volFromDict;
                        			roundResult = roundResult + valueFromDict;
                        			roundVolume = roundVolume + volFromDict;
                        			//System.out.println(arrOftempResult[lenArrOfTempResult -1] + "\n");
                        			lenArrOfTempResult--;
                        		}
                        		//System.out.println("total value from this decimal orders: " + roundResult + " " + tempResult);
                        		
                        		
                        		if(roundResult > bestResult && roundVolume < farmerInfo.sellingVol) {
                        			bestResult = roundResult;
                        			bestResultArray = Arrays.deepToString(arrOftempResult);
                        		}
                        		maxBinaryNum--;
                        	}
                        	
                        	myGUI.displayUI("Best offer is: " + bestResultArray + " " + bestResult);
                        	
                        	//Adding information data for winner.
                        	bestResultArray = bestResultArray.replace(" ", "");
                        	bestResultArray = bestResultArray.replace("[", "");
                        	bestResultArray = bestResultArray.replace("]", "");
                            String[] tempAcceptName = bestResultArray.split(",");
                            for (int i = 0; i < tempAcceptName.length; i++) {
                            	for (int j = 0; j <=replyInfoList.size() - 1; j++){
                            		if(tempAcceptName[i].equals(replyInfoList.get(j).name)){
                            			replyInfoList.get(j).status = "accept";
                            		}
                            		myGUI.displayUI("\n" + getLocalName() + "  " + replyInfoList.get(j).name + "  " + replyInfoList.get(j).status );
                            	}
        					}
                            
                            step = 2;
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    /*
                     * Calculating and adding accepted water volume for bidder based on highest price.
                     * Sending message to bidders with two types (Accept proposal or Refuse) based on
                     * accepted water volume to sell.
                     */

                	
                	//Result Preparison.
                    String tempBehaviour= "";
                    if(biddingBehaviourRule == 0){
                        tempBehaviour = "Generous";
                    }else if(biddingBehaviourRule == 1){
                        tempBehaviour = "Neutral";
                    }else {
                        tempBehaviour = "Covetous";
                    }

                    //Result Preparison.
                    String writingFileResult = getLocalName() + "," + farmerInfo.dbName + "," + tempBehaviour + "," + bestResult;
                    //String writingFileResult = getLocalName() + "," + farmerInfo.dbName + "," + bestResult;
                	int writCnt = 0;
                	
                	for (int i = 0; i <= replyInfoList.size() - 1; i++) {
                		myGUI.displayUI("ertyuhygtrfr" + replyInfoList.get(i).status + "\n");
						if(replyInfoList.get(i).status.equals("accept")) {
							writingFileResult = writingFileResult + "," + replyInfoList.get(i).name + "," + replyInfoList.get(i).nameDB +
									"," + replyInfoList.get(i).totalVolume + "," + replyInfoList.get(i).price + "," + replyInfoList.get(i).pctPriceReduce;
							writCnt++;
						}
						
					}
                	if(writCnt == 0) {
                		writingFileResult = writingFileResult + "," + 0.0;
                		output.add(writingFileResult);
                	}else {
						output.add(writingFileResult);
					}
                	
                    //Sending ACCEPT_PROPOSAL to winner.
                    for (int i = 0; i <= replyInfoList.size() -1; i++) {
                    	for (int j = 0; j < bidderAgent.length; j++) {
							if(bidderAgent[j].getLocalName().equals(replyInfoList.get(i).name) && replyInfoList.get(i).status=="accept"){
								// Send the purchase order to the seller that provided the best offer
                                ACLMessage acceptedRequest = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                acceptedRequest.addReceiver(bidderAgent[j]);
                                acceptedRequest.setConversationId("bidding");
                                acceptedRequest.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                myAgent.send(acceptedRequest);
                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo
                                        (acceptedRequest.getReplyWith()));
                                //myGUI.displayUI(acceptedRequest.toString() + "\n");
							}else if (bidderAgent[j].getLocalName().equals(replyInfoList.get(i).name)) {
								ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                rejectMsg.addReceiver(bidderAgent[i]);
                                rejectMsg.setConversationId("bidding");
                                rejectMsg.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                myAgent.send(rejectMsg);
							}
						}
                    }
                    
                    informCnt = acceptNameList.size();
                    
                    myGUI.displayUI( "\nssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss" + output.toString() + "\n");
                    
                    //Writing the all bidder result calculation side to file.
                    //output file location.
                    //String outputFile = "/Users/kitti.ch/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 		//Macbook
                    String outputFile = "F:/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 	//Home PC
                    //String outputFile = "C:/Users/chiewchk/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv";  	//Office
                        
                    try {
                    	WriteToFile(output.toString(), outputFile);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}	
                    myGUI.displayUI("Stop it!!!!!!");
                    myAgent.doSuspend();
                    
                    step = 3;
                    
                    break;
            }

        }
        
		public boolean done() {
        	 if (step == 3 && volumnDict.size() == 0) {
        	//if (step == 3 && maxEuList.size() == 0) {
                myGUI.displayUI("Do not buyer who provide the matching price");
                takeDown();

            }
            return step == 3 ;		//Normal is 0
        }
    }

    private class CovetousBehaviour extends Behaviour {
        //The list of known water selling agent
        private AID[] bidderAgent;
        private int refuseCnt;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        ArrayList<String> bidderList = new ArrayList<String>();  //sorted list follows maximum price factor.
        //ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.

        //Creating dictionary for buyer volume and pricing
        Dictionary<String, Double> volumnDict = new Hashtable<String, Double>();
        Dictionary<String, Double> priceDict = new Hashtable<String, Double>();
        Dictionary<String, Double> profitLossDict = new Hashtable<>();
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    //update bidder list
                    String serviceName = "bidder";
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(serviceName);
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
                    String tempSellingVol = String.valueOf(farmerInfo.sellingVol);
                    cfp.setContent(tempSellingVol);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);

                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        //myGUI.displayUI(reply.toString());
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {

                            System.out.println("Receive message: " + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            String tempName = reply.getSender().getLocalName();
                            double tempVolume = Double.parseDouble(arrOfStr[0]);
                            double tempPrice = Double.parseDouble(arrOfStr[1]);
                            double tempProfitLossValue = Double.parseDouble(arrOfStr[2]);
                            String tempNameDB = arrOfStr[3];
                            int pctPriceReduce = Integer.parseInt(arrOfStr[4]);
                            double tempValue = tempVolume * tempPrice;

                            //Covetous behaviour process for 5% reserved price setting.
                            if(tempPrice >= farmerInfo.sellingPrice + farmerInfo.sellingPrice * 0.05) {
                                replyInfoList.add(new Agents(tempVolume, tempPrice, tempValue, tempProfitLossValue, tempName, tempNameDB, "none", pctPriceReduce));
                                //adding data to dictionary
                                volumnDict.put(reply.getSender().getLocalName(), tempVolume);
                                priceDict.put(reply.getSender().getLocalName(), tempPrice);
                                profitLossDict.put(reply.getSender().getLocalName(), tempProfitLossValue);
                            }
                        }

                        if (repliesCnt >= bidderAgent.length) {

                            //Show reply list (all bidders for this round).
                            myGUI.displayUI("Bidder reply for this stage:" + "\n");
                            myGUI.displayUI("\n" + getAID().getLocalName() + "    voldict  " + volumnDict.size() + "   price dict  " + priceDict.size());
                            for (int i = 0; i <= replyInfoList.size() - 1;i++){
                                myGUI.displayUI(replyInfoList.get(i).toString() + "\n");
                            }
                            // We received all replies
                            for(Enumeration e = volumnDict.keys(); e.hasMoreElements();){
                                String temp = e.nextElement().toString();
                                bidderList.add(temp);
                            }
                            String[] tempBidderList = GetStringArray(bidderList);


                            int maxBinaryNum = (int) Math.pow(2, tempBidderList.length);
                            myGUI.displayUI("Maximum round calculation: " + maxBinaryNum);
                            //Initialize result constructors.
                            //Combinatorial process.
                            while(maxBinaryNum > 1) {
                                String tempResult = binaryToVale(maxBinaryNum -1, tempBidderList);
                                double roundResult = 0.0;
                                double roundVolume = 0.0;
                                String[] arrOftempResult = tempResult.split(" ");
                                int lenArrOfTempResult = arrOftempResult.length;
                                while (lenArrOfTempResult >= 1) {
                                    double volFromDict = volumnDict.get(arrOftempResult[lenArrOfTempResult -1]);
                                    double priceFromDict = priceDict.get(arrOftempResult[lenArrOfTempResult -1]);
                                    double valueFromDict = priceFromDict * volFromDict;
                                    roundResult = roundResult + valueFromDict;
                                    roundVolume = roundVolume + volFromDict;
                                    //System.out.println(arrOftempResult[lenArrOfTempResult -1] + "\n");
                                    lenArrOfTempResult--;
                                }
                                //System.out.println("total value from this decimal orders: " + roundResult + " " + tempResult);


                                if(roundResult > bestResult && roundVolume < farmerInfo.sellingVol) {
                                    bestResult = roundResult;
                                    bestResultArray = Arrays.deepToString(arrOftempResult);
                                }
                                maxBinaryNum--;
                            }

                            myGUI.displayUI("Best offer is: " + bestResultArray + " " + bestResult);

                            //Adding information data for winner.
                            bestResultArray = bestResultArray.replace(" ", "");
                            bestResultArray = bestResultArray.replace("[", "");
                            bestResultArray = bestResultArray.replace("]", "");
                            String[] tempAcceptName = bestResultArray.split(",");
                            for (int i = 0; i < tempAcceptName.length; i++) {
                                for (int j = 0; j <=replyInfoList.size() - 1; j++){
                                    if(tempAcceptName[i].equals(replyInfoList.get(j).name)){
                                        replyInfoList.get(j).status = "accept";
                                    }
                                    myGUI.displayUI("\n" + getLocalName() + "  " + replyInfoList.get(j).name + "  " + replyInfoList.get(j).status );
                                }
                            }

                            step = 2;
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    /*
                     * Calculating and adding accepted water volume for bidder based on highest price.
                     * Sending message to bidders with two types (Accept proposal or Refuse) based on
                     * accepted water volume to sell.
                     */


                    //Result Preparison.
                    String tempBehaviour= "";
                    if(biddingBehaviourRule == 0){
                        tempBehaviour = "Generous";
                    }else if(biddingBehaviourRule == 1){
                        tempBehaviour = "Neutral";
                    }else {
                        tempBehaviour = "Covetous";
                    }

                    //Result Preparison.
                    String writingFileResult = getLocalName() + "," + farmerInfo.dbName + "," + tempBehaviour + "," + bestResult;
                    //String writingFileResult = getLocalName() + "," + farmerInfo.dbName + "," + bestResult;
                    int writCnt = 0;

                    for (int i = 0; i <= replyInfoList.size() - 1; i++) {
                        myGUI.displayUI("ertyuhygtrfr" + replyInfoList.get(i).status + "\n");
                        if(replyInfoList.get(i).status.equals("accept")) {
                            writingFileResult = writingFileResult + "," + replyInfoList.get(i).name + "," + replyInfoList.get(i).nameDB +
                                    "," + replyInfoList.get(i).totalVolume + "," + replyInfoList.get(i).price + "," + replyInfoList.get(i).pctPriceReduce;
                            writCnt++;
                        }

                    }
                    if(writCnt == 0) {
                        writingFileResult = writingFileResult + "," + 0.0;
                        output.add(writingFileResult);
                    }else {
                        output.add(writingFileResult);
                    }

                    //Sending ACCEPT_PROPOSAL to winner.
                    for (int i = 0; i <= replyInfoList.size() -1; i++) {
                        for (int j = 0; j < bidderAgent.length; j++) {
                            if(bidderAgent[j].getLocalName().equals(replyInfoList.get(i).name) && replyInfoList.get(i).status=="accept"){
                                // Send the purchase order to the seller that provided the best offer
                                ACLMessage acceptedRequest = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                acceptedRequest.addReceiver(bidderAgent[j]);
                                acceptedRequest.setConversationId("bidding");
                                acceptedRequest.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                myAgent.send(acceptedRequest);
                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo
                                        (acceptedRequest.getReplyWith()));
                                //myGUI.displayUI(acceptedRequest.toString() + "\n");
                            }else if (bidderAgent[j].getLocalName().equals(replyInfoList.get(i).name)) {
                                ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                rejectMsg.addReceiver(bidderAgent[i]);
                                rejectMsg.setConversationId("bidding");
                                rejectMsg.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                myAgent.send(rejectMsg);
                            }
                        }
                    }

                    informCnt = acceptNameList.size();

                    myGUI.displayUI( "\nssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss" + output.toString() + "\n");

                    //Writing the all bidder result calculation side to file.
                    //output file location.
                    //String outputFile = "/Users/kitti.ch/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 		//Macbook
                    String outputFile = "F:/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv"; 	//Home PC
                    //String outputFile = "C:/Users/chiewchk/OneDrive/PhD-Lincoln/javaProgram/DBandText/ResultCalculation/" + getLocalName() + ".csv";  	//Office

                    try {
                        WriteToFile(output.toString(), outputFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    myGUI.displayUI("Stop it!!!!!!");
                    myAgent.doSuspend();

                    step = 3;

                    break;
            }

        }

        public boolean done() {
            if (step == 3 && volumnDict.size() == 0) {
                //if (step == 3 && maxEuList.size() == 0) {
                myGUI.displayUI("Do not buyer who provide the matching price");
                takeDown();

            }
            return step == 3 ;		//Normal is 0
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
        doSuspend();
        System.out.println(getAID().getName()+" terminating.");
        
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
            this.farmSize = farmSize;
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

    //All parameters and method for combinatorial auction process.

    static ArrayList<ArrayList<String> > getSubset(String[] set, int index) {
        ArrayList<ArrayList<String> > allSubsets;
        if (index < 0) {
            allSubsets = new ArrayList<ArrayList<String> >();
            allSubsets.add(new ArrayList<String>());
        }

        else {
            allSubsets = getSubset(set, index - 1);
            String item = set[index];
            ArrayList<ArrayList<String> > moreSubsets
                    = new ArrayList<ArrayList<String> >();

            for (ArrayList<String> subset : allSubsets) {
                ArrayList<String> newSubset = new ArrayList<String>();
                newSubset.addAll(subset);
                newSubset.add(item);
                moreSubsets.add(newSubset);
            }
            allSubsets.addAll(moreSubsets);
        }
        return allSubsets;
    }

    // Function to convert ArrayList<String> to String[]
    public static String[] GetStringArray(ArrayList<String> arr)
    {
        // declaration and initialize String Array
        String str[] = new String[arr.size()];
        // ArrayList to Array Conversion
        for (int j = 0; j < arr.size(); j++) {
            // Assign each value to String array
            str[j] = arr.get(j);
        }
        return str;
    }

    //adding new class for sorted seller agent data.
    class Agents{
        double totalVolume;
        double price;
        double totalValue;
        double profitLostPct;
        String name;
        String nameDB;
        String status;
        int pctPriceReduce;
        //Constructor
        public Agents(double totalVolume, double price, double totalValue, double profitLostPct, String name, String nameDB, String status, int pctPriceReduce){
            this.totalVolume = totalVolume;
            this.price = price;
            this.totalValue = totalValue;
            this.profitLostPct = profitLostPct;
            this.name = name;
            this.nameDB = nameDB;
            this.status = status;
            this.pctPriceReduce = pctPriceReduce;
        }
        public String toString(){
            return this.name + "  DB name: " + this.nameDB + "  Total Volume: " + this.totalVolume + " Price: " + this.price + " Profit lost (%): " + this.profitLostPct + "  Total Value:  " + this.totalValue;
        }
        public double toValue() {
        	double totalValue = this.price * this.totalVolume;
        	return totalValue;
        }
    }
    class SortbyTotalVol implements Comparator<Agents> {
        //Used for sorting in ascending order of the volume.
        public int compare(Agents a, Agents b) {
            return Double.compare(a.totalVolume, b.totalVolume);
        }
    }
    
    //Storing result of combinatorial auction value which include sorting water sharing, maximum price and profit.
    class combinatorialResult{
    	String[] setOfbidders;
    	double valueFactor;
    	double waterSharingFactor;
    	double profitFactor;
    	
    	public combinatorialResult(String[] setOfbidders, double valueFactor, double waterSharingFactor, double profitFactor) {
    		this.setOfbidders = setOfbidders;
    		this.valueFactor = valueFactor;
    		this.waterSharingFactor = waterSharingFactor;
    		this.profitFactor = profitFactor;
    	}
    }
    
    class SortbyValueFactor implements Comparator<combinatorialResult>{
    	public int compare(combinatorialResult a, combinatorialResult b) {
    		return Double.compare(a.valueFactor, b.valueFactor);
    	}
    }
    
    class SortbyWaterSharingFactor implements Comparator<combinatorialResult>{
    	public int compare(combinatorialResult a, combinatorialResult b) {
    		return Double.compare(a.waterSharingFactor, b.waterSharingFactor);
    	}
    }
    
    class SortByProfitFactor implements Comparator<combinatorialResult>{
    	public int compare(combinatorialResult a, combinatorialResult b) {
    		return Double.compare(a.profitFactor, b.profitFactor);
    	}
    }
    
    public void WriteToFile(String bidderDBList, String outputPath) 
  		  throws IOException {
  		    BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, true));
  		    writer.newLine();
  		    writer.write(bidderDBList);
  		    writer.close();
  		}
    
    //convertDecimal to Binary and calculate values based on decision rules.
    public String binaryToVale(int decimal, String[] setOfReplyAgent) {
    	System.out.println("Decimal input is: " + decimal);
    	String result = "";
    	String binaryString = Integer.toBinaryString(decimal);
    	String[] tempBinaryString = binaryString.split("");
    	for (int i=0; i <= tempBinaryString.length -1; i++ ) {
			String tempXX = tempBinaryString[i];
			
			if(tempXX.contentEquals("1")) {
				result = result + setOfReplyAgent[tempBinaryString.length -1 - i] + " ";
			}
		}
    	
    	return result;
    }

    private static int getRandomNumberInRange(int min, int max){
        if(min >= max){
            throw  new IllegalArgumentException("max number must be greater than min number");
        }
        Random r  = new Random();
        return r.nextInt((max - min) +1) + min;
    }
    
}
