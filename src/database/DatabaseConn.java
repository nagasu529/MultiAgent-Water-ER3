package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 *
 * @author Kitti
 */
public class DatabaseConn {

    public double avgET0 = 0.0;
    public double stdET0 = 7.0;
    public double avgKc = 0.0;
    public double KcValue = 0.0;
    public double KcBased = 0.0;
    public double irrigationRate = 0.0;
    public double grossMaginValue = 0.0;
    public double totalIncome = 0.0;
    public double expanditureValue = 0.0;
    public double tonePerHec = 0.0;
    public double pricePerKG = 0.0;
    public double yieldAmount;
    
    //Initial variable for selected database data.
    public String Name;
    public double FarmSize;
    public double ConsentPrice;
    public double WaterReq;
    public double TotalProfitValue;
    public double TotalCost;
    public double TotalFarmGrossMargin;
    public double PctReduction;
    public double WaterReqAfterReduction;
    public double ProfitAfterReduction;
    
    public double SellingVol;
    public double SellingPrice;
    public double BuyingPirce;
    

    //Database connect for calculationg ET0
    private Connection connect(){
        //SQlite connietion string
    	//String url = "jdbc:sqlite:/Users/kitti.ch/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/db/FarmDB-temp.sqlite"; //Macbook
        String url = "jdbc:sqlite:F:/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/db/FarmDB-temp.sqlite"; //Home PC
        //String url = "jdbc:sqlite:C:/Users/chiewchk/OneDrive - Bansomdejchaopraya Rajabhat University/PhD-Lincoln/javaProgram/DBandText/db/FarmDB-temp.sqlite";  //Office
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        return conn;
    }
    /**
     * Select data from ET0 Cable
     *
     */
    public void selectET(){
        avgET0 = 0.0;
        String sql = "SELECT Location, Month, ET0 FROM ET0";

        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                System.out.println(rs.getInt("Location") + "\t" +
                        rs.getInt("Month") + "\t" +
                        rs.getDouble("ET0"));

            }
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    //Insert records to database (Farmers)
    public void insertFarmer(String Name, double FarmSize, double ConsentPrice, double WaterReq,  double TotalProfitValue, 
    		double TotalCost, double TotalFarmGrossMargin, double PctReduction, double WaterReqAfterReduction, double ProfitAfterReduction, double BuyingPrice) {
    	int numRowInserted = 0;
    	String sql = "INSERT INTO Bidders(Name, FarmSize, ConsentPrice, WaterReq, TotalProfitValue, TotalCost, TotalFarmGrossMargin, PctReduction, "
    			+ "WaterReqAfterReduction, ProfitAfterReduction, BuyingPrice) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
    	//System.out.println(sql);
    	
    	try {
    		Connection conn = this.connect();
    		PreparedStatement pstmt = conn.prepareStatement(sql);
    		pstmt.setString(1, Name);
    		pstmt.setDouble(2, FarmSize);
    		pstmt.setDouble(3, ConsentPrice);
    		pstmt.setDouble(4, WaterReq);
    		pstmt.setDouble(5, TotalProfitValue);
    		pstmt.setDouble(6, TotalCost);
    		pstmt.setDouble(7, TotalFarmGrossMargin);
    		pstmt.setDouble(8, PctReduction);
    		pstmt.setDouble(9, WaterReqAfterReduction);
    		pstmt.setDouble(10, ProfitAfterReduction);
    		pstmt.setDouble(11, BuyingPrice);
    		
    		pstmt.executeUpdate();
		} catch (SQLException e) {
			// TODO: handle exception
			System.out.println(e);
		}
    }
    
