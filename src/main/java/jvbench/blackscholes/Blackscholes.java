package jvbench.blackscholes;

import jdk.incubator.vector.*;

import java.io.*;

public class Blackscholes {

    static private final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    static private final int SPECIES_LENGTH = SPECIES.length();

    static final int nThreads = 1;
    static final String outputFileName = "";
    static int numOptions;
    static OptionData [] data;
    static double [] prices;

    private static final int PAD =  256;
    private static final int LINESIZE =  64;
    private static final int NUM_RUNS = 100;

    private static double [] buffer;
    private static double [] sptprice;
    private static double [] strike;
    private static double [] rate;
    private static double [] volatility;
    private static double [] otime;


    private static double[] buffer2;
    static long [] otype;

    private static final double inv_sqrt_2xPI = (double) 0.39894228040143270286;

    public static void main(String[] args) {
        Blackscholes.init(System.getProperty("input","/blackscholes/input/in_64K.input"));
//        Blackscholes.scalar();
        Blackscholes.vector();
    }

    public static void init(String inputFileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Blackscholes.class.getResourceAsStream(inputFileName)))) {

            numOptions = Integer.parseInt(reader.readLine());

            buffer = new double[5 * numOptions + PAD];
            sptprice = new double[5 * numOptions + PAD];
            strike = new double[5 * numOptions + PAD];
            rate = new double[5 * numOptions + PAD];
            volatility = new double[5 * numOptions + PAD];
            otime = new double[5 * numOptions + PAD];

            data = new OptionData[numOptions];
            prices = new double[numOptions];

            buffer2 = new double[5 * numOptions];
            otype = new long[5 * numOptions];

            for (int loopNum = 0; loopNum < numOptions; loopNum++) {
                String line = reader.readLine();
                String [] options = line.split(" ");
                if (options.length != 9) {
                    throw new IllegalArgumentException("Invalid number of options: " + options.length + " should be 9");
                }
                data[loopNum] = new OptionData(
                        Double.parseDouble(options[0]),
                        Double.parseDouble(options[1]),
                        Double.parseDouble(options[2]),
                        Double.parseDouble(options[3]),
                        Double.parseDouble(options[4]),
                        Double.parseDouble(options[5]),
                        options[6].charAt(0),
                        Double.parseDouble(options[7]),
                        Double.parseDouble(options[8])
                );

            }

        } catch (IOException e) {
            System.err.println("ERROR: Unable to read file " + inputFileName + "\n" + e.getMessage());
        }

        for (int i = 0; i < numOptions; i++) {
            otype[i] = (data[i].getOptionType() == 'P') ? 1 : 0;
            sptprice[i] = data[i].getS();
            strike[i] = data[i].getStrike();
            rate[i] = data[i].getR();
            volatility[i] = data[i].getV();
            otime[i] = data[i].getT();
        }
    }

    public static double [] getPrices() {
        return prices;
    }

    public static OptionData [] getData() {return data;}

    public static void scalar() {
        for (int j=0; j < NUM_RUNS; j++) {
            for (int i=0; i<numOptions; i++) {
                double price =  blkSchlsEqEuroNoDiv(sptprice[i], strike[i], rate[i], volatility[i], otime[i], otype[i], 0);
                prices[i] = price;
            }
        }
    }

    public static void vector() {
        for (int j=0; j < NUM_RUNS; j++) {
            int limit = SPECIES.loopBound(numOptions);
            int i;
            for (i=0; i<limit; i += SPECIES_LENGTH) {
                blkSchlsEqEuroNoDivVector(i);
            }

            for (; i<numOptions; i++) {
                double price =  blkSchlsEqEuroNoDiv(sptprice[i], strike[i], rate[i], volatility[i], otime[i], otype[i], 0);
                prices[i] = price;
            }
        }
    }

    private static void blkSchlsEqEuroNoDivVector(int i) {

        DoubleVector xStockPrice;
        DoubleVector xStrikePrice;
        DoubleVector xRiskFreeRate;
        DoubleVector xVolatility;
        DoubleVector xTime;
        DoubleVector xSqrtTime;

        DoubleVector xLogTerm;
        DoubleVector xD1, xD2;
        DoubleVector xPowerTerm;
        DoubleVector xDen;

        DoubleVector xRatexTime;
        DoubleVector xFutureValueX;

        VectorMask<Long> xMask;
        LongVector xOtype;
        LongVector  xZero;

        DoubleVector xOptionPrice;
        DoubleVector xOptionPrice1;
        DoubleVector xOptionPrice2;
        DoubleVector xfXd1;
        DoubleVector xfXd2;

        xStockPrice = DoubleVector.fromArray(SPECIES, sptprice, i);
        xStrikePrice = DoubleVector.fromArray(SPECIES, strike, i);
        xRiskFreeRate = DoubleVector.fromArray(SPECIES, rate, i);
        xVolatility = DoubleVector.fromArray(SPECIES, volatility, i);

        xTime = DoubleVector.fromArray(SPECIES, otime, i);
        xSqrtTime = xTime.sqrt();

        xLogTerm = (xStockPrice.div(xStrikePrice)).lanewise(VectorOperators.LOG);

        xPowerTerm = xVolatility.mul(xVolatility);
        xPowerTerm = xPowerTerm.mul(0.5f);

        xD1 = xRiskFreeRate.add(xPowerTerm);
        xD1 = xD1.mul(xTime);
        xD1 = xD1.add(xLogTerm);


        xDen = xVolatility.mul(xSqrtTime);
        xD1 = xD1.div(xDen);
        xD2 = xD1.sub(xDen);

        xfXd1 = cndfSIMD(xD1);
        xfXd2 = cndfSIMD(xD2);


        xRatexTime = xRiskFreeRate.mul(xTime);
        xRatexTime = xRatexTime.lanewise(VectorOperators.NEG);
        xFutureValueX = xRatexTime.lanewise(VectorOperators.EXP);
        xStrikePrice = DoubleVector.fromArray(SPECIES, strike, i);
        xFutureValueX = xFutureValueX.mul(xStrikePrice);


        xOtype = LongVector.fromArray(LongVector.SPECIES_PREFERRED, otype, i);
        xZero = LongVector.zero(LongVector.SPECIES_PREFERRED);
        xMask = xZero.eq(xOtype);


        xOptionPrice1 = xStockPrice.mul(xfXd1);
        xOptionPrice1 = xOptionPrice1.sub(xFutureValueX.mul(xfXd2));

        xfXd1 = (DoubleVector.broadcast(SPECIES, 1.0f).sub(xfXd1));
        xfXd2 = (DoubleVector.broadcast(SPECIES, 1.0f).sub(xfXd2));

        xOptionPrice2 = xFutureValueX.mul(xfXd2);
        xOptionPrice2 = xOptionPrice2.sub(xStockPrice.mul(xfXd1));

        xOptionPrice = xOptionPrice2.blend(xOptionPrice1, xMask.cast(SPECIES));

        xOptionPrice.intoArray(prices, i);
    }

    private static DoubleVector cndfSIMD(DoubleVector xInput) {
        DoubleVector xNPrimeofX;
        DoubleVector xK2;
        DoubleVector xK2_2;
        DoubleVector xK2_3;
        DoubleVector xK2_4;
        DoubleVector xK2_5;
        DoubleVector xLocal;
        DoubleVector xLocal_1;
        DoubleVector xLocal_2;
        DoubleVector xLocal_3;

        VectorMask<Double> xMask;

        DoubleVector expValues;

        DoubleVector xOne = DoubleVector.broadcast(SPECIES, 1);


        xMask = xInput.lt(0.0f);


        xInput = xInput.lanewise(VectorOperators.NEG, xMask);

        expValues = xInput.mul(xInput);
        expValues = expValues.mul(-0.5f);
        expValues = expValues.lanewise(VectorOperators.EXP);

        xNPrimeofX = expValues;
        xNPrimeofX = xNPrimeofX.mul(inv_sqrt_2xPI);


        xK2 = xInput.mul(0.2316419f).add(1.0f);
        xK2 = xOne.div(xK2);

        xK2_2 = xK2.mul(xK2);
        xK2_3 = xK2_2.mul(xK2);
        xK2_4 = xK2_3.mul(xK2);
        xK2_5 = xK2_4.mul(xK2);

        xLocal_1 = xK2.mul(0.319381530f);
        xLocal_2 = xK2_2.mul(-0.356563782f);
        xLocal_3 = xK2_3.mul(1.781477937f);
        xLocal_2 = xLocal_2.add(xLocal_3);
        xLocal_3 = xK2_4.mul(-1.821255978f);
        xLocal_2 = xLocal_2.add(xLocal_3);
        xLocal_3 = xK2_5.mul(1.330274429f);
        xLocal_2 = xLocal_2.add(xLocal_3);


        xLocal_1 = xLocal_2.add(xLocal_1);

        xLocal = xLocal_1.mul(xNPrimeofX);
        xLocal = xOne.sub(xLocal);

        xLocal = xLocal.blend(xOne.sub(xLocal), xMask);

        return xLocal;




    }

    private static double blkSchlsEqEuroNoDiv(double sptprice, double strike, double rate, double volatility, double time, long otype, double timet) {
        double OptionPrice;

        // local private working variables for the calculation
        double xStockPrice;
        double xStrikePrice;
        double xRiskFreeRate;
        double xVolatility;
        double xTime;
        double xSqrtTime;

        double logValues;
        double xLogTerm;
        double xD1;
        double xD2;
        double xPowerTerm;
        double xDen;
        double d1;
        double d2;
        double FutureValueX;
        double NofXd1;
        double NofXd2;
        double NegNofXd1;
        double NegNofXd2;

        xStockPrice = sptprice;
        xStrikePrice = strike;
        xRiskFreeRate = rate;
        xVolatility = volatility;

        xTime = time;
        xSqrtTime = (double) Math.sqrt(xTime);

        logValues = (double) Math.log( sptprice / strike );

        xLogTerm = logValues;

        xPowerTerm = xVolatility * xVolatility;
        xPowerTerm = (xPowerTerm * 0.5f);


        xD1 = xRiskFreeRate + xPowerTerm;
        xD1 = xD1 * xTime;
        xD1 = xD1 + xLogTerm;

        xDen = xVolatility * xSqrtTime;
        xD1 = xD1 / xDen;
        xD2 = xD1 -  xDen;

        d1 = xD1;
        d2 = xD2;

        NofXd1 = CNDF( d1 );
        NofXd2 = CNDF( d2 );

        FutureValueX = (double) (strike * ( Math.exp( -(rate)*(time) ) ));
        if (otype == 0) {
            OptionPrice = (sptprice * NofXd1) - (FutureValueX * NofXd2);
        } else {
            NegNofXd1 = (double) (1.0 - NofXd1);
            NegNofXd2 = (double) (1.0 - NofXd2);
            OptionPrice = (FutureValueX * NegNofXd2) - (sptprice * NegNofXd1);
        }

        return OptionPrice;
    }

    private static double CNDF(double inputX) {
        int sign;

        double outputX;
        double xInput;
        double xNPrimeofX;
        double expValues;
        double xK2;
        double xK2_2, xK2_3;
        double xK2_4, xK2_5;
        double xLocal, xLocal_1;
        double xLocal_2, xLocal_3;

        // Check for negative value of InputX
        if (inputX < 0.0f) {
            inputX = -inputX;
            sign = 1;
        } else
            sign = 0;

        xInput = inputX;

        // Compute NPrimeX term common to both four & six decimal accuracy calcs
        expValues = (double) Math.exp(-0.5f * inputX * inputX);
        xNPrimeofX = expValues;
        xNPrimeofX = xNPrimeofX * inv_sqrt_2xPI;

        xK2 =  (0.2316419f * xInput);
        xK2 =  (1.0f + xK2);
        xK2 =  (1.0f / xK2);
        xK2_2 = xK2 * xK2;
        xK2_3 = xK2_2 * xK2;
        xK2_4 = xK2_3 * xK2;
        xK2_5 = xK2_4 * xK2;

        xLocal_1 = (xK2 * 0.319381530f);
        xLocal_2 = (xK2_2 * (-0.356563782f));
        xLocal_3 = (xK2_3 * 1.781477937f);
        xLocal_2 = xLocal_2 + xLocal_3;
        xLocal_3 = (xK2_4 * (-1.821255978f));
        xLocal_2 = xLocal_2 + xLocal_3;
        xLocal_3 = (xK2_5 * 1.330274429f);
        xLocal_2 = xLocal_2 + xLocal_3;

        xLocal_1 = xLocal_2 + xLocal_1;
        xLocal   = xLocal_1 * xNPrimeofX;
        xLocal   = (1.0f - xLocal);

        outputX  = xLocal;

        if (sign == 1) {
            outputX = (1.0f - outputX);
        }

        return outputX;
    }
}
