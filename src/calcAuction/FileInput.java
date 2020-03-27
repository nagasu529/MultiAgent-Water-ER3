package calcAuction;
import database.DatabaseConn;
import jade.util.leap.Iterator;

import java.awt.image.CropImageFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Random;

import calcAuction.FileInput.cropType;



public class FileInput extends DatabaseConn {
	
	DecimalFormat df = new DecimalFormat("#.##");
	
	DatabaseConn app = new DatabaseConn();
	Random rand = new Random();

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
			app.KcStageValue(cropName, tempInput[1], tempInput[8]);
			kcStageValue = app.KcValue;
			dsValue = calcDSValue(cropName, cropStage, droughtSensitivity);
            cvValue = calcCVValue(plotSize, yieldAmount, pricePerKg);
			stValue = calcSTValue(plotSize, soilType);
			waterReq = calcWaterReqPerDay(kcStageValue, valueET, plotSize);
            literPerSecHec = calcLitePerSecHecDay(waterReq, plotSize);
            soilWaterContainValue = calcSoilMoistureValue(15, 30);
            waterReqWithSoil = calcWaterReqWithSoil(waterReq, soilWaterContainValue);
            totalWaterReq = totalWaterReq + waterReqWithSoil;
			
			outputInArrayList.add(new cropType(cropName, cropStage, droughtSensitivity, plotSize, yieldAmount,
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
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Pea (field)", "Maize", "Wheat", "Barley", "Pea (vining)", "Oil seed", "Hybrid carrot seed"));

		//all pasture type
		//ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Pasture", "White clover", "Pasture", "Kale", "Fodder beet", "Perennial ryegrass"));

		//all crops and pasture.
		ArrayList<String> cropNameGen = new ArrayList<String>(Arrays.asList("Wheat", "Barley", "White clover", "Perennial ryegrass", "Pea (field)", "Kale", "Fodder beet", "Hybrid carrot seed", "Maize", "Pea (vining)", "Oil seed"));

		List<String> irrigationTypeGen = Arrays.asList("Sprinkler", "Basin", "Border", "Furrow", "Trickle");
        List<String> cropStageGenText = Arrays.asList("Flowering", "Germination", "Development", "Ripening");
        //int cropStageGen = ThreadLocalRandom.current().nextInt(1, 4);
        //int droughtSensitivityGen = ThreadLocalRandom.current().nextInt(1,3);
        //double plotSizeGen = ThreadLocalRandom.current().nextDouble(300,1000);
        //int soilTypeGen = ThreadLocalRandom.current().nextInt(1, 3);
        //double consentCostGen = ThreadLocalRandom.current().nextDouble(10000, 20000);
        
		//Getting farm name and water consent information.
        int numberOfElements = 5;
        
		//farmName = farmerNameGen.get(rand.nextInt(farmerNameGen.size()));
		//waterConsentCost = getRandDoubleRange(10000, 20000);
		
		for (int i = 0; i < numberOfElements; i++) {
			int cropNameGenIndex = rand.nextInt(cropNameGen.size());
			cropName = cropNameGen.get(cropNameGenIndex);
			int cropStageGenIndex = rand.nextInt(cropStageGenText.size());
            cropNameGen.remove(cropNameGenIndex);
            cropStage = getRandIntRange(1, 4);
            droughtSensitivity = getRandIntRange(1, 3);

            // Adding the number of farm size.
			if(numberOfElements == 4){
				plotSize =  200 - farmSizeMax;
			}else {
				plotSize = getRandDoubleRange(35, 55);
				farmSizeMax = farmSizeMax + plotSize;
			}

            yieldAmount = app.getYieldAmount(cropName);
            pricePerKg = app.getPricePerKG(cropName);
            soilType = getRandIntRange(1, 3);
            int irrigationTypeIndex = rand.nextInt(irrigationTypeGen.size());
            String irrigationType = irrigationTypeGen.get(irrigationTypeIndex);
            app.getIrrigationTypeValue(irrigationTypeGen.get(irrigationTypeIndex));
            irrigationTypeValue = app.irrigationRate;
            app.KcStageValue(cropName, cropStageGenText.get(cropStage - 1), irrigationTypeGen.get(irrigationTypeIndex));
            kcStageValue = app.KcValue;
            costPerKg = app.getCostPerKg(cropName);
                        
            //Generate eET0 on Summer
            valueET = etValueDB("Summer");
            dsValue = calcDSValue(cropName, cropStage, droughtSensitivity);
            cvValue = calcCVValue(plotSize, yieldAmount, pricePerKg);
			stValue = calcSTValue(plotSize, soilType);
			waterReq = calcWaterReqPerDay(kcStageValue, valueET, plotSize);
            literPerSecHec = calcLitePerSecHecDay(waterReq, plotSize);
            soilWaterContainValue = calcSoilMoistureValue(15, 30);
            waterReqWithSoil = calcWaterReqWithSoil(waterReq, soilWaterContainValue);
            
			outputInArrayList.add(new cropType(cropName, cropStage, droughtSensitivity, plotSize, yieldAmount,
					pricePerKg, soilType, irrigationTypeValue, kcStageValue, literPerSecHec, waterReq, soilWaterContainValue,
					waterReqWithSoil, waterReduction, productValueLost, dsValue, cvValue, stValue, cropEU, costPerKg, profitLost, grossMargin, waterNeed));
			System.out.println("Crop Name: " + cropName + "  water requirement: " + df.format(waterReq) + "  Value ET: " + df.format(valueET) + "  Plot size:  " + df.format(plotSize) + "  Yield amount: " + df.format(yieldAmount) +
					"  Price per kg. : " + df.format(pricePerKg) + "  Crop stage: " + df.format(cropStage) + "  kc Stage Value: " + df.format(kcStageValue) + "  water Req with Soil: " + df.format(waterReqWithSoil) + " soil water contain: " + df.format(soilWaterContainValue) +
					" Profit value: "  + df.format(cvValue));
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
                value = 10;
            else if (cropStage == 2)
            	value = 20;
            else
            	value = 30;
        }
        else{
        	value = (cropStage *10) + droughtSensitivity;
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
	/***
    public String calcCropEU(ArrayList<cropType> inputArrayList,int decisionMethod) {

		String log = "";

		switch (decisionMethod) {
			case '0':
				//Decision-1 CV value is the first priority.
				log = log + "The first priority is crop value";
				Collections.sort(inputArrayList, new SortbyCvValue());
				Collections.reverse(inputArrayList);
				break;
			case '1':
				//Second decision: The Drought sensitivity.
				log = log + "The first priority is crop drought sensitivity";
				Collections.sort(inputArrayList, new SortbyDsValue());
				Collections.reverse(inputArrayList);
				break;
			case '3':
				//Third decision: Soil moisture factors.
				log = log + "the first priority is soil moisture level";
				Collections.sort(inputArrayList, new SortbyDsValue());
				Collections.reverse(inputArrayList);
		}
		return log;
    }
	 ***/

    /***
	public String calcCropEU(ArrayList<cropType> inputArrayList) {
		double sumCV = 0.0;
		double sumDS = 0.0;
		double sumST = 0.0;
		double maxDS = (inputArrayList.size() * 33);	//Maximum dsValue on farm.

		String log = "";
		for (int i = 0; i <= inputArrayList.size() -1; i++) {
			sumCV = sumCV + inputArrayList.get(i).cvValue;
			sumDS = sumDS + inputArrayList.get(i).dsValue;
			sumST = sumST + inputArrayList.get(i).stValue;
			System.out.print(inputArrayList.get(i).toString());
		}

		//Decision-1 CV value is the first priority.
		Collections.sort(inputArrayList, new SortbyCvValue());
		Collections.reverse(inputArrayList);
		if(inputArrayList.get(0).cvValue > (sumCV * 0.7)) {
			log = log + "Choosing dicision 1 \n" + "First priority: Crop value \n";

			for (int j = 0; j <= inputArrayList.size() -1; j++) {
				cropType tempData = inputArrayList.get(j);
				tempData.cropEU = (0.0 * tempData.dsValue) + (0.0 * tempData.stValue) + (1.0 * tempData.cvValue);
				inputArrayList.remove(j);
				inputArrayList.add(j,tempData);
			}
			Collections.sort(inputArrayList, new SortbyCvValue());
			Collections.reverse(inputArrayList);
		}else if (sumDS > (maxDS * 0.6)){
			log = log + "Choosing dicision 2 \n" + "First priority: Drought Sensitivity \n";
			for (int j = 0; j <= inputArrayList.size() -1; j++) {
				cropType tempData = inputArrayList.get(j);
				tempData.cropEU = (1.0 * tempData.dsValue) + (0.0 * tempData.stValue) + (0.0 * tempData.cvValue);
				inputArrayList.remove(j);
				inputArrayList.add(j,tempData);
			}
			Collections.sort(inputArrayList, new SortbyDsValue());
			Collections.reverse(inputArrayList);
		}else {
			log = log + "Choosing dicision 3 \n" + "First priority: Drought Soil type \n";
			for (int j = 0; j <= inputArrayList.size() -1; j++) {
				cropType tempData = inputArrayList.get(j);
				tempData.cropEU = (1.0 * tempData.dsValue) + (1.0 * tempData.stValue) + (0.0 * tempData.cvValue);
				inputArrayList.remove(j);
				inputArrayList.add(j,tempData);
			}
			Collections.sort(inputArrayList, new SortbyStValue());
			Collections.reverse(inputArrayList);
		}

		return log;
	}
    ***/

    public String calcWaterReduction(double reductionPct, ArrayList<cropType> inputArray, String agentName, double waterConsertCost, int decisionMethod) {
    	//Preparing.
    	//Collections.sort(inputArray, new SortbyEU());
    	Collections.reverse(inputArray);
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

		switch (decisionMethod) {
			case '0':
				//Decision-1 CV value is the first priority.
				decisionStr =  "The first priority is crop value";
				Collections.sort(inputArray, new SortbyCvValue());
				Collections.reverse(inputArray);
				break;
			case '1':
				//Second decision: The Drought sensitivity.
				decisionStr = "The first priority is crop drought sensitivity";
				Collections.sort(inputArray, new SortbyDsValue());
				Collections.reverse(inputArray);
				break;
			case '3':
				//Third decision: Soil moisture factors.
				decisionStr = "the first priority is soil moisture level";
				Collections.sort(inputArray, new SortbyDsValue());
				Collections.reverse(inputArray);
		}
    	
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

			if((tempArray.waterReduction + totalReduction) > totalReductionRequire){
				tempArray.waterReduction = totalReductionRequire - totalReduction;
				totalReduction = totalReductionRequire;
			}else {
				totalReduction = totalReduction + tempArray.waterReduction;
			}
			tempArray.waterNeed = tempArray.waterReduction;
			/***
			if(totalReduction > totalReductionRequire){
				tempArray.waterReduction = totalReduction - totalReductionRequire;
				totalReduction = totalReductionRequire;
			}
			***/
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

			//Checking the total reduction with the reduction requirement before changed profit calculation
			/***
			if(totalReduction > totalReductionRequire){
				totalReduction = totalReductionRequire;
			}
			 ***/

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
		/***
		//Writing the result to database (Farmers table).
		double tempReductionReq = totalWaterReqOnFarm - totalReduction;
		app.insertFarmer(agentName, totalFarmSize, waterConsertCost, totalWaterReqOnFarm, totalFarmCvValue, totalFarmCvValueCost, totalFarmGrossMargin, reductionPct, tempReductionReq, totalCvAfterReduction, waterConsertCost/1000);
    	***/
    	return log;
    }
    
	public class cropType{
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

        cropType(String cropName, int cropStage, int droughtSensitivity, double plotSize, double yieldAmount, double pricePerKg, int soilType,
        		double irrigationType, double kcStageValue, double literPerSecHec, double waterReq, double soilWaterContainValue, 
        		double waterReqWithSoil, double waterReduction, double productValueLost, int dsValue, double cvValue, double stValue,
        		double cropEU, double costPerKg, double profitLostPct, double grossMarginValue, double waterNeed) {
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
        	public void set(int i, double tempData) {
			// TODO Auto-generated method stub
			
		}
			public String toString() {
        		double profitAfterReduction = ((100 - this.profitLostPct)/100) * this.cvValue;
        		return this.cropName + "  Water requirement for crop:  " + df.format(this.waterReqWithSoil) +  "  water reduction:  " 
			+ df.format(this.waterReduction) + "  Cost:  " + df.format(this.costPerKg) + "  Profit after reduction: " + df.format(profitAfterReduction) + "  Profit loss (%) :  " + df.format(this.profitLostPct) +
			"  Gross margin:  " + df.format(this.grossMarginValue) + "  Buying volume need (mm^3/day): " + df.format(this.waterNeed) + "\n";
        	}
        	public String toStringSource(){
        	String tempCropstage = "";
        	if (this.cropStage == 1){
        		tempCropstage = "Initial stage";
			}else if(this.cropStage == 2){
        		tempCropstage = "Development stage";
			}else if(this.cropStage == 3){
        		tempCropstage = "Germination stage";
			}else {
        		tempCropstage = "Flowering stage";
			}
        	return "Crop name : " + this.cropName + "  Planting size: " + df.format(this.plotSize) + "  Crop Stage: " + tempCropstage + "  Water Requirement: " + df.format(this.waterReqWithSoil) + "  Profit before reduction: " + df.format(this.cvValue) +
					"  Kc stage value: " + df.format(this.kcStageValue) + "  Soil moisture contain: " + df.format(this.soilWaterContainValue);
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
		public int compare(cropType a, cropType b) {
			return Double.compare(a.cropEU, b.cropEU);
		}
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

/***
//Profit lost calculation.
currentWaterVolForCrop = tempArray.waterReqWithSoil - tempArray.waterReduction;

tempArray.grossMarginPct = 100 - ((cvValueCost * 100)/tempArray.cvValue);

//Adding cvValueCost cvValue
yieldAmountAfterReduction = (currentWaterVolForCrop * tempArray.yieldAmount)/tempArray.waterReqWithSoil;
cvValueAfterReduction = yieldAmountAfterReduction * tempArray.pricePerKg * tempArray.plotSize;
totalCvAfterReduction = totalCvAfterReduction + cvValueAfterReduction;
tempArray.profitLostPct = (cvValueAfterReduction *100)/tempArray.cvValue;
System.out.println(df.format(tempArray.cvValue) + "   " + df.format(cvValueAfterReduction));

//Comparing the Profit loss and 
/***
if(cvValueAfterReduction >= cvValueCost) {
	tempArray.waterNeed = 0.0;
}else {
	//finding the %diff value.
	diffValue = cvValueCost - cvValueAfterReduction;
	yieldKgCoverringCost = diffValue/(tempArray.pricePerKg * tempArray.plotSize);
	tempArray.waterNeed = 1.0;
	
	//Buying water volume need.
	//tempArray.waterNeed = ((yieldKgCoverringCost * 1.2) * tempArray.waterReqWithSoil)/tempArray.yieldAmount;
}
***/

/***
//Yield amount after water reduction.
double productAfterReduction = (tempArray.waterReduction * tempArray.yieldAmount)/tempArray.waterReq;

//Calculating and comparing income before and after water reduction scheme.
double incomeBeforeReductionPerKg = tempArray.cvValue;		//total income before water reduction scheme.
double incomeAfterReductionPerKg = productAfterReduction * tempArray.pricePerKg * tempArray.plotSize;	//total income after water reduction.
double costForEachCrop = tempArray.yieldAmount * tempArray.costPerKg;	//minimum cost per crop.

tempArray.profitLost = 100 - ((incomeAfterReductionPerKg * 100)/incomeBeforeReductionPerKg);


//comparing costCV and afterReduction CV value.
if(incomeAfterReductionPerKg >= costForEachCrop) {
	tempArray.profitLost = 0.0;
	tempArray.waterNeed = 0.0;
}else {
	double moreKgForSell = (Math.abs(incomeAfterReductionPerKg - costForEachCrop))/tempArray.pricePerKg;
	tempArray.waterNeed = (moreKgForSell * tempArray.yieldAmount)/tempArray.waterReqWithSoil;
}

***/

//CalcWaterReduction Back up before editing the profit value after reduction.
/***
 * public String calcWaterReduction(double reductionPct, ArrayList<cropType> inputArray, String agentName, double waterConsertCost) {
 *     	//Preparing.
 *     	//Collections.sort(inputArray, new SortbyEU());
 *     	Collections.reverse(inputArray);
 *     	String log = "";
 *     	double totalWaterReqOnFarm = 0.0;
 *     	//Total cost initialize.
 *     	double totalReduction = 0.0;
 *     	double resultReductionPct = 0.0;
 *     	double totalFarmCvValueCost = 0.0;
 *     	double totalFarmCvValue = 0.0;
 *     	double totalFarmGrossMargin = 0.0;
 *     	double totalCvAfterReduction = 0.0;
 *     	double cvValueCost;
 *     	double totalReductionRequire;
 *     	double totalFarmSize = 0.0;
 *
 *     	for (int i = 0; i <= inputArray.size() - 1; i++) {
 *     		totalWaterReqOnFarm = totalWaterReqOnFarm + inputArray.get(i).waterReqWithSoil;
 *     		totalFarmSize = totalFarmSize + inputArray.get(i).plotSize;
 *                }
 *
 *     	totalReductionRequire = totalWaterReqOnFarm * (reductionPct)/100;
 *
 *     	//Reduction rules and functions.
 *     	for (int i = 0; i <= inputArray.size() -1; i++) {
 * 			cropType tempArray = inputArray.get(i);
 * 			//Profit lost calculation.
 * 			cvValueCost = tempArray.yieldAmount * tempArray.costPerKg * tempArray.plotSize;
 * 			totalFarmCvValue = totalFarmCvValue + tempArray.cvValue;
 *
 * 			//Adding cvValueCost cvValue
 * 			totalFarmCvValueCost = totalFarmCvValueCost + cvValueCost;
 * 			tempArray.grossMarginValue = tempArray.cvValue - cvValueCost;
 * 			totalFarmGrossMargin = totalFarmGrossMargin + tempArray.grossMarginValue;
 *
 * 			if (totalReduction < totalReductionRequire ) {
 * 				if(tempArray.cropName.equals("Pasture") && tempArray.cropStage == 1) {
 * 					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.5;
 * 					totalReduction = totalReduction + tempArray.waterReduction;
 *                }else if (tempArray.cropName.equals("Pasture") && tempArray.cropStage == 2) {
 * 					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.2;
 * 					totalReduction = totalReduction + tempArray.waterReduction;
 *                }else if (tempArray.cropName.equals("Pasture") && tempArray.cropStage == 3) {
 * 					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.1;
 * 					totalReduction = totalReduction + tempArray.waterReduction;
 *                }else if (tempArray.cropStage == 1) {
 * 					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.5;
 * 					totalReduction = totalReduction + tempArray.waterReduction;
 *                }else if (tempArray.cropStage == 2) {
 * 					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.2;
 * 					totalReduction = totalReduction + tempArray.waterReduction;
 *                }else if (tempArray.cropStage == 3) {
 * 					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.15;
 * 					totalReduction = totalReduction + tempArray.waterReduction;
 *                }else {
 * 					tempArray.waterReduction = tempArray.waterReqWithSoil * 0.1;
 * 					totalReduction = totalReduction + tempArray.waterReduction;
 *                }
 *
 *            }else {
 * 				tempArray.waterReduction = 0.0;
 *            }
 *
 * 			//Profit lost calculation.
 * 			double currentWaterVolForCrop = tempArray.waterReqWithSoil - tempArray.waterReduction;
 *
 * 			//Adding cvValueCost cvValue
 * 			double yieldAmountAfterReduction = (currentWaterVolForCrop * tempArray.yieldAmount)/tempArray.waterReqWithSoil;
 * 			double cvValueAfterReduction = yieldAmountAfterReduction * tempArray.pricePerKg * tempArray.plotSize;
 * 			totalCvAfterReduction = totalCvAfterReduction + cvValueAfterReduction;
 * 			if(tempArray.waterReduction == 0) {
 * 				tempArray.profitLostPct = 0;
 *            }else {
 * 				tempArray.profitLostPct = (cvValueAfterReduction *100)/tempArray.cvValue;
 *            }
 * 			System.out.println("Profit  " + df.format(tempArray.cvValue) + "   " + df.format(cvValueAfterReduction));
 * 			System.out.println("sdfdsfsdfds  " + df.format(currentWaterVolForCrop));
 *
 * 			inputArray.remove(i);
 * 			inputArray.add(i,tempArray);
 *        }
 *
 *     	//Result after reduction.
 *     	resultReductionPct = (totalReduction * 100)/totalWaterReqOnFarm;
 * 		log = log + "\n" + "Total water requirement on farm:  " + df.format(totalWaterReqOnFarm) + "\n" + "Required reduction:  "
 * 				+ df.format(totalReductionRequire) + "  " + df.format(reductionPct) + " (%)";
 * 		log = log + "Total water reduction:  " + df.format(totalReduction) + "    " + df.format(resultReductionPct) + " (%)" + "\n"
 * 				+ "Total gross margin Value :  " + totalFarmGrossMargin/5 + "\n" + "Total profit loss after reduction (%):  " + (100 - ((totalCvAfterReduction * 100)/totalFarmCvValue));
 * 		log = log + "\n";
 *
 * 		for (cropType e : inputArray) {
 * 			log = log + e.toString()+ "\n";
 *        }
 *
 * 		//Writing the result to database (Farmers table).
 * 		//double tempReductionReq = totalWaterReqOnFarm - totalReduction;
 * 		//app.insertFarmer(agentName, totalFarmSize, waterConsertCost, totalWaterReqOnFarm, totalFarmCvValue, totalFarmCvValueCost, totalFarmGrossMargin, reductionPct, tempReductionReq, totalCvAfterReduction, waterConsertCost/1000);
 *
 *     	return log;
		 *     }
 ***/
