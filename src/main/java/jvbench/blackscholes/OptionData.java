package jvbench.blackscholes;

public class OptionData {

    private double s;
    private double strike;
    private double r;
    private double divq;
    private double v;
    private double t;

    private char OptionType; // "P"=PUT, "C"=CALL
    private double divs;
    private double DGrefval;

    public OptionData(double s, double strike, double r, double divq, double v, double t, char optionType, double divs, double DGrefval) {
        this.s = s;
        this.strike = strike;
        this.r = r;
        this.divq = divq;
        this.v = v;
        this.t = t;
        OptionType = optionType;
        this.divs = divs;
        this.DGrefval = DGrefval;
    }

    public double getS() {
        return s;
    }

    public void setS(double s) {
        this.s = s;
    }

    public double getStrike() {
        return strike;
    }

    public void setStrike(double strike) {
        this.strike = strike;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public double getDivq() {
        return divq;
    }

    public void setDivq(double divq) {
        this.divq = divq;
    }

    public double getV() {
        return v;
    }

    public void setV(double v) {
        this.v = v;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public char getOptionType() {
        return OptionType;
    }

    public void setOptionType(char optionType) {
        OptionType = optionType;
    }

    public double getDivs() {
        return divs;
    }

    public void setDivs(double divs) {
        this.divs = divs;
    }

    public double getDGrefval() {
        return DGrefval;
    }

    public void setDGrefval(double DGrefval) {
        this.DGrefval = DGrefval;
    }
}
