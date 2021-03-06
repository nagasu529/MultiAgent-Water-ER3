package calcAuction;

import database.DatabaseConn;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileInputTemp extends DatabaseConn {
	
	DecimalFormat df = new DecimalFormat("#.##");
	
	DatabaseConn app = new DatabaseConn();
	Random rand = new Random();

	private int order;
	private String farmName;
	private String districName;
	private Double farmSize;
	private Double waterconsentCost;
	private String cropName;
	private int cropStage;
	private int droughtSensitivity;
	private Double plotSize;
	private Double yieldAmount;
	private Double pricePerKg;
	private int soilType;
	private Double irrigationTypeValue;
	private Double kcStageValue;
	private Double literPerSecHec;
    private Double waterReq;
    private Double soilWaterContainValue;
    private Double waterReqWithSoil;
    private Double waterReduction = 0.0;
    private Double productValueLost = 0.0;
    private Double cropEU = 0.0;
    private Double profitLost = 0.0;
    private Double costPerKg = 0.0;
    private Double grossMargin = 0.0;
    private Double waterNeed = 0.0;
    
    private Double totalWaterReq;
    private int dsValue;
    private Double cvValue;
    private Double stValue;
    private Double totalCropProductValue;
    private Double valueET;
    private Double waterConsentCost;
    
	
	public List<String> readText(String FileName, List<String> resultList) {
		try (Stream<String> stream = Files.lines(Paths.get((FileName)))){
			resultList = stream.collect(Collectors.toList());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		String[] farmInfoArray;
		String separator = "\\s*,";
		farmInfoArray = resultList.get(0).split(separator);
		System.out.println("Importing data process finished");
		
		return resultList;
	}
	
	public void farmFactorValues(List<String> inputList, ArrayList<cropType> outputInArrayList) {
		String separator = "\\s*,*";
		int listSize = inputList.size();
		
		//Getting farm name and water consent information.
		String[] farmGenInfo = inputList.get(0).split(separator);
		farmName = farmGenInfo[0];
		districName = farmGenInfo[1];
		farmSize = Double.parseDouble(farmGenInfo[2]);
		waterconsentCost = Double.parseDouble(farmGenInfo[3]);
		
		while (listSize != 1) {
			String[] tempInput = inputList.get(listSize -1).split(separator);
			order = listSize - 1;
			cropName = tempInput[0];
			if(tempInput[0].equals("Pasture")) {
				if(tempInput[0].equals("Pasture") && tempInput[1].equals("Initial")) {
				cropStage = 3;
				}else if (tempInput[0].equals("Pasture") && tempInput[1].equals("Development") ) {
					cropStage= 2;
				}else {
					cropStage = 1;
				}
			}else {
				if (tempInput[1].equals("Flowering") || tempInput.equals("Grain Filling"))
                    cropStage = 4;
                else if (tempInput[1].equals("Germination"))
                    cropStage = 3;
                else if (tempInput[1].equals("Development"))
                    cropStage = 2;
                else
                    cropStage = 1;
			}
			if(tempInput[2].equals("Low")) {
				droughtSensitivity = 1;
			}else if (tempInput[2].equals("Medium")) {
				droughtSensitivity = 2;
			}else {
				droughtSensitivity = 3;
			}
			plotSize = Double.parseDouble(tempInput[3]);
			yieldAmount = Double.parseDouble(tempInput[4]);
			pricePerKg = Double.parseDouble(tempInput[5]);
			if(tempInput[6].equals("Light")) {
				soilType = 3;
			}else if (tempInput[6].equals("Medium")) {
				soilType = 2;
			}else {
				soilType = 1;
			}
			app.getIrrigationTypeValue(tempInput[8]);
			irrigationTypeValue = app.irrigationRate;
			app.KcStageValue(cropName, tempInput[1], tempInput[8], soilType, droughtSensitivity);
			kcStageValue = app.KcValue;
			dsValue = calcDSValue(cropName, cropStage, droughtSensitivity);
            cvValue = calcCVValue(plotSize, yieldAmount, pricePerKg);
			stValue = calcSTValue(plotSize, soilType);
			waterReq = calcWaterReqPerDay(kcStageValue, valueET, plotSize);
            literPerSecHec = calcLitePerSecHecDay(waterReq, plotSize);
            soilWaterContainValue = calcSoilMoistureValue(15, 30);
            waterReqWithSoil = calcWaterReqWithSoil(waterReq, soilWaterContainValue);
            totalWaterReq = totalWaterReq + waterReqWithSoil;
			
			outputInArrayList.add(new cropType(order, cropName, cropStage, droughtSensitivity, plotSize, yieldAmount,
					pricePerKg, soilType, irrigationTypeValue, kcStageValue, literPerSecHec, waterReq, soilWaterContainValue,
					waterReqWithSoil, waterReduction, productValueLost, dsValue, cvValue, stValue, cropEU, costPerKg, profitLost, grossMargin, waterNeed));
			listSize--;
		}
	}
	
	public void randFarmFactorValues(ArrayList<cropType> outputInArrayList) {

		//Preparing random data
        Random rand = new Random();

        //Setting the farm size max to 200 Hectre.
        double farmSizeMax = 0;


        List<String> farmerNameGen = Arrays.asList("John", "Mark", "Dave", "Morgan", "Steve", "Anna", "Heather", "Nick", "Toby", "Rob");

		//all crops without pasture.
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Pea (field)", "Maize", "Wheat", "Barley", "Pea (vining)", "Oil seed", "Hybrid carrot seed", "White clover", "Kale", "Fodder beet"));
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Kale", "Maize", "Wheat", "Barley", "Oil seed", "Peanut"));
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Maize", "Wheat", "Peanut", "Wheat", "Maize", "Wheat", "Peanut", "Peanut"));


		//all pasture type
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Pasture", "Pasture", "Perennial ryegrass"));
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Pasture", "Pasture", "Pasture", "Pasture", "Pasture", "Pasture"));

		//all crops and pasture.
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Wheat", "Barley", "White clover", "Perennial ryegrass", "Pea (field)", "Kale", "Fodder beet", "Hybrid carrot seed", "Maize", "Pea (vining)", "Oil seed"));
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Wheat", "Maize", "Peanut", "Oil seed", "Pasture", "Pasture"));
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("White clover", "Maize", "Barley", "Pasture", "Perennial ryegrass"));
		ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Maize", "Wheat", "Oil seed", "Pasture", "Pasture"));


		List<String> irrigationTypeGen = Arrays.asList("Sprinkler", "Basin", "Border", "Furrow", "Trickle");
        List<String> cropStageGenText = Arrays.asList("","Germination", "Development","Flowering", "Ripening");

		//Getting farm name and water consent information.
        int numberOfElements = 5;
        
		//farmName = farmerNameGen.get(rand.nextInt(farmerNameGen.size()));
		//waterConsentCost = getRandDoubleRange(10000, 20000);
		
		for (int i = 0; i < numberOfElements; i++) {
			order = i + 1;
			int cropNameGenIndex = rand.nextInt(cropNameGen.size());
			cropName = cropNameGen.get(cropNameGenIndex);
			int cropStageGenIndex = rand.nextInt(cropStageGenText.size());
            cropNameGen.remove(cropNameGenIndex);

            //Crop stage loop function
            //cropStage = getRandIntRange(1, 4);
			if(cropName == "Pasture"){
				//cropStage = getRandIntRange(0, 2);

				cropStage = i +1;
				if(i > 3){
					cropStage = getRandIntRange(1,3);
				}
			}else {
				//cropStage = getRandIntRange(0,3);

				cropStage = i + 1;
				if(i >=4){
					cropStage = getRandIntRange(1,4);
				}
			}

            //droughtSensitivity = 3;
			droughtSensitivity = getRandIntRange(1,3);
			/***
			droughtSensitivity = i +1;
			if(droughtSensitivity >=3){
				droughtSensitivity = 3;
			}
			 ***/

            // Adding the number of farm size.
			plotSize = 50.0;
			farmSizeMax = farmSizeMax + plotSize;
			/***
			if(numberOfElements == 4){
				plotSize =  200 - farmSizeMax;
			}else {
				plotSize = getRandDoubleRange(35, 55);
				farmSizeMax = farmSizeMax + plotSize;
			}
			***/
            yieldAmount = app.getYieldAmount(cropName);
            pricePerKg = app.getPricePerKG(cropName);
            //soilType = 1;
            soilType = getRandIntRange(1, 3);
            /***
            int irrigationTypeIndex = rand.nextInt(irrigationTypeGen.size());
            String irrigationType = irrigationTypeGen.get(irrigationTypeIndex);
            app.getIrrigationTypeValue(irrigationTypeGen.get(irrigationTypeIndex));
            irrigationTypeValue = app.irrigationRate;
			app.KcStageValue(cropName, cropStageGenText.get(cropStage), irrigationTypeGen.get(irrigationTypeIndex),soilType, droughtSensitivity);
			 ***/
            app.getIrrigationTypeValue(irrigationTypeGen.get(0));
            irrigationTypeValue = app.irrigationRate;
			app.KcStageValue(cropName, cropStageGenText.get(cropStage), irrigationTypeGen.get(0),soilType, droughtSensitivity);
			kcStageValue = app.KcStageValue;
			//app.KcCalculation(cropName,cropStageGenText.get(cropStage - 1));
			//KcValue = app.KcValue;


            costPerKg = app.getCostPerKg(cropName);
                        
            //Generate eET0 on Summer
            valueET = etValueDB("Summer");
            dsValue = calcDSValue(cropName, cropStage, droughtSensitivity);
            cvValue = calcCVValue(plotSize, yieldAmount, pricePerKg);
			stValue = calcSTValue(plotSize, soilType);
			//waterReq = calcWaterReqPerDay(KcValue, valueET, plotSize);
			waterReq = calcWaterReqPerDay(kcStageValue, valueET, plotSize);
            literPerSecHec = calcLitePerSecHecDay(waterReq, plotSize);
            //soilWaterContainValue = calcSoilMoistureValue(15, 30);
			soilWaterContainValue = 0.3;
            waterReqWithSoil = calcWaterReqWithSoil(waterReq, soilWaterContainValue);
            

            outputInArrayList.add(new cropType(order, cropName, cropStage, droughtSensitivity, plotSize, yieldAmount,
					pricePerKg, soilType, irrigationTypeValue, kcStageValue, literPerSecHec, waterReq, soilWaterContainValue,
					waterReqWithSoil, waterReduction, productValueLost, dsValue, cvValue, stValue, cropEU, costPerKg, profitLost, grossMargin, waterNeed));

			 System.out.println("No.: " + order + "  Crop Name: " + cropName + "  water requirement: " + df.format(waterReq) + "  Value ET: " + df.format(valueET) + "  Plot size:  " + df.format(plotSize) + "  Yield amount: " + df.format(yieldAmount) +
			 "  Price per kg. : " + df.format(pricePerKg) + "  Crop stage: " + df.format(cropStage) + "  kc Stage Value: " + df.format(kcStageValue) + "  water Req with Soil: " + df.format(waterReqWithSoil) + " soil water contain: " + df.format(soilWaterContainValue) +
			 " Profit value: "  + df.format(cvValue) + "  Drought sensitivity: " + df.format(droughtSensitivity) + "\n" + "Ds value: " + df.format(dsValue) + "  Irrigation type: " + irrigationTypeGen.get(0));
			/***
			outputInArrayList.add(new cropType(order, cropName, cropStage, droughtSensitivity, plotSize, yieldAmount,
					pricePerKg, soilType, irrigationTypeValue, KcValue, literPerSecHec, waterReq, soilWaterContainValue,
					waterReqWithSoil, waterReduction, productValueLost, dsValue, cvValue, stValue, cropEU, costPerKg, profitLost, grossMargin, waterNeed));
			System.out.println("No.: " + order + "  Crop Name: " + cropName + "  water requirement: " + df.format(waterReq) + "  Value ET: " + df.format(valueET) + "  Plot size:  " + df.format(plotSize) + "  Yield amount: " + df.format(yieldAmount) +
					"  Price per kg. : " + df.format(pricePerKg) + "  Crop stage: " + df.format(cropStage) + "  kc Stage Value: " + df.format(KcValue) + "  water Req with Soil: " + df.format(waterReqWithSoil) + " soil water contain: " + df.format(soilWaterContainValue) +
					" Profit value: "  + df.format(cvValue) + "  Drought sensitivity: " + df.format(droughtSensitivity) + "\n" + "Ds value: " + df.format(dsValue));
			 ***/
		}
	}
	
	
	//Related calculation methods for farm information input.
	
	public double etValueDB(String season) {
		double value;
		if(season.equals("Spring")) {
			app.ET0Spring();
			//valueET = app.avgET0;
		}else if (season.equals("Summer")) {
			app.ET0Summer();
			//valueET = app.avgET0;
		}else if (season.equals("Autumn")) {
			app.ET0Autumn();
		}else {
			app.ET0Winter();
			//valueET = app.avgET0;
		}
		value = app.avgET0;
		return value;
	}
	
	public double calcSoilMoistureValue(int day, double avgSoilMoistureMonthly) {
        double value;
        double deltaSoilMoisture;

        deltaSoilMoisture = avgSoilMoistureMonthly/31;
        value = avgSoilMoistureMonthly - (deltaSoilMoisture * day);
        return value;
    }
	
	public double calcWaterReqPerDay(double kcStageValue, double valueET, double plotSize) {
		double value = (kcStageValue * valueET) * plotSize * 10;
		return value;
	}
	
	public double calcLitePerSecHecDay(double cumerPerday, double plotSize) {
		double litePerSecHecDay;
		litePerSecHecDay = ((cumerPerday * 1000)/plotSize)/(24 * 60 * 60);
		return litePerSecHecDay;
	}
	
	public double calcWaterReqWithSoil(double waterReq, double soilWaterContainValue) {
        double value = waterReq - soilWaterContainValue;
        return value;
    }
    
    public int calcDSValue(String cropName, int cropStage, int droughtSensitivity){
        int  value;
    	if (cropName.equals("Pasture")){
            if (cropStage == 1)
                value = 30;
            else if (cropStage == 2)
            	value = 20;
            else
            	value = 10;
        }
        else{
        	value = (cropStage * 10) + droughtSensitivity;
        }
        return value;
    }
    
    public double calcCVValue(double plotSize, double yieldAmount, double pricePerKg) {
    	double value;
    	value = (plotSize * yieldAmount) * pricePerKg;
    	return value;
    }
    
    public double calcSTValue(double soilType, double plotSize) {
    	double value;
    	value = soilType * plotSize;
    	return value;
    }
    
    public void calcGroosMargin(double farmProductionValue, double waterConsentCost, double outputVariable){
        outputVariable = farmProductionValue - waterConsentCost;
    }

    public double calcProductValueLost(double waterReductionMM, double cropProductValue, double cropWaterReq) {
        double outputVariable = (waterReductionMM * cropProductValue) / cropWaterReq;
        return outputVariable;
    }

    public String calcCropEU(ArrayList<cropType> inputArrayList,int decisionMethod) {

		String log = "";

		switch (decisionMethod) {
			case '0':
				//Decision-1 CV value is the first priority.
				log = log + "The first priority is crop value";
				cropEU = (cvValue * 0.5) + (dsValue * 0.25) + (stValue * 0.25);
				Collections.sort(inputArrayList, new SortbyEU());
				Collections.reverse(inputArrayList);
				break;
			case '1':
				//Second decision: The Drought sensitivity.
				log = log + "The first priority is crop drought sensitivity";
				cropEU = (cvValue * 0.25) + (dsValue * 0.5) + (stValue * 0.25);
				Collections.sort(inputArrayList, new SortbyEU());
				Collections.reverse(inputArrayList);
				break;
			case '3':
				//Third decision: Soil moisture factors.
				log = log + "the first priority is soil moisture level";
				cropEU = (cvValue * 0.25) + (dsValue * 0.25) + (stValue * 0.5);
				Collections.sort(inputArrayList, new SortbyEU());
				Collections.reverse(inputArrayList);
				break;
			case '4':
				//Special decision: Pasture only which not concern about productivity factor.
				log = log + "Soil type and soil moisture level are the same priority";
				cropEU = (cvValue *0) + (dsValue * 0.5) + (stValue * 0.5);
				Collections.sort(inputArrayList, new SortbyEU());
				Collections.reverse(inputArrayList);
			default:
				log = log = "the first priority is Crop stage";
				Collections.sort(inputArrayList, new SortbyCropStage());
		}
		return log;
    }

    public String calcWaterReduction(double reductionPct, ArrayList<cropType> inputArray, String agentName, double waterConsertCost) {
    	//Preparing.
    	String log = "";
    	String tempInput = "";
    	double totalWaterReqOnFarm = 0.0;
    	//Total cost initialize.
    	double totalReduction = 0.0;
    	double resultReductionPct = 0.0;
    	double totalFarmCvValueCost = 0.0;
    	double totalFarmCvValue = 0.0;
    	double totalFarmGrossMargin = 0.0;
    	double totalCvAfterReduction = 0.0;
    	double cvValueCost;
    	double totalReductionRequire;
    	double totalFarmSize = 0.0;
    	String decisionStr = "";

    	double totalCvValueWithoutAlgor = 0.0;
    	
    	for (int i = 0; i <= inputArray.size() - 1; i++) {
    		totalWaterReqOnFarm = totalWaterReqOnFarm + inputArray.get(i).waterReqWithSoil;
    		totalFarmSize = totalFarmSize + inputArray.get(i).plotSize;
		}
    	
    	totalReductionRequire = totalWaterReqOnFarm * (reductionPct)/100;
    	
    	//Reduction rules and functions.
    	for (int i = 0; i <= inputArray.size() -1; i++) {
			cropType tempArray = inputArray.get(i);
			//Profit lost calculation.
			cvValueCost = tempArray.yieldAmount * tempArray.costPerKg * tempArray.plotSize;
			totalFarmCvValue = totalFarmCvValue + tempArray.cvValue;
			double tempCVWithOutAlgor = (((1 - (reductionPct/100)) * tempArray.waterReqWithSoil) * tempArray.cvValue)/tempArray.waterReqWithSoil;
			totalCvValueWithoutAlgor = totalCvValueWithoutAlgor + tempCVWithOutAlgor;
			
			//Adding cvValueCost cvValue
			totalFarmCvValueCost = totalFarmCvValueCost + cvValueCost;
			tempArray.grossMarginValue = tempArray.cvValue - cvValueCost;
			totalFarmGrossMargin = totalFarmGrossMargin + tempArray.grossMarginValue;
			//double tempTotalReductionReq = totalReductionRequire;
	    	
			if (totalReduction < totalReductionRequire ) {
				if((tempArray.cropName.equals("Pasture") && tempArray.cropStage == 1) || (tempArray.cropName.equals("White clover") && tempArray.cropStage == 1)
				|| (tempArray.cropName.equals("Kale") && tempArray.cropStage == 1) || (tempArray.cropName.equals("Fodder beet") && tempArray.cropStage == 1) || (tempArray.cropName.equals("Perennial ryegrass") && tempArray.cropStage == 1)) {
					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.5;
				}else if ((tempArray.cropName.equals("Pasture") && tempArray.cropStage == 2) || (tempArray.cropName.equals("White clover") && tempArray.cropStage == 2)
						|| (tempArray.cropName.equals("Kale") && tempArray.cropStage == 2) || (tempArray.cropName.equals("Fodder beet") && tempArray.cropStage == 2) || (tempArray.cropName.equals("Perennial ryegrass") && tempArray.cropStage == 2)) {
					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.2;
				}else if ((tempArray.cropName.equals("Pasture") && tempArray.cropStage == 3) || (tempArray.cropName.equals("White clover") && tempArray.cropStage == 3)
						|| (tempArray.cropName.equals("Kale") && tempArray.cropStage == 3) || (tempArray.cropName.equals("Fodder beet") && tempArray.cropStage == 3) || (tempArray.cropName.equals("Perennial ryegrass") && tempArray.cropStage == 3)) {
					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.1;
				}else if (tempArray.cropStage == 1) {
					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.5;
				}else if (tempArray.cropStage == 2) {
					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.2;
				}else if (tempArray.cropStage == 3) {
					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.15;
				}else {
					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.1;
				}

			}else {
				tempArray.waterReduction = 0.0;
			}

			totalReduction = totalReduction + tempArray.waterReduction;
			tempArray.waterNeed = tempArray.waterReduction;

			//Adding cvValueCost cvValue

			//Profit lost calculation.
			double currentWaterVolForCrop = tempArray.waterReqWithSoil - tempArray.waterReduction;
			double yieldAmountAfterReduction = (currentWaterVolForCrop * tempArray.yieldAmount)/tempArray.waterReqWithSoil;
			double cvValueAfterReduction = yieldAmountAfterReduction * tempArray.pricePerKg * tempArray.plotSize;
			totalCvAfterReduction = totalCvAfterReduction + cvValueAfterReduction;
			if(tempArray.waterReduction == 0) {
				tempArray.profitLostPct = 0;
            } else {
				tempArray.profitLostPct = 100 - (cvValueAfterReduction *100)/tempArray.cvValue;
			}

			System.out.println("Profit  " + df.format(tempArray.cvValue) + "   " + df.format(cvValueAfterReduction));
			System.out.println(tempArray.cropName + "  " + df.format(currentWaterVolForCrop));
			
			inputArray.remove(i);
			inputArray.add(i,tempArray);
		}
    	
    	//Result after reduction.
    	resultReductionPct = (totalReduction * 100)/totalWaterReqOnFarm;
		log = log + "\n" + "Total water requirement on farm:  " + df.format(totalWaterReqOnFarm) + "\n" + "Required reduction:  " 
				+ df.format(totalReductionRequire) + "  " + df.format(reductionPct) + " (%)" + "\n" + decisionStr + "\n";
		log = log + "Total water reduction:  " + df.format(totalReduction) + "    " + df.format(resultReductionPct) + " (%)" + "\n"
				+ "Total gross margin Value :  " + totalFarmGrossMargin/5 + "\n" + "Total profit loss after reduction (%):  " + (100 - (totalCvAfterReduction * 100)/totalFarmCvValue) +  "  Value: " + totalCvAfterReduction + "\n" +
					"Total profit value before reduction : " + totalFarmCvValue + "  Total profit value after reduction without system (%): " + (100 -  (totalCvValueWithoutAlgor * 100)/totalFarmCvValue) + totalCvValueWithoutAlgor + "\n";
		log = log + "\n";
		
		for (cropType e : inputArray) {
			log = log + e.toString()+ "\n";
		}

    	return log;
    }
    
	public class cropType{
    	int order;
        String cropName;
        int cropStage;
        int droughtSensitivity;
        double plotSize;
        double yieldAmount;
        double pricePerKg;
        int soilType;
        double irrigationType;
        double kcStageValue;
        double literPerSecHec;
        double waterReq;
        double soilWaterContainValue;
        double waterReqWithSoil;
        double waterReduction;
        double productValueLost;
        int dsValue;
        double cvValue;
        double stValue;
        double cropEU;
        double costPerKg;
        double profitLostPct;
        double grossMarginValue;
        double waterNeed;

        cropType(int order, String cropName, int cropStage, int droughtSensitivity, double plotSize, double yieldAmount, double pricePerKg, int soilType,
        		double irrigationType, double kcStageValue, double literPerSecHec, double waterReq, double soilWaterContainValue, 
        		double waterReqWithSoil, double waterReduction, double productValueLost, int dsValue, double cvValue, double stValue,
        		double cropEU, double costPerKg, double profitLostPct, double grossMarginValue, double waterNeed) {
        	this.order = order;
            this.cropName = cropName;
            this.cropStage = cropStage;
            this.droughtSensitivity = droughtSensitivity;
            this.plotSize = plotSize;
            this.yieldAmount = yieldAmount;
            this.pricePerKg = pricePerKg;
            this.soilType = soilType;
            this.irrigationType = irrigationType;
            this.kcStageValue = kcStageValue;
            this.literPerSecHec = literPerSecHec;
            this.waterReq = waterReq;
            this.soilWaterContainValue = soilWaterContainValue;
            this.waterReqWithSoil = waterReqWithSoil;
            this.waterReduction = waterReduction;
            this.productValueLost = productValueLost;
            this.dsValue = dsValue;
            this.cvValue = cvValue;
            this.stValue = stValue;
            this.cropEU = cropEU;
            this.costPerKg = costPerKg;
            this.profitLostPct = profitLostPct;
            this.grossMarginValue = grossMarginValue;
            this.waterNeed = waterNeed;
        }
			public String toString() {
        		String tempCropstage = "";
				if(this.cropName == "Pasture"){
					if(this.cropStage == 3){
						tempCropstage = "Grazing";
					}else if(this.cropStage == 2){
						tempCropstage = "Development";
					}else{
						tempCropstage = "Late-season";
					}
				}else {
					if (this.cropStage == 1){
						tempCropstage = "Late-season stage";
					}else if(this.cropStage == 2){
						tempCropstage = "Mid-season stage";
					}else if(this.cropStage == 3){
						tempCropstage = "Crop development stage";
					}else {
						tempCropstage = "Initial stage";
					}
				}

        		double profitAfterReduction = ((100 - this.profitLostPct)/100) * this.cvValue;
        		return this.cropName + " Crop Stage: " + tempCropstage + "  Water requirement for crop:  " + df.format(this.waterReqWithSoil) +  "  water reduction:  "
			+ df.format(this.waterReduction) + "  Cost:  " + df.format(this.costPerKg) + "  Profit after reduction: " + df.format(profitAfterReduction) + "  Profit loss (%) :  " + df.format(this.profitLostPct) +
			"  Gross margin:  " + df.format(this.grossMarginValue) + "  Buying volume need (mm^3/day): " + df.format(this.waterNeed) + "\n";
        	}
        	public String toStringSource(){
        		String tempCropstage = "";
        		if(this.cropName == "Pasture"){
        			if(this.cropStage == 3){
						tempCropstage = "Grazing";
					}else if(this.cropStage == 2){
						tempCropstage = "Development";
					}else{
						tempCropstage = "Late-season";
					}
				}else {
					if (this.cropStage == 1){
						tempCropstage = "Late-season stage";
					}else if(this.cropStage == 2){
						tempCropstage = "Mid-season stage";
					}else if(this.cropStage == 3){
						tempCropstage = "Crop development stage";
					}else {
					tempCropstage = "Initial stage";
					}
				}
        		return "Crop name : " + this.cropName + "  Planting size: " + df.format(this.plotSize) + "  Crop Stage: " + tempCropstage + "  Water Requirement: " + df.format(this.waterReqWithSoil) + "  Profit before reduction: " + df.format(this.cvValue) +
						"  Kc stage value: " + df.format(this.kcStageValue) + "  Soil moisture contain: " + df.format(this.soilWaterContainValue);
			}


			public String toStringValidation(){
        	String tempValidationTxt = "";
        	return "Order: " + this.order + " Crop name: " + this.cropName + "  CVvalue: " + df.format(this.cvValue) + "Soil type: " + df.format(this.soilType) + "  STvalue: " + df.format(this.stValue) + " Drought Sensitivity: " + df.format(this.droughtSensitivity) + "  DSValue: " + df.format(this.dsValue);
			}
    }
	
	//Sorted value
	class SortbyDsValue implements Comparator<cropType>{
		public int compare(cropType a, cropType b) {
			return Double.compare(a.dsValue, b.dsValue); 
		}
	}
	
	class SortbyCvValue implements Comparator<cropType>{
		public int compare(cropType a, cropType b) {
			return Double.compare(a.cvValue, b.cvValue);
		}
	}
	
	class SortbyStValue implements Comparator<cropType>{
		public int compare(cropType a, cropType b) {
			return Double.compare(a.stValue, b.stValue);
		}
	}
	class SortbyEU implements Comparator<cropType>{
		public int compare(cropType a, cropType b) { return Double.compare(a.cropEU, b.cropEU);
		}
	}
	class SortbyCropStage  implements  Comparator<cropType>{
    	public int compare(cropType a, cropType b){ return Integer.compare(a.cropStage, b.cropStage);}
    }

	//Random value data.
	public int getRandIntRange(int min, int max){
        if(min >= max){
            throw new IllegalArgumentException("max number must be more than min number");
        }
        return rand.nextInt((max - min) + 1) + min;
    }

    public double getRandDoubleRange(double min, double max){
        if(min >= max){
            throw new IllegalArgumentException("max number must be more than min number");
        }
        return rand.nextDouble() * (max - min) + min;
    }
}