    //Read data from Farmers table for all auctions.
    //public void selectBidders(String bidderName) {
    public void selectBiddersRandom() {
    	//ArrayList<agentInfo> tempSelect = new ArrayList<agentInfo>();
    	String sql  = "SELECT * FROM Bidders ORDER BY RANDOM() LIMIT 1";
    	//String sql  = String.format("SELECT * FROM Bidders WHERE Name='%s'",bidderName);
    	
    	try {
			Connection conn = this.connect();
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				 Name = resultSet.getString("Name");
				 FarmSize = resultSet.getDouble("FarmSize");
				 ConsentPrice = resultSet.getDouble("ConsentPrice");
				 WaterReq = resultSet.getDouble("WaterReq");
				 TotalProfitValue = resultSet.getDouble("TotalProfitValue");
				 TotalCost = resultSet.getDouble("TotalCost");
				 TotalFarmGrossMargin = resultSet.getDouble("TotalFarmGrossMargin");
				 PctReduction = resultSet.getDouble("PctReduction");
				 WaterReqAfterReduction = resultSet.getDouble("WaterReqAfterReduction");
				 ProfitAfterReduction = resultSet.getDouble("ProfitAfterReduction");
				 BuyingPirce = resultSet.getDouble("BuyingPrice");
				 
				
			}
		} catch (SQLException e) {
			System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
		}catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void selectBidders(String bidderName) {
    	//ArrayList<agentInfo> tempSelect = new ArrayList<agentInfo>();
    	String sql  = String.format("SELECT * FROM Bidders WHERE Name='%s'",bidderName);    	
    	try {
			Connection conn = this.connect();
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				 Name = resultSet.getString("Name");
				 FarmSize = resultSet.getDouble("FarmSize");
				 ConsentPrice = resultSet.getDouble("ConsentPrice");
				 WaterReq = resultSet.getDouble("WaterReq");
				 TotalProfitValue = resultSet.getDouble("TotalProfitValue");
				 TotalCost = resultSet.getDouble("TotalCost");
				 TotalFarmGrossMargin = resultSet.getDouble("TotalFarmGrossMargin");
				 PctReduction = resultSet.getDouble("PctReduction");
				 WaterReqAfterReduction = resultSet.getDouble("WaterReqAfterReduction");
				 ProfitAfterReduction = resultSet.getDouble("ProfitAfterReduction");
				 BuyingPirce = resultSet.getDouble("BuyingPrice");		
			}
		} catch (SQLException e) {
			System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
		}catch (Exception e) {
			e.printStackTrace();
		}
    }
    
  //Read data from Farmers table for all actions.
    public void selectSeller(String SellerName) {
    	//ArrayList<agentInfo> tempSelect = new ArrayList<agentInfo>();
    	
    	String sql  = String.format("SELECT * FROM Sellers WHERE Name='%s'",SellerName);
    	
    	try {
			Connection conn = this.connect();
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				 Name = resultSet.getString("Name");
				 FarmSize = resultSet.getDouble("FarmSize");
				 ConsentPrice = resultSet.getDouble("ConsentPrice");
				 WaterReq = resultSet.getDouble("WaterReq");
				 TotalProfitValue = resultSet.getDouble("TotalProfitValue");
				 TotalCost = resultSet.getDouble("TotalCost");
				 TotalFarmGrossMargin = resultSet.getDouble("TotalFarmGrossMargin");
				 PctReduction = resultSet.getDouble("PctReduction");
				 WaterReqAfterReduction = resultSet.getDouble("WaterReqAfterReduction");
				 ProfitAfterReduction = resultSet.getDouble("ProfitAfterReduction");
				 SellingVol = resultSet.getDouble("SellingVol");
				 SellingPrice = resultSet.getDouble("SellingPrice");
			}
		} catch (SQLException e) {
			System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
		}catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void selectSellerRandom() {
    	//ArrayList<agentInfo> tempSelect = new ArrayList<agentInfo>();
    	
    	String sql  = "SELECT * FROM Sellers ORDER BY RANDOM() LIMIT 1";
    	
    	try {
			Connection conn = this.connect();
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				 Name = resultSet.getString("Name");
				 FarmSize = resultSet.getDouble("FarmSize");
				 ConsentPrice = resultSet.getDouble("ConsentPrice");
				 WaterReq = resultSet.getDouble("WaterReq");
				 TotalProfitValue = resultSet.getDouble("TotalProfitValue");
				 TotalCost = resultSet.getDouble("TotalCost");
				 TotalFarmGrossMargin = resultSet.getDouble("TotalFarmGrossMargin");
				 PctReduction = resultSet.getDouble("PctReduction");
				 WaterReqAfterReduction = resultSet.getDouble("WaterReqAfterReduction");
				 ProfitAfterReduction = resultSet.getDouble("ProfitAfterReduction");
				 SellingVol = resultSet.getDouble("SellingVol");
				 SellingPrice = resultSet.getDouble("SellingPrice");
			}
		} catch (SQLException e) {
			System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
		}catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void ET0Spring(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 where (Month BETWEEN 9 AND 11)";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            //System.out.println("ET0 Spring is: "+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void ET0Summer(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 where Month  = 12 or Month BETWEEN 1 AND 2";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                //System.out.println(rs.getDouble("ET0"));
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            //System.out.println("ET0 Summer is:"+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void ET0Autumn(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 WHERE Month BETWEEN 3 AND 5";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            //System.out.println("ET0 Autumn is:"+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void ET0Winter(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 WHERE Month BETWEEN 6 AND 8";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            //System.out.println("ET0 Winter is: "+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void KcCalculation(String cropName, String cropPeriod){

        //temp value
        double tmpCal = 0.0;

        String sql = "SELECT Duration.Crop, Duration.Initial, Period.initialDay, Duration.CropDev, Period.CropDevDay, "
                + "Duration.Mid, Period.MidDay, Duration.Late, Period.LateDay, Period.TotalDay FROM Duration INNER JOIN "
                + "Period ON Duration.Crop = period.Crop WHERE Period.Crop = ? AND Period.period = ?;";
        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            //set value
            pstmt.setString(1,cropName);
            pstmt.setString(2, cropPeriod);
            ResultSet rs = pstmt.executeQuery();

            //loop through the result set
            while (rs.next()) {
                avgKc = avgKc + (rs.getDouble("Initial")*rs.getDouble("InitialDay")) +
                        (rs.getDouble("CropDev")*rs.getDouble("CropDevDay")) +
                        (rs.getDouble("Mid")*rs.getDouble("MidDay")) +
                        (rs.getDouble("Late")*rs.getDouble("LateDay"));
            }
            System.out.println(avgKc);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    //Irrigation rate from FAO white paper book

    public Double getIrrigationTypeValue(String IrrigationType){
        irrigationRate = 0.0;
        String sql = "select FWValue FROM FWIrriRate where IrrigationEvent=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,IrrigationType);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("FWValue");
            irrigationRate = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return irrigationRate;
    }

    //Gross margin from database
    public void getGrossMarginValue(String cropName){
        grossMaginValue = 0.0;
        String sql = "select GrossMarginHa FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            grossMaginValue = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getTotalIncome(String cropName){
        totalIncome = 0.0;
        String sql = "select TotalIncome FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            totalIncome = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getexpanditureValue(String cropName){
        expanditureValue = 0.0;
        String sql = "select Expanditure FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            expanditureValue = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getTonPerHa(String cropName){
        tonePerHec = 0.0;
        String sql = "select TonePerHectre FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            tonePerHec = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public double getPricePerKG(String cropName){
        pricePerKG = 0.0;
        String sql = "select PricePerKG FROM tempInput where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("PricePerKG");
            pricePerKG = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return pricePerKG;
    }

    public double getYieldAmount(String cropName){
        yieldAmount = 0.0;
        String sql = "select YieldAmount FROM tempInput where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("YieldAmount");
            yieldAmount = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return yieldAmount;
    }
    
    public double getCostPerKg(String cropName){
        double costPerKg = 0.0;
        String sql = "select CostPerKg FROM tempInput where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("CostPerKg");
            costPerKg = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return costPerKg;
    }

    //Original KC calculation (without Ke soil water wetting data).

    public Double KcStageValue(String cropName, String cropStage, String irrigationType){
        //Temp calculation value
        KcValue = 0.0;
        double KcMax = 0.0;
        double KcMin = 0.0;
        double KcCurrent = 0.0;
        double Fc = 0.0;
        double floweringValue = 0.0;
        double germinationValue = 0.0;
        double developmentValue = 0.0;
        double ripeningValue = 0.0;
        double hight = 0.0;
        //double kWater = 0.0;

        //Phasing parameters
        //kWater = Double.parseDouble(soilWaterRate);


        getIrrigationTypeValue(irrigationType);
        //System.out.println("irrigation type value:" + irrigationRate);


        //cropStage Categories

        if(cropName.equals("Pasture") && cropStage.equals("Initial")){
            cropStage = "Flowering";
        }else if(cropName.equals("Pasture") && cropStage.equals("Late Season")){
            cropStage = "Ripening";
        }

        String sql = "SELECT Flowering, Germination, Development, Ripening, MaxCropHight FROM Duration WHERE Crop=?";
        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            //Set the value
            pstmt.setString(1, cropName);
            //
            ResultSet rs = pstmt.executeQuery();
            floweringValue = rs.getDouble("Flowering");
            germinationValue = rs.getDouble("Germination");
            developmentValue = rs.getDouble("Development");
            ripeningValue = rs.getDouble("Ripening");
            hight = rs.getDouble("MaxCropHight");
            KcCurrent = rs.getDouble(cropStage);
            //System.out.println(germinationValue);
            //System.out.println(KcCurrent);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }

        //Calculating Min and Max Kc
        double[] valueList = {floweringValue, germinationValue, developmentValue, ripeningValue};
        sortAlgor(valueList);
        KcMax = valueList[valueList.length -1];
        KcMin = valueList[0];

        //System.out.println(floweringValue);
        //Calculating Fc
        Fc =  fcCal(KcCurrent, KcMin, KcMax, hight);

        //Finding the minimum of Few
        double[] Fvalue = {1-Fc,irrigationRate};
        sortAlgor(Fvalue);
        Fc = Fvalue[0] * KcMax;

        //Ke calculation
        double Ke = keCal(KcMax, KcCurrent);

        //comparing Ke and Fc
        double temp = 0.0;
        if (Ke <= Fc) {
            temp = Ke;
        } else {
            temp = Fc;
        }

        //result of Kc value which include irrigation system and soil type data
        KcValue = KcCurrent + temp;
        //System.out.println("Kr is " + temp);
        //System.out.println("Kc based current is" + KcCurrent);
        //System.out.println("Kc Stage value is " + KcValue);
        return KcValue;
    }


    //Input generater data from database.
    public void randFarmInputData(String farmerName){

    }


//Method session

    //Boubble sort
    static void sortAlgor(double[] arr){
        int n = arr.length;
        double temp = 0.0;
        for (int i = 0; i < arr.length; i++) {
            for(int j = 1; j < (n-i); j++ ){
                if(arr[j-1] > arr [j]){
                    //swap element
                    temp = arr[j-1];
                    arr[j-1] = arr[j];
                    arr[j] = temp;
                }
            }
        }
    }

    static double fcCal(double KcCurrent, double KcMin, double KcMax, double hight ){
        double temp = ((KcCurrent - KcMin)/(KcMax - KcMin));
        temp = Math.pow(temp, (1+(0.5*hight)));
        return temp;
    }

    static double keCal(double KcMax, double KcCurrent){
        double temp = (KcMax - KcCurrent);
        return temp;
    }

}